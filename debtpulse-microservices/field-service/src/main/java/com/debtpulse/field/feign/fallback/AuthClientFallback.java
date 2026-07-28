package com.debtpulse.field.feign.fallback;

import com.debtpulse.field.feign.AuthClient;
import com.debtpulse.field.feign.dto.AuditLogRequest;
import com.debtpulse.field.feign.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link AuthClient} — safe defaults when auth-service is down. */
@Component
public class AuthClientFallback implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClientFallback.class);

    @Override
    public boolean userExists(String id) {
        log.warn("auth-service unavailable — userExists({}) falling back to false", id);
        return false;
    }

    @Override
    public UserDto getUser(String id) {
        log.warn("auth-service unavailable — getUser({}) falling back to null", id);
        return null;
    }

    @Override
    public void audit(AuditLogRequest req) {
        log.warn("auth-service unavailable — audit log dropped: action={} entity={} record={}",
                req.action(), req.entityType(), req.recordId());
    }
}
