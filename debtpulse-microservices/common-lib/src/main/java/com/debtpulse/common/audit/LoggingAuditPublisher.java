package com.debtpulse.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link AuditPublisher}: emits a structured, greppable audit line on a dedicated logger
 * ({@code com.debtpulse.audit}) so audit output can be routed/retained independently and shipped to
 * a central store. Registered by {@link AuditAutoConfiguration} only when a service hasn't supplied
 * its own {@link AuditPublisher} (e.g. a DB/Kafka publisher) — the standard Boot "sensible default,
 * overridable" pattern.
 */
public class LoggingAuditPublisher implements AuditPublisher {

    private static final Logger audit = LoggerFactory.getLogger("com.debtpulse.audit");

    @Override
    public void publish(AuditEvent e) {
        audit.info("AUDIT action={} entity={} entityId={} outcome={} user={} role={} service={} ip={} "
                        + "correlationId={} requestId={} ts={} detail={}",
                e.action(), e.entity(), e.entityId(), e.outcome(), e.userId(), e.role(), e.service(),
                e.ipAddress(), e.correlationId(), e.requestId(), e.timestamp(), e.detail());
    }
}
