package com.debtpulse.auth.audit;

import com.debtpulse.auth.dto.request.AuditLogRequest;
import com.debtpulse.auth.service.AuditService;
import com.debtpulse.common.audit.AuditEvent;
import com.debtpulse.common.audit.AuditPublisher;
import org.springframework.stereotype.Component;

/**
 * auth-service is the central audit store, so it records in-process (no Feign hop to itself),
 * overriding the default logging publisher. Powers both the automatic {@code HttpAuditAspect} and
 * the {@code @Auditable} {@code AuditAspect}.
 */
@Component
public class AuditServiceAuditPublisher implements AuditPublisher {

    private final AuditService auditService;

    public AuditServiceAuditPublisher(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void publish(AuditEvent event) {
        auditService.record(new AuditLogRequest(
                event.userId(), event.action(), event.entity(), event.entityId(), event.service()));
    }
}
