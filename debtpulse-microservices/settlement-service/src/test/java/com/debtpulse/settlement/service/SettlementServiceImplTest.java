package com.debtpulse.settlement.service;

import com.debtpulse.settlement.exception.BusinessRuleException;
import com.debtpulse.settlement.exception.UnauthorizedActionException;
import com.debtpulse.common.enums.ApprovalDecision;
import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.common.enums.SettlementStatus;
import com.debtpulse.settlement.dto.request.ApprovalDecisionRequest;
import com.debtpulse.settlement.dto.request.SettlementRequest;
import com.debtpulse.settlement.entity.SettlementProposal;
import com.debtpulse.settlement.feign.AccountClient;
import com.debtpulse.settlement.feign.AuthClient;
import com.debtpulse.settlement.feign.NotificationClient;
import com.debtpulse.settlement.mapper.SettlementMapper;
import com.debtpulse.settlement.repository.SettlementProposalRepository;
import com.debtpulse.settlement.service.impl.SettlementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock private SettlementProposalRepository repo;
    @Mock private SettlementMapper mapper;
    @Mock private AccountClient accountClient;
    @Mock private AuthClient authClient;
    @Mock private NotificationClient notificationClient;

    private SettlementServiceImpl service;

    @BeforeEach
    void setUp() {
        // Default thresholds: L2 at 10%, L3 at 25%.
        ApprovalPolicy policy = new ApprovalPolicy(BigDecimal.TEN, BigDecimal.valueOf(25));
        service = new SettlementServiceImpl(repo, mapper, accountClient, authClient, notificationClient, policy);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String userId, String role) {
        var authorities = role == null ? List.<SimpleGrantedAuthority>of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, authorities));
    }

    @Test
    void create_derivesHaircutAndHighestLevel() {
        authenticateAs("USR-OFF", "SETTLEMENT_OFFICER");
        when(accountClient.accountExists("ACC-1")).thenReturn(true);
        when(repo.save(any(SettlementProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        // (100000 - 60000) / 100000 * 100 = 40.00% -> chain [L1, L2, L3], highest L3
        SettlementRequest req = new SettlementRequest(
                "ACC-1", new BigDecimal("100000"), new BigDecimal("60000"),
                LocalDate.now().plusDays(30), "please approve");

        service.create(req);

        ArgumentCaptor<SettlementProposal> captor = ArgumentCaptor.forClass(SettlementProposal.class);
        verify(repo).save(captor.capture());
        SettlementProposal saved = captor.getValue();
        assertThat(saved.getHaircutPercent()).isEqualByComparingTo("40.00");
        assertThat(saved.getApprovalLevel()).isEqualTo(ApprovalLevel.L3); // derived, not client-supplied
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.DRAFT);
        assertThat(saved.getCurrentStep()).isNull();
        verify(authClient).audit(any());
    }

    @Test
    void submit_routesToFirstStep() {
        authenticateAs("USR-OFF", "SETTLEMENT_OFFICER");
        SettlementProposal draft = SettlementProposal.builder()
                .proposalId("S-0").officerId("USR-OFF").haircutPercent(new BigDecimal("40.00"))
                .status(SettlementStatus.DRAFT).build();
        when(repo.findById("S-0")).thenReturn(Optional.of(draft));
        when(repo.save(any(SettlementProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submit("S-0");

        ArgumentCaptor<SettlementProposal> captor = ArgumentCaptor.forClass(SettlementProposal.class);
        verify(repo).save(captor.capture());
        SettlementProposal saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PENDING_APPROVAL);
        assertThat(saved.getCurrentStep()).isEqualTo(ApprovalLevel.L1); // always starts at L1
    }

    @Test
    void decide_byMaker_throwsUnauthorized() {
        authenticateAs("USR-OFF", "L1_APPROVER");
        SettlementProposal pending = SettlementProposal.builder()
                .proposalId("S-1").officerId("USR-OFF").haircutPercent(new BigDecimal("5.00"))
                .currentStep(ApprovalLevel.L1).status(SettlementStatus.PENDING_APPROVAL).build();
        when(repo.findById("S-1")).thenReturn(Optional.of(pending));

        ApprovalDecisionRequest req = new ApprovalDecisionRequest(ApprovalDecision.APPROVE, "ok");

        assertThatThrownBy(() -> service.decide("S-1", ApprovalLevel.L1, req))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void decide_levelMismatch_throwsBusinessRule() {
        authenticateAs("USR-APPR", "L2_APPROVER");
        SettlementProposal pending = SettlementProposal.builder()
                .proposalId("S-mm").officerId("USR-OFF").haircutPercent(new BigDecimal("20.00"))
                .currentStep(ApprovalLevel.L2).status(SettlementStatus.PENDING_APPROVAL).build();
        when(repo.findById("S-mm")).thenReturn(Optional.of(pending));

        // Approver selected L1 in the dropdown, but the step awaiting a decision is L2.
        ApprovalDecisionRequest req = new ApprovalDecisionRequest(ApprovalDecision.APPROVE, null);

        assertThatThrownBy(() -> service.decide("S-mm", ApprovalLevel.L1, req))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void decide_wrongRoleForStep_throwsUnauthorized() {
        authenticateAs("USR-APPR", "L1_APPROVER"); // holds L1 but the step is L2
        SettlementProposal pending = SettlementProposal.builder()
                .proposalId("S-2b").officerId("USR-OFF").haircutPercent(new BigDecimal("20.00"))
                .currentStep(ApprovalLevel.L2).status(SettlementStatus.PENDING_APPROVAL).build();
        when(repo.findById("S-2b")).thenReturn(Optional.of(pending));

        // Selects the correct level (L2) so the mismatch guard passes, then the role check fails.
        assertThatThrownBy(() -> service.decide("S-2b", ApprovalLevel.L2,
                new ApprovalDecisionRequest(ApprovalDecision.APPROVE, null)))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void decide_notPending_throwsBusinessRule() {
        authenticateAs("USR-APPR", "L1_APPROVER");
        SettlementProposal draft = SettlementProposal.builder()
                .proposalId("S-2").officerId("USR-OFF").status(SettlementStatus.DRAFT).build();
        when(repo.findById("S-2")).thenReturn(Optional.of(draft));

        ApprovalDecisionRequest req = new ApprovalDecisionRequest(ApprovalDecision.APPROVE, null);

        assertThatThrownBy(() -> service.decide("S-2", ApprovalLevel.L1, req))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void decide_approveIntermediateStep_advancesChain() {
        authenticateAs("USR-L1", "L1_APPROVER");
        // 40% haircut -> chain [L1, L2, L3]; approving L1 must advance to L2, NOT approve outright.
        SettlementProposal pending = SettlementProposal.builder()
                .proposalId("S-3").officerId("USR-OFF").haircutPercent(new BigDecimal("40.00"))
                .currentStep(ApprovalLevel.L1).status(SettlementStatus.PENDING_APPROVAL).build();
        when(repo.findById("S-3")).thenReturn(Optional.of(pending));
        when(repo.save(any(SettlementProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.decide("S-3", ApprovalLevel.L1, new ApprovalDecisionRequest(ApprovalDecision.APPROVE, "L1 ok"));

        ArgumentCaptor<SettlementProposal> captor = ArgumentCaptor.forClass(SettlementProposal.class);
        verify(repo).save(captor.capture());
        SettlementProposal saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PENDING_APPROVAL);
        assertThat(saved.getCurrentStep()).isEqualTo(ApprovalLevel.L2);
        assertThat(saved.getApprovalSteps()).hasSize(1);
    }

    @Test
    void decide_approveFinalStep_setsApproved() {
        authenticateAs("USR-L1", "L1_APPROVER");
        // 5% haircut -> chain [L1] only; approving L1 is final.
        SettlementProposal pending = SettlementProposal.builder()
                .proposalId("S-4").officerId("USR-OFF").haircutPercent(new BigDecimal("5.00"))
                .currentStep(ApprovalLevel.L1).status(SettlementStatus.PENDING_APPROVAL).build();
        when(repo.findById("S-4")).thenReturn(Optional.of(pending));
        when(repo.save(any(SettlementProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.decide("S-4", ApprovalLevel.L1, new ApprovalDecisionRequest(ApprovalDecision.APPROVE, "looks good"));

        ArgumentCaptor<SettlementProposal> captor = ArgumentCaptor.forClass(SettlementProposal.class);
        verify(repo).save(captor.capture());
        SettlementProposal saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.APPROVED);
        assertThat(saved.getApprovedById()).isEqualTo("USR-L1");
        assertThat(saved.getCurrentStep()).isNull();
        verify(authClient).audit(any());
    }

    @Test
    void decide_reject_setsRejected() {
        authenticateAs("USR-L1", "L1_APPROVER");
        SettlementProposal pending = SettlementProposal.builder()
                .proposalId("S-5").officerId("USR-OFF").haircutPercent(new BigDecimal("40.00"))
                .currentStep(ApprovalLevel.L1).status(SettlementStatus.PENDING_APPROVAL).build();
        when(repo.findById("S-5")).thenReturn(Optional.of(pending));
        when(repo.save(any(SettlementProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.decide("S-5", ApprovalLevel.L1, new ApprovalDecisionRequest(ApprovalDecision.REJECT, "too low"));

        ArgumentCaptor<SettlementProposal> captor = ArgumentCaptor.forClass(SettlementProposal.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SettlementStatus.REJECTED);
        assertThat(captor.getValue().getCurrentStep()).isNull();
    }
}
