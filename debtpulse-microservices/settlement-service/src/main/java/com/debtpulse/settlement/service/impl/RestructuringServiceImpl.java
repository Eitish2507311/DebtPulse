package com.debtpulse.settlement.service.impl;

import com.debtpulse.settlement.exception.BusinessRuleException;
import com.debtpulse.settlement.exception.ResourceNotFoundException;
import com.debtpulse.common.audit.AuditContext;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.RestructuringStatus;
import com.debtpulse.common.enums.Role;
import com.debtpulse.settlement.dto.request.RestructuringRequest;
import com.debtpulse.settlement.dto.response.RestructuringResponse;
import com.debtpulse.settlement.entity.RestructuringProposal;
import com.debtpulse.settlement.feign.AccountClient;
import com.debtpulse.settlement.feign.dto.AccountDto;
import com.debtpulse.settlement.feign.AuthClient;
import com.debtpulse.settlement.feign.dto.AuditLogRequest;
import com.debtpulse.settlement.mapper.RestructuringMapper;
import com.debtpulse.settlement.repository.RestructuringProposalRepository;
import com.debtpulse.settlement.service.RestructuringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RestructuringServiceImpl implements RestructuringService {

    private static final Logger log = LoggerFactory.getLogger(RestructuringServiceImpl.class);
    private static final String SOURCE = "settlement-service";
    private static final String ENTITY = "RestructuringProposal";

    private final RestructuringProposalRepository repo;
    private final RestructuringMapper mapper;
    private final AccountClient accountClient;
    private final AuthClient authClient;

    public RestructuringServiceImpl(RestructuringProposalRepository repo, RestructuringMapper mapper,
                                    AccountClient accountClient, AuthClient authClient) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.authClient = authClient;
    }

    @Override
    @Transactional
    public RestructuringResponse create(RestructuringRequest req) {
        if (!accountClient.accountExists(req.accountId())) {
            throw new ResourceNotFoundException("Account not found: " + req.accountId());
        }
        assertOfficerOwnsAccount(req.accountId());
        // Only one live restructuring plan per account — a new one cannot be raised while an existing
        // plan is still in force (i.e. anything other than DEFAULTED), so plans are never stacked.
        boolean hasLivePlan = repo.findByAccountId(req.accountId()).stream()
                .anyMatch(r -> r.getStatus() != RestructuringStatus.DEFAULTED);
        if (hasLivePlan) {
            throw new BusinessRuleException(
                    "A restructuring plan already exists for account " + req.accountId()
                            + "; a new one cannot be created while it is in force.", "RESTRUCTURE_IN_PROGRESS");
        }
        assertViable(req);
        String officerId = AuthContext.currentUserId();
        RestructuringProposal proposal = RestructuringProposal.builder()
                .accountId(req.accountId())
                .officerId(officerId)
                .revisedTenure(req.revisedTenure())
                .revisedEmi(req.revisedEmi())
                .waiverAmount(req.waiverAmount())
                .startDate(req.startDate())
                .status(RestructuringStatus.DRAFT)
                .build();
        RestructuringProposal saved = repo.save(proposal);
        log.info("Restructuring created id={} account={}", saved.getRestructureId(), saved.getAccountId());
        audit(officerId, "RESTRUCTURE_CREATE", saved.getRestructureId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestructuringResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RestructuringResponse getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestructuringResponse> byAccount(String accountId) {
        return repo.findByAccountId(accountId).stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public RestructuringResponse update(String id, RestructuringRequest req) {
        RestructuringProposal proposal = find(id);
        if (proposal.getStatus() != RestructuringStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT restructuring proposals can be updated (current: " + proposal.getStatus() + ")",
                    "INVALID_STATE");
        }
        assertOfficerOwnsAccount(proposal.getAccountId());
        assertViable(req);
        proposal.setAccountId(req.accountId());
        proposal.setRevisedTenure(req.revisedTenure());
        proposal.setRevisedEmi(req.revisedEmi());
        proposal.setWaiverAmount(req.waiverAmount());
        proposal.setStartDate(req.startDate());
        RestructuringProposal saved = repo.save(proposal);
        audit(AuthContext.currentUserId(), "RESTRUCTURE_UPDATE", saved.getRestructureId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public RestructuringResponse approve(String id) {
        RestructuringProposal proposal = find(id);
        String approverId = AuthContext.currentUserId();
        proposal.setStatus(RestructuringStatus.APPROVED);
        proposal.setApprovedById(approverId);
        RestructuringProposal saved = repo.save(proposal);
        log.info("Restructuring id={} approved by {}", saved.getRestructureId(), approverId);
        audit(approverId, "RESTRUCTURE_APPROVE", saved.getRestructureId());
        // Reflect the approved plan on the account so the portfolio shows it as RESTRUCTURED.
        cascadeAccountStatus(saved.getAccountId(), AccountStatus.RESTRUCTURED);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public RestructuringResponse reject(String id) {
        RestructuringProposal proposal = find(id);
        // Pragmatic: there is no dedicated REJECTED state — send it back to DRAFT for revision.
        proposal.setStatus(RestructuringStatus.DRAFT);
        proposal.setApprovedById(null);
        RestructuringProposal saved = repo.save(proposal);
        log.info("Restructuring id={} rejected (returned to DRAFT) by {}",
                saved.getRestructureId(), AuthContext.currentUserId());
        audit(AuthContext.currentUserId(), "RESTRUCTURE_REJECT", saved.getRestructureId());
        return mapper.toDto(saved);
    }

    /**
     * DP5-19 viability rule: the revised plan must repay what's owed after any waiver, i.e.
     * {@code revisedEmi × revisedTenure ≥ totalOutstanding − waiverAmount}. Outstanding is read live
     * from account-service; if it is unavailable (circuit-breaker fallback) the check is skipped with
     * a warning rather than blocking legitimate proposals — the {@code accountExists} guard already
     * ensures the account is real before we get here.
     */
    private void assertViable(RestructuringRequest req) {
        AccountDto account = accountClient.getAccount(req.accountId());
        if (account == null || account.totalOverdue() == null) {
            log.warn("Viability check skipped for account {} — outstanding unavailable", req.accountId());
            return;
        }
        BigDecimal capacity = req.revisedEmi().multiply(BigDecimal.valueOf(req.revisedTenure()));
        BigDecimal required = account.totalOverdue().subtract(req.waiverAmount());
        if (capacity.compareTo(required) < 0) {
            throw new BusinessRuleException(
                    "Restructuring not viable: revised EMI × tenure (" + capacity
                            + ") is below outstanding minus waiver (" + required + ")",
                    "RESTRUCTURE_NOT_VIABLE");
        }
    }

    /**
     * A settlement officer may only raise/edit restructurings for accounts allocated to them; admins
     * and approvers are exempt. Fail-closed: if the account can't be read, deny.
     */
    private void assertOfficerOwnsAccount(String accountId) {
        if (!Role.SETTLEMENT_OFFICER.name().equals(AuthContext.currentRole())) {
            return;
        }
        AccountDto account = accountClient.getAccount(accountId);
        String me = AuthContext.currentUserId();
        if (account == null || me == null || !me.equals(account.assignedAgentId())) {
            throw new BusinessRuleException(
                    "Account " + accountId + " is not allocated to you.", "ACCOUNT_NOT_ALLOCATED");
        }
    }

    /** Best-effort lifecycle cascade to account-service; a downstream outage never fails the approval. */
    private void cascadeAccountStatus(String accountId, AccountStatus status) {
        if (accountId == null) return;
        try {
            accountClient.updateStatus(accountId, status);
            log.info("Cascaded account {} -> {} after restructuring approval", accountId, status);
        } catch (Exception ex) {
            log.warn("Account status cascade ({} -> {}) failed; continuing", accountId, status, ex);
        }
    }

    private RestructuringProposal find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restructuring proposal not found: " + id));
    }

    private void audit(String userId, String action, String recordId) {
        authClient.audit(new AuditLogRequest(userId, action, ENTITY, recordId, SOURCE));
        AuditContext.markRecorded();
    }
}
