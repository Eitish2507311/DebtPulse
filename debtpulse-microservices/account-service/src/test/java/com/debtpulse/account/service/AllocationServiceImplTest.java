package com.debtpulse.account.service;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.AllocationStrategy;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.entity.AllocationRule;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.feign.ContactClient;
import com.debtpulse.account.feign.NotificationClient;
import com.debtpulse.account.feign.dto.NotificationRequest;
import com.debtpulse.account.feign.dto.UserDto;
import com.debtpulse.account.mapper.AllocationRuleMapper;
import com.debtpulse.account.repository.AllocationRuleRepository;
import com.debtpulse.account.repository.DelinquentAccountRepository;
import com.debtpulse.account.service.allocation.AllocationRuleEngine;
import com.debtpulse.account.service.allocation.AllocationStrategyResolver;
import com.debtpulse.account.service.allocation.BranchBasedStrategy;
import com.debtpulse.account.service.allocation.LeastLoadedStrategy;
import com.debtpulse.account.service.allocation.RoundRobinStrategy;
import com.debtpulse.account.service.impl.AllocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationServiceImplTest {

    @Mock private AllocationRuleRepository ruleRepo;
    @Mock private DelinquentAccountRepository accountRepo;
    @Mock private AllocationRuleMapper mapper;
    @Mock private AuthClient authClient;
    @Mock private ContactClient contactClient;
    @Mock private NotificationClient notificationClient;

    private AllocationServiceImpl allocationService;

    @BeforeEach
    void setUp() {
        AllocationRuleEngine engine = new AllocationRuleEngine(new AllocationStrategyResolver(
                List.of(new LeastLoadedStrategy(), new RoundRobinStrategy(), new BranchBasedStrategy())));
        allocationService = new AllocationServiceImpl(
                ruleRepo, accountRepo, mapper, authClient, contactClient, notificationClient, engine);
    }

    private UserDto user(String id, String role) {
        return new UserDto(id, "User " + id, id + "@dp.com", "9", role, "B01", "ACTIVE", null);
    }

    @Test
    void executeAllocation_assignsUnassignedAccountToLeastLoadedAgent() {
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-1").loanRef("LN-1").status(AccountStatus.ACTIVE).build();
        when(accountRepo.findByStatusAndAssignedAgentIdIsNull(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(authClient.activeByRole(eq("COLLECTIONS_AGENT"), any())).thenReturn(List.of(user("USR-050", "COLLECTIONS_AGENT")));
        when(accountRepo.countByAssignedAgentId("USR-050")).thenReturn(0L);
        when(accountRepo.save(any(DelinquentAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> summary = allocationService.executeAllocation();

        assertThat(summary.get("assigned")).isEqualTo(1);
        assertThat(account.getAssignedAgentId()).isEqualTo("USR-050");
        verify(accountRepo).save(account);
    }

    @Test
    void executeAllocation_noAgents_assignsNothing() {
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-2").status(AccountStatus.ACTIVE).build();
        when(accountRepo.findByStatusAndAssignedAgentIdIsNull(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(authClient.activeByRole(eq("COLLECTIONS_AGENT"), any())).thenReturn(List.of());

        Map<String, Object> summary = allocationService.executeAllocation();

        assertThat(summary.get("assigned")).isEqualTo(0);
        assertThat(account.getAssignedAgentId()).isNull();
        verify(accountRepo, never()).save(any());
    }

    @Test
    void reassignForEscalation_matchesRuleAndEscalatesToTargetRole() {
        AllocationRule npaLegal = AllocationRule.builder()
                .ruleId("ALR-1").name("NPA Legal Escalation").strategy(AllocationStrategy.LEAST_LOADED)
                .bucket(DpdBucket.NPA).targetRole("LEGAL_OFFICER").daysInBucketThreshold(10)
                .autoEscalate(true).priority(100).active(true).build();
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-9").status(AccountStatus.ACTIVE).bucket(DpdBucket.NPA)
                .daysInCurrentBucket(15).assignedAgentId("USR-agent").build();

        when(ruleRepo.findByActiveTrueOrderByPriorityDesc()).thenReturn(List.of(npaLegal));
        when(accountRepo.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(contactClient.activePtpCount("ACC-9")).thenReturn(0L);
        when(authClient.activeByRole(eq("LEGAL_OFFICER"), any())).thenReturn(List.of(user("USR-legal", "LEGAL_OFFICER")));
        when(accountRepo.countByAssignedAgentId("USR-legal")).thenReturn(0L);
        when(accountRepo.save(any(DelinquentAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        int reassigned = allocationService.reassignForEscalation();

        assertThat(reassigned).isEqualTo(1);
        assertThat(account.getAssignedAgentId()).isEqualTo("USR-legal");
        verify(accountRepo).save(account);
        // The new owner is notified under the ESCALATION category.
        verify(notificationClient).notify(any(NotificationRequest.class));
    }

    @Test
    void executeAllocation_isRuleDriven_honouringBucketRoleAndStrategy() {
        // An allocation rule (autoEscalate=false) targets a NON-default role in a specific branch;
        // the unassigned NPA account in that branch must go to the least-loaded eligible user.
        AllocationRule seniorRule = AllocationRule.builder()
                .ruleId("ALR-2").name("NPA Senior Allocation").strategy(AllocationStrategy.LEAST_LOADED)
                .bucket(DpdBucket.NPA).targetRole("PORTFOLIO_MANAGER").branchId("B01")
                .autoEscalate(false).priority(50).active(true).build();
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-5").loanRef("LN-5").status(AccountStatus.ACTIVE)
                .bucket(DpdBucket.NPA).branchId("B01").build();

        when(ruleRepo.findByActiveTrueOrderByPriorityDesc()).thenReturn(List.of(seniorRule));
        when(accountRepo.findByStatusAndAssignedAgentIdIsNull(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(authClient.activeByRole(eq("PORTFOLIO_MANAGER"), eq("B01")))
                .thenReturn(List.of(user("USR-pm1", "PORTFOLIO_MANAGER"), user("USR-pm2", "PORTFOLIO_MANAGER")));
        when(accountRepo.countByAssignedAgentId("USR-pm1")).thenReturn(3L);
        when(accountRepo.countByAssignedAgentId("USR-pm2")).thenReturn(1L);
        when(accountRepo.save(any(DelinquentAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> summary = allocationService.executeAllocation();

        assertThat(summary.get("assigned")).isEqualTo(1);
        assertThat(account.getAssignedAgentId()).isEqualTo("USR-pm2"); // least loaded of the branch PMs
        // The generic collections-agent fallback must NOT be used when a rule matches.
        verify(authClient, never()).activeByRole(eq("COLLECTIONS_AGENT"), any());
    }

    @Test
    void reassignForEscalation_skipsWhenActivePtp() {
        AllocationRule rule = AllocationRule.builder()
                .ruleId("ALR-1").strategy(AllocationStrategy.LEAST_LOADED).bucket(DpdBucket.NPA)
                .targetRole("LEGAL_OFFICER").daysInBucketThreshold(10).autoEscalate(true).priority(100).active(true).build();
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-9").status(AccountStatus.ACTIVE).bucket(DpdBucket.NPA).daysInCurrentBucket(15).build();

        when(ruleRepo.findByActiveTrueOrderByPriorityDesc()).thenReturn(List.of(rule));
        when(accountRepo.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(contactClient.activePtpCount("ACC-9")).thenReturn(2L); // active PTP → do not disturb

        int reassigned = allocationService.reassignForEscalation();

        assertThat(reassigned).isZero();
        verify(accountRepo, never()).save(any());
    }

    @Test
    void reassignForEscalation_idempotent_whenAlreadyAssignedToEligibleTarget() {
        AllocationRule rule = AllocationRule.builder()
                .ruleId("ALR-1").strategy(AllocationStrategy.LEAST_LOADED).bucket(DpdBucket.NPA)
                .targetRole("LEGAL_OFFICER").daysInBucketThreshold(10).autoEscalate(true).priority(100).active(true).build();
        DelinquentAccount account = DelinquentAccount.builder()
                .accountId("ACC-9").status(AccountStatus.ACTIVE).bucket(DpdBucket.NPA)
                .daysInCurrentBucket(15).assignedAgentId("USR-legal").build(); // already with a legal officer

        when(ruleRepo.findByActiveTrueOrderByPriorityDesc()).thenReturn(List.of(rule));
        when(accountRepo.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(account));
        when(contactClient.activePtpCount("ACC-9")).thenReturn(0L);
        when(authClient.activeByRole(eq("LEGAL_OFFICER"), any())).thenReturn(List.of(user("USR-legal", "LEGAL_OFFICER")));
        when(accountRepo.countByAssignedAgentId("USR-legal")).thenReturn(1L);

        int reassigned = allocationService.reassignForEscalation();

        assertThat(reassigned).isZero();                 // already correctly placed
        verify(accountRepo, never()).save(any());
    }
}
