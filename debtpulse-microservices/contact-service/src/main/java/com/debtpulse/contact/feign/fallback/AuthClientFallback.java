package com.debtpulse.contact.feign.fallback;

import com.debtpulse.contact.feign.AuthClient;
import com.debtpulse.contact.feign.dto.AuditLogRequest;
import com.debtpulse.contact.feign.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resilience4j fallback for {@link AuthClient}. Auditing and user lookups are best-effort:
 * failures are logged but never block the primary business operation.
 */
@Component
public class AuthClientFallback implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClientFallback.class);

    @Override
    public UserDto getUser(String id) {
        log.warn("auth-service unavailable — returning null user for {} (fallback)", id);
        return null;
    }

    @Override
    public void audit(AuditLogRequest req) {
        log.warn("auth-service unavailable — audit dropped: action={} entity={} record={} (fallback)",
                req.action(), req.entityType(), req.recordId());
    }
}
