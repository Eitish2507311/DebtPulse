package com.debtpulse.notification.feign.dto;

/**
 * Local copy of auth-service's {@code AuditLogRequest} (field/constructor order byte-for-byte
 * compatible per INTERNAL_CONTRACTS): {@code (userId, action, entityType, recordId, sourceService)}.
 */
public record AuditLogRequest(
        String userId,
        String action,
        String entityType,
        String recordId,
        String sourceService
) {}
