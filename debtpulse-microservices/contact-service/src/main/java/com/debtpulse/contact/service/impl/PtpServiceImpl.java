package com.debtpulse.contact.service.impl;

import com.debtpulse.contact.exception.BusinessRuleException;
import com.debtpulse.contact.exception.ResourceNotFoundException;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.enums.PtpStatus;
import com.debtpulse.common.enums.Role;
import com.debtpulse.contact.dto.request.PtpRequest;
import com.debtpulse.contact.dto.response.PtpDto;
import com.debtpulse.contact.entity.PromiseToPay;
import com.debtpulse.contact.feign.AccountClient;
import com.debtpulse.contact.feign.AuthClient;
import com.debtpulse.contact.feign.NotificationClient;
import com.debtpulse.contact.feign.dto.AuditLogRequest;
import com.debtpulse.contact.feign.dto.NotificationRequest;
import com.debtpulse.contact.mapper.PtpMapper;
import com.debtpulse.contact.repository.PromiseToPayRepository;
import com.debtpulse.contact.repository.PtpSpecifications;
import com.debtpulse.contact.service.PtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PtpServiceImpl implements PtpService {

    private static final Logger log = LoggerFactory.getLogger(PtpServiceImpl.class);
    private static final String SOURCE_SERVICE = "contact-service";
    private static final String CATEGORY_PTP = "PTP";

    private final PromiseToPayRepository repo;
    private final PtpMapper mapper;
    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final AuthClient authClient;

    public PtpServiceImpl(PromiseToPayRepository repo, PtpMapper mapper, AccountClient accountClient,
                          NotificationClient notificationClient, AuthClient authClient) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.notificationClient = notificationClient;
        this.authClient = authClient;
    }

    @Override
    public PtpDto create(PtpRequest req) {
        if (!accountClient.accountExists(req.accountId())) {
            throw new BusinessRuleException("Account not found: " + req.accountId(), "ACCOUNT_NOT_FOUND");
        }
        // One active promise per account: reject an identical ACTIVE PTP (same amount + commitment
        // date). A genuinely different commitment is allowed; to change an existing one, edit it.
        boolean duplicate = repo.findByAccountIdAndStatus(req.accountId(), PtpStatus.ACTIVE).stream()
                .anyMatch(p -> req.commitmentDate().equals(p.getCommitmentDate())
                        && p.getPtpAmount() != null && req.ptpAmount().compareTo(p.getPtpAmount()) == 0);
        if (duplicate) {
            throw new BusinessRuleException(
                    "An identical active PTP (same amount and commitment date) already exists for account "
                            + req.accountId() + ". Edit or reschedule the existing one instead.",
                    "DUPLICATE_ACTIVE_PTP");
        }
        String agentId = resolveAgentId(req.agentId());
        PromiseToPay entity = PromiseToPay.builder()
                .accountId(req.accountId())
                .agentId(agentId)
                .ptpDate(req.ptpDate())
                .ptpAmount(req.ptpAmount())
                .commitmentDate(req.commitmentDate())
                .status(PtpStatus.ACTIVE)
                .build();
        PromiseToPay saved = repo.save(entity);
        log.info("PTP created id={} account={} amount={} commitmentDate={}",
                saved.getPtpId(), saved.getAccountId(), saved.getPtpAmount(), saved.getCommitmentDate());

        if (agentId != null && !agentId.isBlank()) {
            notificationClient.notify(new NotificationRequest(agentId,
                    "New PTP of " + saved.getPtpAmount() + " for account " + saved.getAccountId()
                            + " due " + saved.getCommitmentDate(),
                    CATEGORY_PTP));
        }
        audit("CREATE", saved.getPtpId());
        return mapper.toDto(saved);
    }

    @Override
    public PtpDto getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    public PtpDto update(String id, PtpRequest req) {
        PromiseToPay entity = find(id);
        entity.setPtpDate(req.ptpDate());
        entity.setPtpAmount(req.ptpAmount());
        entity.setCommitmentDate(req.commitmentDate());
        PromiseToPay saved = repo.save(entity);
        log.info("PTP updated id={} amount={} commitmentDate={}", id, saved.getPtpAmount(), saved.getCommitmentDate());
        audit("UPDATE", id);
        return mapper.toDto(saved);
    }

    @Override
    public Page<PtpDto> list(String accountId, String agentId, PtpStatus status,
                             LocalDate from, LocalDate to, Pageable pageable) {
        return repo.findAll(
                PtpSpecifications.withFilters(accountId, agentId, status, from, to), pageable)
                .map(mapper::toDto);
    }

    @Override
    public PtpDto recordPayment(String id, BigDecimal actualPaidAmount) {
        PromiseToPay entity = find(id);
        entity.setActualPaidAmount(actualPaidAmount);
        boolean kept = actualPaidAmount != null
                && entity.getPtpAmount() != null
                && actualPaidAmount.compareTo(entity.getPtpAmount()) >= 0;
        entity.setStatus(kept ? PtpStatus.KEPT : PtpStatus.PARTIAL);
        PromiseToPay saved = repo.save(entity);
        log.info("PTP payment recorded id={} paid={} status={}", id, actualPaidAmount, saved.getStatus());
        audit("PAYMENT", id);
        return mapper.toDto(saved);
    }

    @Override
    public PtpDto reschedule(String id, LocalDate newCommitmentDate) {
        PromiseToPay entity = find(id);
        entity.setCommitmentDate(newCommitmentDate);
        entity.setStatus(PtpStatus.RESCHEDULED);
        PromiseToPay saved = repo.save(entity);
        log.info("PTP rescheduled id={} newCommitmentDate={}", id, newCommitmentDate);
        audit("RESCHEDULE", id);
        return mapper.toDto(saved);
    }

    @Override
    public long activeCount(String accountId) {
        return repo.countByAccountIdAndStatus(accountId, PtpStatus.ACTIVE);
    }

    @Override
    public Map<String, Object> stats() {
        long total = repo.count();
        long active = repo.countByStatus(PtpStatus.ACTIVE);
        long kept = repo.countByStatus(PtpStatus.KEPT);
        long broken = repo.countByStatus(PtpStatus.BROKEN);
        double breachRate = total == 0 ? 0.0 : (double) broken / total;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPtp", total);
        stats.put("activePtp", active);
        stats.put("keptPtp", kept);
        stats.put("brokenPtp", broken);
        stats.put("ptpBreachRate", breachRate);
        return stats;
    }

    @Override
    public int markBreachedPtps() {
        List<PromiseToPay> lapsed =
                repo.findByStatusAndCommitmentDateBefore(PtpStatus.ACTIVE, LocalDate.now());
        for (PromiseToPay ptp : lapsed) {
            ptp.setStatus(PtpStatus.BROKEN);
            repo.save(ptp);
            if (ptp.getAgentId() != null && !ptp.getAgentId().isBlank()) {
                notificationClient.notify(new NotificationRequest(ptp.getAgentId(),
                        "PTP broken for account " + ptp.getAccountId()
                                + " (committed " + ptp.getCommitmentDate() + ")",
                        CATEGORY_PTP));
            }
        }
        if (!lapsed.isEmpty()) {
            log.info("PTP breach sweep marked {} PTP(s) BROKEN", lapsed.size());
        }
        return lapsed.size();
    }

    private String resolveAgentId(String requestedAgentId) {
        if (Role.COLLECTIONS_AGENT.name().equals(AuthContext.currentRole())) {
            return AuthContext.currentUserId();
        }
        return requestedAgentId;
    }

    private PromiseToPay find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PTP not found: " + id));
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(
                AuthContext.currentUserId(), action, "PromiseToPay", recordId, SOURCE_SERVICE));
        com.debtpulse.common.audit.AuditContext.markRecorded();
    }
}
