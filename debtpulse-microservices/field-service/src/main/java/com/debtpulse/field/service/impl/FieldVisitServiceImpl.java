package com.debtpulse.field.service.impl;

import com.debtpulse.field.exception.BusinessRuleException;
import com.debtpulse.field.exception.ResourceNotFoundException;
import com.debtpulse.field.exception.UnauthorizedActionException;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.audit.AuditContext;
import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.feign.dto.UserDto;
import com.debtpulse.field.dto.request.CompleteVisitRequest;
import com.debtpulse.field.dto.request.ScheduleVisitRequest;
import com.debtpulse.field.dto.response.FieldVisitDto;
import com.debtpulse.field.entity.FieldVisit;
import com.debtpulse.field.feign.AccountClient;
import com.debtpulse.field.feign.AuthClient;
import com.debtpulse.field.feign.NotificationClient;
import com.debtpulse.field.feign.dto.AuditLogRequest;
import com.debtpulse.field.feign.dto.NotificationRequest;
import com.debtpulse.field.mapper.FieldVisitMapper;
import com.debtpulse.field.repository.FieldVisitRepository;
import com.debtpulse.field.repository.FieldVisitSpecifications;
import com.debtpulse.field.service.FieldVisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FieldVisitServiceImpl implements FieldVisitService {

    private static final Logger log = LoggerFactory.getLogger(FieldVisitServiceImpl.class);
    private static final String SOURCE = "field-service";
    private static final String CATEGORY = "FIELD_VISIT";

    private final FieldVisitRepository repo;
    private final FieldVisitMapper mapper;
    private final AccountClient accountClient;
    private final AuthClient authClient;
    private final NotificationClient notificationClient;

    public FieldVisitServiceImpl(FieldVisitRepository repo, FieldVisitMapper mapper,
                                 AccountClient accountClient, AuthClient authClient,
                                 NotificationClient notificationClient) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.authClient = authClient;
        this.notificationClient = notificationClient;
    }

    @Override
    public FieldVisitDto schedule(ScheduleVisitRequest req) {
        if (!accountClient.accountExists(req.accountId())) {
            throw new BusinessRuleException("Account not found: " + req.accountId(), "ACCOUNT_NOT_FOUND");
        }
        // A field officer may only schedule visits for accounts allocated to them.
        assertOfficerOwnsAccount(req.accountId());
        // The assigned officer must exist, be ACTIVE, and actually be a FIELD_OFFICER (#11).
        UserDto officer = authClient.getUser(req.officerId());
        if (officer == null) {
            throw new BusinessRuleException("Field officer not found: " + req.officerId(), "OFFICER_NOT_FOUND");
        }
        if (!"ACTIVE".equalsIgnoreCase(officer.status())) {
            throw new BusinessRuleException("Field officer is not active: " + req.officerId(), "OFFICER_NOT_ACTIVE");
        }
        if (!"FIELD_OFFICER".equalsIgnoreCase(officer.role())) {
            throw new BusinessRuleException(
                    "Assigned user " + req.officerId() + " is not a FIELD_OFFICER (role=" + officer.role() + ")",
                    "NOT_A_FIELD_OFFICER");
        }

        FieldVisit saved = repo.save(mapper.toEntity(req));
        log.info("Field visit scheduled id={} account={} officer={} date={}",
                saved.getVisitId(), saved.getAccountId(), saved.getOfficerId(), saved.getScheduledDate());

        notificationClient.notify(new NotificationRequest(saved.getOfficerId(),
                "You have a new field visit scheduled for account " + saved.getAccountId()
                        + " on " + saved.getScheduledDate(), CATEGORY));

        audit("SCHEDULE_VISIT", saved.getVisitId());
        return mapper.toDto(saved);
    }

    @Override
    public Page<FieldVisitDto> list(String accountId, String officerId, VisitStatus status,
                                    LocalDate from, LocalDate to, Pageable pageable) {
        return repo.findAll(
                FieldVisitSpecifications.withFilters(accountId, officerId, status, from, to), pageable)
                .map(mapper::toDto);
    }

    @Override
    public List<FieldVisitDto> myVisits(String officerId) {
        return repo.findByOfficerId(officerId).stream().map(mapper::toDto).toList();
    }

    @Override
    public FieldVisitDto complete(String id, CompleteVisitRequest req) {
        FieldVisit visit = find(id);
        assertCanModify(visit);
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setVisitDate(req.visitDate() != null ? req.visitDate() : LocalDate.now());
        visit.setBorrowerMet(req.borrowerMet());
        visit.setAssetSighted(req.assetSighted());
        visit.setOutcomeSummary(req.outcomeSummary());
        if (req.nextActionRequired() != null) visit.setNextActionRequired(req.nextActionRequired());

        FieldVisit saved = repo.save(visit);
        log.info("Field visit completed id={} borrowerMet={} assetSighted={}",
                saved.getVisitId(), saved.getBorrowerMet(), saved.getAssetSighted());

        notificationClient.notify(new NotificationRequest(saved.getOfficerId(),
                "Field visit " + saved.getVisitId() + " for account " + saved.getAccountId()
                        + " has been marked COMPLETED", CATEGORY));

        audit("COMPLETE_VISIT", saved.getVisitId());
        return mapper.toDto(saved);
    }

    @Override
    public FieldVisitDto markMissed(String id) {
        FieldVisit visit = find(id);
        assertCanModify(visit);
        visit.setStatus(VisitStatus.MISSED);
        FieldVisit saved = repo.save(visit);
        log.info("Field visit marked MISSED id={}", saved.getVisitId());
        audit("MISS_VISIT", saved.getVisitId());
        return mapper.toDto(saved);
    }

    @Override
    public Map<String, Object> stats() {
        long total = repo.count();
        long completed = repo.countByStatus(VisitStatus.COMPLETED);
        long missed = repo.countByStatus(VisitStatus.MISSED);
        double successRate = total == 0 ? 0.0 : (completed * 100.0) / total;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVisits", total);
        stats.put("completedVisits", completed);
        stats.put("missedVisits", missed);
        stats.put("fieldVisitSuccessRate", successRate);
        return stats;
    }

    private FieldVisit find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field visit not found: " + id));
    }

    /**
     * A field officer may only schedule visits for accounts allocated to them; admins and portfolio
     * managers are exempt. Fail-closed: if the account can't be read, deny.
     */
    private void assertOfficerOwnsAccount(String accountId) {
        if (!"FIELD_OFFICER".equals(AuthContext.currentRole())) {
            return;
        }
        com.debtpulse.field.feign.dto.AccountDto account = accountClient.getAccount(accountId);
        String me = AuthContext.currentUserId();
        if (account == null || me == null || !me.equals(account.assignedAgentId())) {
            throw new UnauthorizedActionException(
                    "Account " + accountId + " is not allocated to you.");
        }
    }

    /**
     * Object-level authorization (fixes IDOR): a FIELD_OFFICER may only modify a visit assigned to
     * them. ADMIN and PORTFOLIO_MANAGER (supervisory roles) may modify any visit. Anyone else → 403.
     */
    private void assertCanModify(FieldVisit visit) {
        String role = AuthContext.currentRole();
        if ("ADMIN".equals(role) || "PORTFOLIO_MANAGER".equals(role)) {
            return;
        }
        String currentUserId = AuthContext.currentUserId();
        if (currentUserId == null || !currentUserId.equals(visit.getOfficerId())) {
            throw new UnauthorizedActionException(
                    "You can only modify field visits assigned to you (visit " + visit.getVisitId() + ").");
        }
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(AuthContext.currentUserId(), action, "FieldVisit", recordId, SOURCE));
        AuditContext.markRecorded();
    }
}
