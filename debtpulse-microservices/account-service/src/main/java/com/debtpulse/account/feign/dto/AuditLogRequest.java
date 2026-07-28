package com.debtpulse.account.feign.dto;

/**
 * Local copy of auth-service's {@code AuditLogRequest} JSON contract. POSTed to
 * {@code /api/internal/audit-logs} to record a business action in the central audit trail.
 */
public record AuditLogRequest(
        String userId,
        String action,
        String entityType,
        String recordId,
        String sourceService
) {}
