package com.debtpulse.legal.feign;

import com.debtpulse.legal.feign.dto.AuditLogRequest;
import com.debtpulse.legal.feign.dto.UserDto;
import com.debtpulse.legal.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Feign client for auth-service internal user lookups and the central audit trail. */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable String id);

    @GetMapping("/users/{id}/exists")
    boolean userExists(@PathVariable String id);

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
