package com.debtpulse.common.audit;

import java.time.OffsetDateTime;

/**
 * An immutable audit record capturing a single business action. Designed for compliance,
 * investigations and user-activity tracking — carries the actor, the target, the outcome, and the
 * request-tracing ids so an action can be correlated end-to-end across services.
 */
public record AuditEvent(
        String correlationId,
        String requestId,
        String userId,
        String role,
        String service,
        String entity,
        String entityId,
        String action,
        String ipAddress,
        OffsetDateTime timestamp,
        String outcome,   // SUCCESS | FAILURE
        String detail     // optional (e.g. failure message)
) {}
