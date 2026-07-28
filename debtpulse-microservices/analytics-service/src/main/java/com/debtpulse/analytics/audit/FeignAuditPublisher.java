package com.debtpulse.analytics.audit;

import com.debtpulse.analytics.feign.AuthClient;
import com.debtpulse.analytics.feign.dto.AuditLogRequest;
import com.debtpulse.common.audit.AuditEvent;
import com.debtpulse.common.audit.AuditPublisher;
import org.springframework.stereotype.Component;

/**
 * Persists audit events centrally via auth-service (POST /api/internal/audit-logs), overriding the
 * default logging publisher. Powers the automatic HttpAuditAspect for this service.
 */
@Component
public class FeignAuditPublisher implements AuditPublisher {

    private final AuthClient authClient;

    public FeignAuditPublisher(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public void publish(AuditEvent event) {
        authClient.audit(new AuditLogRequest(
                event.userId(), event.action(), event.entity(), event.entityId(), event.service()));
    }
}
