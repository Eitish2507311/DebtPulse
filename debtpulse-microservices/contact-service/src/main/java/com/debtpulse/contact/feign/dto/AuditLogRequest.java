package com.debtpulse.contact.feign.dto;

/**
 * Local copy of auth-service's {@code AuditLogRequest} JSON (INTERNAL_CONTRACTS.md), posted
 * to {@code /api/internal/audit-logs} to record a business action in the central trail.
 */
public record AuditLogRequest(
        String userId,
        String action,
        String entityType,
        String recordId,
        String sourceService
) {}
