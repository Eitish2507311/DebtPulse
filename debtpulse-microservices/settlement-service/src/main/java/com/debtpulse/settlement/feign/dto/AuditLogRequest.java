package com.debtpulse.settlement.feign.dto;

/**
 * Local copy of auth-service's {@code AuditLogRequest} JSON contract
 * (see INTERNAL_CONTRACTS: {@code POST /api/internal/audit-logs}).
 */
public record AuditLogRequest(
        String userId,
        String action,
        String entityType,
        String recordId,
        String sourceService
) {}
