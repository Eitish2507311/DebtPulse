package com.debtpulse.common.audit;

/**
 * Sink for {@link AuditEvent}s (hexagonal port). The default {@link LoggingAuditPublisher} writes a
 * structured audit log line (shippable to a SIEM / log aggregator, 12-Factor "logs as event stream").
 * A service can override it with a bean that persists to the central audit store instead — the
 * {@link AuditAspect} depends only on this abstraction.
 */
public interface AuditPublisher {

    void publish(AuditEvent event);
}
