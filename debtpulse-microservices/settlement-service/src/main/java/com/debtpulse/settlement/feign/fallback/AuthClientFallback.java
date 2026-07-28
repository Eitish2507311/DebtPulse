package com.debtpulse.settlement.feign.fallback;

import com.debtpulse.settlement.feign.AuthClient;
import com.debtpulse.settlement.feign.dto.AuditLogRequest;
import com.debtpulse.settlement.feign.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Safe defaults when auth-service is unavailable (circuit open). */
@Component
public class AuthClientFallback implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClientFallback.class);

    @Override
    public UserDto getUser(String id) {
        log.warn("auth-service unavailable — getUser({}) fell back to null", id);
        return null;
    }

    @Override
    public UserDto firstByRole(String role) {
        log.warn("auth-service unavailable — firstByRole({}) fell back to null", role);
        return null;
    }

    @Override
    public void audit(AuditLogRequest req) {
        log.warn("auth-service unavailable — audit action={} dropped", req == null ? null : req.action());
    }
}
