package com.debtpulse.field.feign.dto;

/**
 * Local copy of auth-service's {@code AuditLogRequest} JSON (field names byte-for-byte
 * compatible per INTERNAL_CONTRACTS). POSTed to {@code /api/internal/audit-logs}.
 */
public record AuditLogRequest(
        String userId,
        String action,
        String entityType,
        String recordId,
        String sourceService
) {}
