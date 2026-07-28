package com.debtpulse.legal.service.impl;

import com.debtpulse.legal.exception.BusinessRuleException;
import com.debtpulse.legal.exception.ResourceNotFoundException;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.legal.dto.request.CourtHearingRequest;
import com.debtpulse.legal.dto.request.LegalCaseRequest;
import com.debtpulse.legal.dto.request.RecoveryOrderRequest;
import com.debtpulse.legal.dto.response.CourtHearingDto;
import com.debtpulse.legal.dto.response.LegalCaseDto;
import com.debtpulse.legal.dto.response.RecoveryOrderDto;
import com.debtpulse.legal.entity.CourtHearing;
import com.debtpulse.legal.entity.LegalCase;
import com.debtpulse.legal.entity.RecoveryOrder;
import com.debtpulse.legal.feign.AccountClient;
import com.debtpulse.legal.feign.AuthClient;
import com.debtpulse.legal.feign.NotificationClient;
import com.debtpulse.legal.feign.dto.AuditLogRequest;
import com.debtpulse.legal.feign.dto.NotificationRequest;
import com.debtpulse.legal.mapper.LegalMapper;
import com.debtpulse.legal.repository.CourtHearingRepository;
import com.debtpulse.legal.repository.LegalCaseRepository;
import com.debtpulse.legal.repository.RecoveryOrderRepository;
import com.debtpulse.legal.service.LegalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegalServiceImpl implements LegalService {

    private static final Logger log = LoggerFactory.getLogger(LegalServiceImpl.class);
    private static final String SOURCE = "legal-service";
    private static final String CATEGORY_LEGAL = "LEGAL";

    private final LegalCaseRepository caseRepo;
    private final CourtHearingRepository hearingRepo;
    private final RecoveryOrderRepository orderRepo;
    private final LegalMapper mapper;
    private final AccountClient accountClient;
    private final AuthClient authClient;
    private final NotificationClient notificationClient;

    public LegalServiceImpl(LegalCaseRepository caseRepo, CourtHearingRepository hearingRepo,
                            RecoveryOrderRepository orderRepo, LegalMapper mapper,
                            AccountClient accountClient, AuthClient authClient,
                            NotificationClient notificationClient) {
        this.caseRepo = caseRepo;
        this.hearingRepo = hearingRepo;
        this.orderRepo = orderRepo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.authClient = authClient;
        this.notificationClient = notificationClient;
    }

    // ---------------------------------------------------------------- cases

    @Override
    @Transactional
    public LegalCaseDto initiateCase(LegalCaseRequest req) {
        if (!accountClient.accountExists(req.accountId())) {
            throw new BusinessRuleException("Account not found: " + req.accountId(), "ACCOUNT_NOT_FOUND");
        }
        String officerId = AuthContext.currentUserId();
        LegalCase legalCase = LegalCase.builder()
                .accountId(req.accountId())
                .legalOfficerId(officerId)
                .caseType(req.caseType())
                .filingDate(req.filingDate())
                .courtName(req.courtName())
                .caseNumber(req.caseNumber())
                .status(req.status() != null ? req.status() : CaseStatus.FILED)
                .build();
        LegalCase saved = caseRepo.save(legalCase);
        log.info("Legal case filed id={} accountId={} type={} officer={}",
                saved.getCaseId(), saved.getAccountId(), saved.getCaseType(), officerId);

        audit("CASE_FILED", "LegalCase", saved.getCaseId());
        cascadeAccountStatus(saved.getAccountId(), AccountStatus.LEGAL);
        notifyOfficer(officerId, "Legal case " + saved.getCaseNumber()
                + " filed against account " + saved.getAccountId());
        return mapper.toDto(saved);
    }

    @Override
    public Page<LegalCaseDto> listCases(Pageable pageable) {
        return caseRepo.findAll(pageable).map(mapper::toDto);
    }

    @Override
    public LegalCaseDto getCase(String id) {
        return mapper.toDto(findCase(id));
    }

    @Override
    @Transactional
    public LegalCaseDto updateCase(String id, LegalCaseRequest req) {
        LegalCase legalCase = findCase(id);
        if (req.caseType() != null) legalCase.setCaseType(req.caseType());
        if (req.filingDate() != null) legalCase.setFilingDate(req.filingDate());
        if (req.courtName() != null) legalCase.setCourtName(req.courtName());
        if (req.caseNumber() != null) legalCase.setCaseNumber(req.caseNumber());
        if (req.status() != null) legalCase.setStatus(req.status());
        LegalCase saved = caseRepo.save(legalCase);
        log.info("Legal case updated id={} status={}", saved.getCaseId(), saved.getStatus());
        audit("CASE_UPDATED", "LegalCase", saved.getCaseId());
        return mapper.toDto(saved);
    }

    // ------------------------------------------------------------- hearings

    @Override
    @Transactional
    public CourtHearingDto addHearing(CourtHearingRequest req) {
        LegalCase legalCase = findCase(req.caseId());
        CourtHearing hearing = CourtHearing.builder()
                .legalCase(legalCase)
                .hearingDate(req.hearingDate())
                .hearingOutcome(req.hearingOutcome())
                .nextHearingDate(req.nextHearingDate())
                .notes(req.notes())
                .build();
        CourtHearing saved = hearingRepo.save(hearing);

        // A future hearing has been scheduled -> reflect it on the case.
        if (req.nextHearingDate() != null) {
            legalCase.setStatus(CaseStatus.HEARING_SCHEDULED);
            caseRepo.save(legalCase);
            log.info("Case id={} moved to HEARING_SCHEDULED (next hearing {})",
                    legalCase.getCaseId(), req.nextHearingDate());
        }
        log.info("Court hearing recorded id={} case={} outcome={}",
                saved.getHearingId(), legalCase.getCaseId(), saved.getHearingOutcome());
        audit("HEARING_ADDED", "CourtHearing", saved.getHearingId());
        return mapper.toDto(saved);
    }

    @Override
    public List<CourtHearingDto> listHearings(String caseId) {
        // Ensure the case exists so a bad id returns 404 rather than an empty list.
        findCase(caseId);
        return hearingRepo.findByLegalCase_CaseId(caseId).stream().map(mapper::toDto).toList();
    }

    // --------------------------------------------------------------- orders

    @Override
    @Transactional
    public RecoveryOrderDto issueOrder(RecoveryOrderRequest req) {
        LegalCase legalCase = findCase(req.caseId());
        RecoveryOrder order = RecoveryOrder.builder()
                .legalCase(legalCase)
                .orderType(req.orderType())
                .issuedDate(req.issuedDate())
                .executionDeadline(req.executionDeadline())
                .status(req.status() != null ? req.status() : com.debtpulse.common.enums.OrderStatus.ISSUED)
                .build();
        RecoveryOrder saved = orderRepo.save(order);
        log.info("Recovery order issued id={} case={} type={}",
                saved.getOrderId(), legalCase.getCaseId(), saved.getOrderType());
        audit("ORDER_ISSUED", "RecoveryOrder", saved.getOrderId());
        return mapper.toDto(saved);
    }

    @Override
    public List<RecoveryOrderDto> listOrders() {
        List<RecoveryOrderDto> out = new ArrayList<>();
        orderRepo.findAll().forEach(o -> out.add(mapper.toDto(o)));
        return out;
    }

    @Override
    public RecoveryOrderDto getOrder(String id) {
        return mapper.toDto(findOrder(id));
    }

    @Override
    @Transactional
    public RecoveryOrderDto updateOrderStatus(String id, com.debtpulse.common.enums.OrderStatus status) {
        RecoveryOrder order = findOrder(id);
        order.setStatus(status);
        RecoveryOrder saved = orderRepo.save(order);
        log.info("Recovery order id={} status -> {}", id, status);
        audit("ORDER_STATUS_CHANGE", "RecoveryOrder", id);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteOrder(String id) {
        RecoveryOrder order = findOrder(id);
        orderRepo.delete(order);
        log.info("Recovery order deleted id={}", id);
        audit("ORDER_DELETED", "RecoveryOrder", id);
    }

    // ---------------------------------------------------------------- stats

    @Override
    public Map<String, Object> stats() {
        long total = caseRepo.count();
        long filed = caseRepo.countByStatus(CaseStatus.FILED);
        long decreed = caseRepo.countByStatus(CaseStatus.DECREED);
        long settled = caseRepo.countByStatus(CaseStatus.SETTLED);
        double conversionRate = total == 0 ? 0.0 : ((double) decreed / total) * 100.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCases", total);
        stats.put("filedCases", filed);
        stats.put("decreedCases", decreed);
        stats.put("settledCases", settled);
        stats.put("legalConversionRate", conversionRate);
        return stats;
    }

    // -------------------------------------------------------------- helpers

    private LegalCase findCase(String id) {
        return caseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legal case not found: " + id));
    }

    private RecoveryOrder findOrder(String id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery order not found: " + id));
    }

    /** Best-effort central audit; never breaks the primary transaction (fallback logs a warning). */
    private void audit(String action, String entityType, String recordId) {
        authClient.audit(new AuditLogRequest(AuthContext.currentUserId(), action, entityType, recordId, SOURCE));
    }

    /** Best-effort notification to a legal officer (fallback drops it if the service is down). */
    private void notifyOfficer(String officerId, String message) {
        if (officerId == null || officerId.isBlank()) return;
        notificationClient.notify(new NotificationRequest(officerId, message, CATEGORY_LEGAL));
    }

    /**
     * Best-effort lifecycle cascade to account-service (DP5-20). The circuit-breaker fallback absorbs a
     * downstream outage; this guard shields case creation from any other client error, so opening a
     * legal case can never be rolled back by the cascade.
     */
    private void cascadeAccountStatus(String accountId, AccountStatus status) {
        if (accountId == null) return;
        try {
            accountClient.updateStatus(accountId, status);
            log.info("Cascaded account {} -> {} after legal case filed", accountId, status);
        } catch (Exception ex) {
            log.warn("Account status cascade ({} -> {}) failed; continuing", accountId, status, ex);
        }
    }
}
