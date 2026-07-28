package com.debtpulse.account.feign.fallback;

import com.debtpulse.account.feign.AuthClient;
import com.debtpulse.account.feign.dto.AuditLogRequest;
import com.debtpulse.account.feign.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Safe defaults when auth-service is unavailable (Resilience4j circuit open). */
@Component
public class AuthClientFallback implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClientFallback.class);

    @Override
    public UserDto getUser(String id) {
        log.warn("auth-service unavailable — getUser({}) falling back to null", id);
        return null;
    }

    @Override
    public boolean userExists(String id) {
        log.warn("auth-service unavailable — userExists({}) falling back to false", id);
        return false;
    }

    @Override
    public UserDto firstByRole(String role) {
        log.warn("auth-service unavailable — firstByRole({}) falling back to null", role);
        return null;
    }

    @Override
    public List<UserDto> activeByRole(String role, String branchId) {
        log.warn("auth-service unavailable — activeByRole({}) falling back to empty list", role);
        return List.of();
    }

    @Override
    public void audit(AuditLogRequest req) {
        log.warn("auth-service unavailable — audit action '{}' on {} dropped",
                req == null ? null : req.action(), req == null ? null : req.recordId());
    }
}
