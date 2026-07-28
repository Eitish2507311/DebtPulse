package com.debtpulse.legal.feign.fallback;

import com.debtpulse.legal.feign.AuthClient;
import com.debtpulse.legal.feign.dto.AuditLogRequest;
import com.debtpulse.legal.feign.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link AuthClient} — degrades gracefully when auth-service is down. */
@Component
public class AuthClientFallback implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClientFallback.class);

    @Override
    public UserDto getUser(String id) {
        log.warn("auth-service unavailable — getUser({}) fell back to null", id);
        return null;
    }

    @Override
    public boolean userExists(String id) {
        log.warn("auth-service unavailable — userExists({}) fell back to false", id);
        return false;
    }

    @Override
    public void audit(AuditLogRequest req) {
        log.warn("auth-service unavailable — audit log dropped: action={} entity={}",
                req == null ? null : req.action(), req == null ? null : req.entityType());
    }
}
