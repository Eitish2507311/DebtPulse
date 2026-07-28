package com.debtpulse.auth.service;

import com.debtpulse.auth.dto.request.AuditLogRequest;
import com.debtpulse.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/** Central audit trail: recording (from any service) and querying. */
public interface AuditService {

    AuditLog record(AuditLogRequest request);

    Page<AuditLog> getAll(String userId, String entityType, String action,
                          LocalDate from, LocalDate to, Pageable pageable);

    AuditLog getById(String id);

    Page<AuditLog> getByUser(String userId, Pageable pageable);

    List<AuditLog> getByEntity(String entityType, String recordId);
}
