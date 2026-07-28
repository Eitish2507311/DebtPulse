package com.debtpulse.auth.service.impl;

import com.debtpulse.auth.dto.request.AuditLogRequest;
import com.debtpulse.auth.entity.AuditLog;
import com.debtpulse.auth.repository.AuditLogRepository;
import com.debtpulse.auth.repository.AuditLogSpecifications;
import com.debtpulse.auth.service.AuditService;
import com.debtpulse.auth.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository repo;

    public AuditServiceImpl(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Override
    public AuditLog record(AuditLogRequest req) {
        AuditLog entry = AuditLog.builder()
                .userId(req.userId() == null || req.userId().isBlank() ? "SYSTEM" : req.userId())
                .action(req.action())
                .entityType(req.entityType())
                .recordId(req.recordId())
                .sourceService(req.sourceService())
                .timestamp(LocalDateTime.now())
                .build();
        AuditLog saved = repo.save(entry);
        log.debug("Audit recorded: {} {} {} by {}", saved.getAction(), saved.getEntityType(),
                saved.getRecordId(), saved.getUserId());
        return saved;
    }

    @Override
    public Page<AuditLog> getAll(String userId, String entityType, String action,
                                 LocalDate from, LocalDate to, Pageable pageable) {
        return repo.findAll(
                AuditLogSpecifications.withFilters(userId, entityType, action, from, to), pageable);
    }

    @Override
    public AuditLog getById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + id));
    }

    @Override
    public Page<AuditLog> getByUser(String userId, Pageable pageable) {
        return repo.findByUserId(userId, pageable);
    }

    @Override
    public List<AuditLog> getByEntity(String entityType, String recordId) {
        return repo.findByEntityTypeAndRecordId(entityType, recordId);
    }
}
