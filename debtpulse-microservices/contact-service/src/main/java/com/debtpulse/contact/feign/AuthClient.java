package com.debtpulse.contact.feign;

import com.debtpulse.contact.feign.dto.AuditLogRequest;
import com.debtpulse.contact.feign.dto.UserDto;
import com.debtpulse.contact.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for auth-service internal endpoints (INTERNAL_CONTRACTS.md): user lookups
 * and central audit-log recording.
 */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable String id);

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
