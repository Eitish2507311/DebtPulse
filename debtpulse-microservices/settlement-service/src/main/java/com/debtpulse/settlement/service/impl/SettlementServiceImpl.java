package com.debtpulse.settlement.service.impl;

import com.debtpulse.settlement.exception.BusinessRuleException;
import com.debtpulse.settlement.exception.ResourceNotFoundException;
import com.debtpulse.settlement.exception.UnauthorizedActionException;
import com.debtpulse.common.audit.AuditContext;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.ApprovalDecision;
import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.common.enums.SettlementStatus;
import com.debtpulse.settlement.dto.request.ApprovalDecisionRequest;
import com.debtpulse.settlement.dto.request.SettlementRequest;
import com.debtpulse.settlement.dto.response.SettlementResponse;
import com.debtpulse.settlement.entity.ApprovalStep;
import com.debtpulse.settlement.entity.SettlementProposal;
import com.debtpulse.settlement.feign.AccountClient;
import com.debtpulse.settlement.feign.AuthClient;
import com.debtpulse.settlement.feign.NotificationClient;
import com.debtpulse.settlement.feign.dto.AuditLogRequest;
import com.debtpulse.settlement.feign.dto.NotificationRequest;
import com.debtpulse.settlement.feign.dto.UserDto;
import com.debtpulse.settlement.mapper.SettlementMapper;
import com.debtpulse.settlement.repository.SettlementProposalRepository;
import com.debtpulse.settlement.service.ApprovalPolicy;
import com.debtpulse.settlement.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementServiceImpl implements SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementServiceImpl.class);
    private static final String SOURCE = "settlement-service";
    private static final String ENTITY = "SettlementProposal";

    private final SettlementProposalRepository repo;
    private final SettlementMapper mapper;
    private final AccountClient accountClient;
    private final AuthClient authClient;
    private final NotificationClient notificationClient;
    private final ApprovalPolicy approvalPolicy;

    public SettlementServiceImpl(SettlementProposalRepository repo, SettlementMapper mapper,
                                 AccountClient accountClient, AuthClient authClient,
                                 NotificationClient notificationClient, ApprovalPolicy approvalPolicy) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.authClient = authClient;
        this.notificationClient = notificationClient;
        this.approvalPolicy = approvalPolicy;
    }

    @Override
    @Transactional
    public SettlementResponse create(SettlementRequest req) {
        if (!accountClient.accountExists(req.accountId())) {
            throw new ResourceNotFoundException("Account not found: " + req.accountId());
        }
        String officerId = AuthContext.currentUserId();
        BigDecimal haircut = computeHaircut(req.totalOutstanding(), req.settlementAmount());
        // Approval authority is DERIVED from the haircut — never supplied by the client.
        ApprovalLevel highest = approvalPolicy.highestLevel(haircut);

        SettlementProposal proposal = SettlementProposal.builder()
                .accountId(req.accountId())
                .officerId(officerId)
                .totalOutstanding(req.totalOutstanding())
                .settlementAmount(req.settlementAmount())
                .haircutPercent(haircut)
                .paymentDeadline(req.paymentDeadline())
                .approvalLevel(highest)
                .notes(req.notes())
                .status(SettlementStatus.DRAFT)
                .build();

        SettlementProposal saved = repo.save(proposal);
        log.info("Settlement created id={} account={} haircut={}% chain={}", saved.getProposalId(),
                saved.getAccountId(), saved.getHaircutPercent(), approvalPolicy.requiredLevels(haircut));
        audit(officerId, "SETTLEMENT_CREATE", saved.getProposalId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public SettlementResponse submit(String id) {
        SettlementProposal proposal = find(id);
        if (proposal.getStatus() != SettlementStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT settlements can be submitted (current: " + proposal.getStatus() + ")",
                    "INVALID_STATE");
        }
        // Route to the FIRST link of the derived chain; each level then approves in sequence.
        ApprovalLevel firstStep = approvalPolicy.requiredLevels(proposal.getHaircutPercent()).get(0);
        proposal.setStatus(SettlementStatus.PENDING_APPROVAL);
        proposal.setCurrentStep(firstStep);
        SettlementProposal saved = repo.save(proposal);

        notifyApprover(firstStep, saved, "awaits your " + firstStep + " approval");
        audit(AuthContext.currentUserId(), "SETTLEMENT_SUBMIT", saved.getProposalId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementResponse> outstanding() {
        return repo.findByStatusNotIn(List.of(
                        SettlementStatus.PAID, SettlementStatus.REJECTED, SettlementStatus.EXPIRED))
                .stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementResponse> pastDeadline() {
        return repo.findByStatusAndPaymentDeadlineBefore(SettlementStatus.APPROVED, LocalDate.now())
                .stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementResponse> approvalQueue() {
        return repo.findByStatus(SettlementStatus.PENDING_APPROVAL)
                .stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public SettlementResponse update(String id, SettlementRequest req) {
        SettlementProposal proposal = find(id);
        if (proposal.getStatus() != SettlementStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT settlements can be updated (current: " + proposal.getStatus() + ")",
                    "INVALID_STATE");
        }
        BigDecimal haircut = computeHaircut(req.totalOutstanding(), req.settlementAmount());
        proposal.setAccountId(req.accountId());
        proposal.setTotalOutstanding(req.totalOutstanding());
        proposal.setSettlementAmount(req.settlementAmount());
        proposal.setHaircutPercent(haircut);
        proposal.setPaymentDeadline(req.paymentDeadline());
        proposal.setApprovalLevel(approvalPolicy.highestLevel(haircut));
        proposal.setNotes(req.notes());
        SettlementProposal saved = repo.save(proposal);
        audit(AuthContext.currentUserId(), "SETTLEMENT_UPDATE", saved.getProposalId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public SettlementResponse decide(String id, ApprovalLevel level, ApprovalDecisionRequest req) {
        SettlementProposal proposal = find(id);
        if (proposal.getStatus() != SettlementStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException(
                    "Settlement is not awaiting approval (current: " + proposal.getStatus() + ")",
                    "INVALID_STATE");
        }
        // The acting tier is the chain's current step — the client cannot choose it.
        ApprovalLevel actingLevel = proposal.getCurrentStep();
        if (actingLevel == null) {
            throw new BusinessRuleException("Settlement has no active approval step", "NO_ACTIVE_STEP");
        }
        // Explicit guard: the level selected on the request (the ?level= dropdown) must match the step
        // awaiting a decision. The chain still comes from the haircut; this catches a wrong selection.
        if (level != actingLevel) {
            throw new BusinessRuleException(
                    "You selected " + level + " but this settlement is awaiting " + actingLevel
                            + " approval.", "APPROVAL_LEVEL_MISMATCH");
        }

        String approverId = AuthContext.currentUserId();
        // Maker-checker: the approver must not be the officer who raised the proposal.
        if (approverId != null && approverId.equals(proposal.getOfficerId())) {
            throw new UnauthorizedActionException(
                    "Maker-checker violation: the raising officer cannot approve their own settlement");
        }
        // The caller must actually hold the role for the level currently awaiting a decision
        // (an L1 approver cannot act on an L2 step). ADMIN may act on any level.
        String role = AuthContext.currentRole();
        if (!"ADMIN".equals(role) && !approverRoleFor(actingLevel).equals(role)) {
            throw new UnauthorizedActionException(
                    "This settlement is awaiting " + actingLevel + " approval; your role (" + role
                            + ") cannot act on this step");
        }

        ApprovalStep step = ApprovalStep.builder()
                .approverId(approverId)
                .level(actingLevel)
                .decision(req.decision())
                .decidedAt(LocalDateTime.now())
                .comments(req.comments())
                .build();
        proposal.addApprovalStep(step);

        if (req.decision() == ApprovalDecision.REJECT) {
            proposal.setStatus(SettlementStatus.REJECTED);
            proposal.setCurrentStep(null);
            notificationClient.notify(new NotificationRequest(proposal.getOfficerId(),
                    "Settlement " + proposal.getProposalId() + " was REJECTED at " + actingLevel + ".",
                    "SETTLEMENT"));
        } else {
            ApprovalLevel next = approvalPolicy.nextLevel(proposal.getHaircutPercent(), actingLevel);
            if (next != null) {
                // More sign-offs required — advance the chain to the next tier.
                proposal.setCurrentStep(next);
                notifyApprover(next, proposal, "approved at " + actingLevel + "; awaits your " + next + " approval");
            } else {
                // Final approval reached.
                proposal.setStatus(SettlementStatus.APPROVED);
                proposal.setCurrentStep(null);
                proposal.setApprovedById(approverId);
                notificationClient.notify(new NotificationRequest(proposal.getOfficerId(),
                        "Settlement " + proposal.getProposalId() + " is FULLY APPROVED.", "SETTLEMENT"));
            }
        }

        SettlementProposal saved = repo.save(proposal);
        log.info("Settlement id={} step={} decided {} by approver={} -> status={} nextStep={}",
                saved.getProposalId(), actingLevel, req.decision(), approverId, saved.getStatus(), saved.getCurrentStep());
        audit(approverId, "SETTLEMENT_DECISION", saved.getProposalId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public SettlementResponse markPaid(String id) {
        SettlementProposal proposal = find(id);
        if (proposal.getStatus() != SettlementStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED settlements can be marked paid (current: " + proposal.getStatus() + ")",
                    "INVALID_STATE");
        }
        proposal.setStatus(SettlementStatus.PAID);
        SettlementProposal saved = repo.save(proposal);
        log.info("Settlement id={} marked PAID", saved.getProposalId());
        audit(AuthContext.currentUserId(), "SETTLEMENT_PAID", saved.getProposalId());
        cascadeAccountStatus(saved.getAccountId(), AccountStatus.SETTLED);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSettlements", repo.count());
        stats.put("approvedSettlements", repo.countByStatus(SettlementStatus.APPROVED));
        stats.put("rejectedSettlements", repo.countByStatus(SettlementStatus.REJECTED));
        stats.put("paidSettlements", repo.countByStatus(SettlementStatus.PAID));
        stats.put("pendingSettlements", repo.countByStatus(SettlementStatus.PENDING_APPROVAL));
        return stats;
    }

    // ---- helpers ----

    private BigDecimal computeHaircut(BigDecimal totalOutstanding, BigDecimal settlementAmount) {
        if (totalOutstanding == null || settlementAmount == null
                || totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalOutstanding.subtract(settlementAmount)
                .divide(totalOutstanding, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Looks up the approver for a level and notifies them; logs a warning if none is configured. */
    private void notifyApprover(ApprovalLevel level, SettlementProposal proposal, String action) {
        String role = approverRoleFor(level);
        UserDto approver = authClient.firstByRole(role);
        if (approver != null && approver.userId() != null) {
            notificationClient.notify(new NotificationRequest(
                    approver.userId(),
                    "Settlement proposal " + proposal.getProposalId() + " " + action + ".",
                    "SETTLEMENT"));
            log.info("Settlement id={} routed to approver={} role={}", proposal.getProposalId(), approver.userId(), role);
        } else {
            // Do NOT skip the control — the step stays required; it just can't be notified.
            log.warn("Settlement id={} awaits {} approval but no user has role={}",
                    proposal.getProposalId(), level, role);
        }
    }

    /** Maps the approval tier to the approver role name (L1 -> L1_APPROVER, etc.). */
    private String approverRoleFor(ApprovalLevel level) {
        return switch (level) {
            case L1 -> "L1_APPROVER";
            case L2 -> "L2_APPROVER";
            case L3 -> "L3_APPROVER";
        };
    }

    private SettlementProposal find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + id));
    }

    private void audit(String userId, String action, String recordId) {
        authClient.audit(new AuditLogRequest(userId, action, ENTITY, recordId, SOURCE));
        AuditContext.markRecorded();
    }

    /**
     * Best-effort lifecycle cascade to account-service (DP5-18). The circuit-breaker fallback already
     * absorbs a downstream outage; this guard additionally shields the settlement transaction from any
     * unexpected client error, so marking a settlement paid can never be rolled back by the cascade.
     */
    private void cascadeAccountStatus(String accountId, AccountStatus status) {
        if (accountId == null) return;
        try {
            accountClient.updateStatus(accountId, status);
            log.info("Cascaded account {} -> {} after settlement paid", accountId, status);
        } catch (Exception ex) {
            log.warn("Account status cascade ({} -> {}) failed; continuing", accountId, status, ex);
        }
    }
}
