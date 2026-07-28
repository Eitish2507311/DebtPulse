package com.debtpulse.field.feign;

import com.debtpulse.field.feign.dto.AuditLogRequest;
import com.debtpulse.field.feign.dto.UserDto;
import com.debtpulse.field.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for auth-service internal endpoints. Used to validate that an assigned
 * officer is a real user and to record business actions in the central audit trail.
 */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/users/{id}/exists")
    boolean userExists(@PathVariable String id);

    /** Full user projection — used to verify an officer is ACTIVE and has the FIELD_OFFICER role. */
    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable String id);

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
