package com.debtpulse.account.service.impl;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.AllocationStrategy;
import com.debtpulse.common.enums.Role;
import com.debtpulse.account.dto.request.AllocationRuleRequest;
import com.debtpulse.account.entity.AllocationRule;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.feign.ContactClient;
import com.debtpulse.account.feign.NotificationClient;
import com.debtpulse.account.feign.dto.AuditLogRequest;
import com.debtpulse.account.feign.dto.NotificationRequest;
import com.debtpulse.account.feign.dto.UserDto;
import com.debtpulse.account.mapper.AllocationRuleMapper;
import com.debtpulse.account.repository.AllocationRuleRepository;
import com.debtpulse.account.repository.DelinquentAccountRepository;
import com.debtpulse.account.service.AllocationService;
import com.debtpulse.account.service.allocation.AllocationRuleEngine;
import com.debtpulse.account.exception.ResourceNotFoundException;
import com.debtpulse.common.security.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AllocationServiceImpl implements AllocationService {

    private static final Logger log = LoggerFactory.getLogger(AllocationServiceImpl.class);
    private static final String SOURCE_SERVICE = "account-service";
    private static final String ENTITY_TYPE = "DelinquentAccount";
    private static final String ESCALATION_CATEGORY = "ESCALATION";
    private static final String ALLOCATION_CATEGORY = "ALLOCATION";

    private final AllocationRuleRepository ruleRepo;
    private final DelinquentAccountRepository accountRepo;
    private final AllocationRuleMapper mapper;
    private final AuthClient authClient;
    private final ContactClient contactClient;
    private final NotificationClient notificationClient;
    private final AllocationRuleEngine engine;

    public AllocationServiceImpl(AllocationRuleRepository ruleRepo,
                                 DelinquentAccountRepository accountRepo,
                                 AllocationRuleMapper mapper,
                                 AuthClient authClient,
                                 ContactClient contactClient,
                                 NotificationClient notificationClient,
                                 AllocationRuleEngine engine) {
        this.ruleRepo = ruleRepo;
        this.accountRepo = accountRepo;
        this.mapper = mapper;
        this.authClient = authClient;
        this.contactClient = contactClient;
        this.notificationClient = notificationClient;
        this.engine = engine;
    }

    @Override
    public Page<AllocationRule> listRules(Pageable pageable) {
        return ruleRepo.findAll(pageable);
    }

    @Override
    public AllocationRule getRule(String id) {
        return findRule(id);
    }

    @Override
    public AllocationRule createRule(AllocationRuleRequest request) {
        AllocationRule saved = ruleRepo.save(mapper.toEntity(request));
        log.info("Created allocation rule id={} name={} strategy={}", saved.getRuleId(), saved.getName(), saved.getStrategy());
        return saved;
    }

    @Override
    public AllocationRule updateRule(String id, AllocationRuleRequest req) {
        AllocationRule rule = findRule(id);
        if (req.name() != null) rule.setName(req.name());
        if (req.strategy() != null) rule.setStrategy(req.strategy());
        rule.setBucket(req.bucket());
        if (req.targetRole() != null) rule.setTargetRole(req.targetRole());
        rule.setDaysInBucketThreshold(req.daysInBucketThreshold());
        rule.setMinDpd(req.minDpd());
        rule.setGracePeriodDays(req.gracePeriodDays());
        rule.setCapacityLimit(req.capacityLimit());
        rule.setBranchId(req.branchId());
        if (req.priority() != null) rule.setPriority(req.priority());
        if (req.autoEscalate() != null) rule.setAutoEscalate(req.autoEscalate());
        if (req.active() != null) rule.setActive(req.active());
        return ruleRepo.save(rule);
    }

    @Override
    public void deleteRule(String id) {
        ruleRepo.delete(findRule(id));
        log.info("Deleted allocation rule id={}", id);
    }

    @Override
    public Map<String, Object> executeAllocation() {
        List<AllocationRule> allocationRules = activeAllocationRules();
        List<DelinquentAccount> unassigned =
                accountRepo.findByStatusAndAssignedAgentIdIsNull(AccountStatus.ACTIVE);

        int assigned = 0;
        for (DelinquentAccount account : unassigned) {
            // Isolate each account: a downstream failure (e.g. auth-service unreachable) skips this
            // one and moves on, so one bad call never aborts the whole allocation run.
            try {
                if (allocate(account, allocationRules)) {
                    accountRepo.save(account);
                    notifyAllocation(account, account.getAssignedAgentId());
                    audit("ALLOCATE", account.getAccountId());
                    assigned++;
                }
            } catch (Exception ex) {
                log.warn("Allocation skipped for account {} this run — {}",
                        account.getAccountId(), ex.getMessage());
            }
        }
        log.info("executeAllocation: assigned {} of {} unassigned account(s) using {} allocation rule(s)",
                assigned, unassigned.size(), allocationRules.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("assigned", assigned);
        summary.put("evaluated", unassigned.size());
        summary.put("unassignedRemaining", unassigned.size() - assigned);
        return summary;
    }

    @Override
    public DelinquentAccount autoAllocate(DelinquentAccount account) {
        // Import-time placement: apply the matching allocation rule (bucket/branch/strategy/capacity),
        // falling back to the least-loaded collection agent when no rule matches. The caller persists.
        // Allocation is best-effort: if it can't run (e.g. auth-service unreachable) the account is
        // still created, just left unassigned to be picked up by a later allocation/escalation run —
        // onboarding must never fail because an agent couldn't be chosen right now.
        try {
            if (allocate(account, activeAllocationRules())) {
                notifyAllocation(account, account.getAssignedAgentId());
            } else {
                log.warn("autoAllocate: no eligible agent for account loanRef={} — left unassigned",
                        account.getLoanRef());
            }
        } catch (Exception ex) {
            account.setAssignedAgentId(null);
            log.warn("autoAllocate: allocation unavailable for loanRef={} — left unassigned ({})",
                    account.getLoanRef(), ex.getMessage());
        }
        return account;
    }

    @Override
    public int reassignForEscalation() {
        // Escalation is driven only by escalation rules (autoEscalate=true), evaluated priority-desc;
        // the matched rule decides the target role, branch scope, capacity and distribution strategy.
        List<AllocationRule> escalationRules = activeEscalationRules();
        int reassigned = 0;

        for (DelinquentAccount account : accountRepo.findByStatus(AccountStatus.ACTIVE)) {
            // Isolate each account: any downstream failure (contact-service or auth-service
            // unreachable, etc.) skips this account for this run rather than aborting the whole job.
            try {
                Optional<AllocationRule> matched = engine.selectRule(account, escalationRules);
                if (matched.isEmpty()) {
                    continue;
                }
                AllocationRule rule = matched.get();
                if (rule.getTargetRole() == null) {
                    continue;
                }
                // Never yank an account away while a Promise-To-Pay is still active. If contact-service
                // can't be reached, hasActivePtp fails safe (treats it as protected) so we don't escalate.
                if (hasActivePtp(account.getAccountId())) {
                    continue;
                }

                String target = pickTarget(rule, account);
                if (target == null) {
                    continue;
                }
                // Idempotency: already assigned to an eligible target-role user → nothing to do.
                if (target.equals(account.getAssignedAgentId())) {
                    continue;
                }
                account.setAssignedAgentId(target);
                accountRepo.save(account);
                notifyEscalation(account, rule, target);
                audit("ESCALATION", account.getAccountId());
                reassigned++;
            } catch (Exception ex) {
                log.warn("Escalation skipped for account {} this run — {}",
                        account.getAccountId(), ex.getMessage());
            }
        }
        log.info("reassignForEscalation: reassigned {} account(s) using {} escalation rule(s)",
                reassigned, escalationRules.size());
        return reassigned;
    }

    // ---- helpers ----

    /**
     * True if the account has an active Promise-To-Pay. On any contact-service failure this returns
     * {@code true} (fail-safe): an account we cannot verify is treated as PTP-protected and left where
     * it is, so a transient outage can neither wrongly escalate an account nor abort the whole run.
     */
    private boolean hasActivePtp(String accountId) {
        try {
            return contactClient.activePtpCount(accountId) > 0;
        } catch (Exception ex) {
            log.warn("Could not check active PTP for account {} — skipping escalation this run ({})",
                    accountId, ex.getMessage());
            return true;
        }
    }

    /** Active initial-allocation rules (autoEscalate=false), highest priority first. */
    private List<AllocationRule> activeAllocationRules() {
        return ruleRepo.findByActiveTrueOrderByPriorityDesc().stream()
                .filter(r -> !r.isAutoEscalate())
                .toList();
    }

    /** Active escalation rules (autoEscalate=true), highest priority first. */
    private List<AllocationRule> activeEscalationRules() {
        return ruleRepo.findByActiveTrueOrderByPriorityDesc().stream()
                .filter(AllocationRule::isAutoEscalate)
                .toList();
    }

    /**
     * Rule-driven initial placement of a single account onto a collection agent. Sets
     * {@code assignedAgentId} in-memory (the caller persists) and returns whether it changed.
     * When a matching allocation rule exists its role/branch/strategy/capacity are honoured;
     * otherwise the account falls back to the least-loaded active collection agent so imports
     * are never left silently unassigned when no rule is configured.
     */
    private boolean allocate(DelinquentAccount account, List<AllocationRule> allocationRules) {
        String target = engine.selectRule(account, allocationRules)
                .map(rule -> pickTarget(rule, account))
                .orElseGet(() -> pickDefaultAgent(account));
        if (target == null || target.equals(account.getAssignedAgentId())) {
            return false;
        }
        account.setAssignedAgentId(target);
        return true;
    }

    /** Fallback target when no allocation rule matches: least-loaded active collection agent. */
    private String pickDefaultAgent(DelinquentAccount account) {
        List<UserDto> agents = authClient.activeByRole(Role.COLLECTIONS_AGENT.name(), null);
        if (agents == null || agents.isEmpty()) {
            return null;
        }
        List<String> candidateIds = agents.stream().map(UserDto::userId).toList();
        return engine.chooseTarget(AllocationStrategy.LEAST_LOADED, candidateIds, currentLoad(agents))
                .orElse(null);
    }

    /** Resolve candidates for the rule (role + branch + capacity), then apply its strategy. */
    private String pickTarget(AllocationRule rule, DelinquentAccount account) {
        String branchScope = (rule.getStrategy() == AllocationStrategy.BRANCH_BASED || rule.getBranchId() != null)
                ? account.getBranchId() : null;
        List<UserDto> candidates = authClient.activeByRole(rule.getTargetRole(), branchScope);
        if (candidates == null || candidates.isEmpty()) {
            log.warn("Rule '{}' matched account {} but no active {} user available",
                    rule.getName(), account.getAccountId(), rule.getTargetRole());
            return null;
        }
        Map<String, Long> load = currentLoad(candidates);
        List<String> candidateIds = new ArrayList<>(candidates.stream().map(UserDto::userId).toList());
        // Capacity: drop candidates at/over their limit.
        if (rule.getCapacityLimit() != null) {
            candidateIds.removeIf(id -> load.getOrDefault(id, 0L) >= rule.getCapacityLimit());
        }
        // If the account is already held by an eligible candidate, keep it there (idempotent).
        if (account.getAssignedAgentId() != null && candidateIds.contains(account.getAssignedAgentId())) {
            return account.getAssignedAgentId();
        }
        if (candidateIds.isEmpty()) {
            log.warn("Rule '{}' matched account {} but all {} candidates are at capacity",
                    rule.getName(), account.getAccountId(), rule.getTargetRole());
            return null;
        }
        return engine.chooseTarget(rule.getStrategy(), candidateIds, load).orElse(null);
    }

    private Map<String, Long> currentLoad(List<UserDto> agents) {
        Map<String, Long> load = new HashMap<>();
        for (UserDto agent : agents) {
            load.put(agent.userId(), accountRepo.countByAssignedAgentId(agent.userId()));
        }
        return load;
    }

    /** Best-effort alert to the agent an account was just allocated to (fallback logs if down). */
    private void notifyAllocation(DelinquentAccount account, String target) {
        if (target == null || target.isBlank()) {
            return;
        }
        String message = String.format(
                "Account %s (%s, DPD %d) has been allocated to you — you can now log contact attempts "
                        + "and record promises-to-pay for it.",
                account.getAccountId(), account.getBucket(),
                account.getDpd() == null ? 0 : account.getDpd());
        notificationClient.notify(new NotificationRequest(target, message, ALLOCATION_CATEGORY));
    }

    /** Best-effort alert to the officer an account was just escalated to (fallback logs if down). */
    private void notifyEscalation(DelinquentAccount account, AllocationRule rule, String target) {
        String message = String.format(
                "Account %s (%s, DPD %d) has been escalated to you under rule '%s'.",
                account.getAccountId(), account.getBucket(),
                account.getDpd() == null ? 0 : account.getDpd(), rule.getName());
        notificationClient.notify(new NotificationRequest(target, message, ESCALATION_CATEGORY));
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(
                AuthContext.currentUserId(), action, ENTITY_TYPE, recordId, SOURCE_SERVICE));
        com.debtpulse.common.audit.AuditContext.markRecorded();
    }

    private AllocationRule findRule(String id) {
        return ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation rule not found: " + id));
    }
}
