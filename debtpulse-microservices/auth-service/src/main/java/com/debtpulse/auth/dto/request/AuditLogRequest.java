package com.debtpulse.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload other microservices POST to {@code /api/internal/audit-logs} (over Feign)
 * to record a business action in the central audit trail.
 */
public record AuditLogRequest(
        String userId,

        @NotBlank(message = "Action is required")
        String action,

        @NotBlank(message = "Entity type is required")
        String entityType,

        String recordId,

        String sourceService
) {}
