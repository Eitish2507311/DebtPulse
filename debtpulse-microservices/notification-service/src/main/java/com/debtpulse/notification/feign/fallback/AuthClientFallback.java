package com.debtpulse.notification.feign.fallback;

import com.debtpulse.notification.feign.AuthClient;
import com.debtpulse.notification.feign.dto.AuditLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link AuthClient} — auditing is best-effort, never blocks business ops. */
@Component
public class AuthClientFallback implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClientFallback.class);

    @Override
    public void audit(AuditLogRequest req) {
        log.warn("auth-service unavailable — audit dropped action={} entity={}", req.action(), req.entityType());
    }
}
