package com.debtpulse.legal.feign.dto;

/**
 * Local copy of auth-service's {@code AuditLogRequest} JSON contract (INTERNAL_CONTRACTS),
 * POSTed to {@code /api/internal/audit-logs} to record a business action centrally.
 */
public record AuditLogRequest(
        String userId,
        String action,
        String entityType,
        String recordId,
        String sourceService
) {}
