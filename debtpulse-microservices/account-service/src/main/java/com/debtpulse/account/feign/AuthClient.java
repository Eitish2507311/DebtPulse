package com.debtpulse.account.feign;

import com.debtpulse.account.feign.dto.AuditLogRequest;
import com.debtpulse.account.feign.dto.UserDto;
import com.debtpulse.account.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for auth-service user lookups and central audit recording. Paths and DTO field
 * names match INTERNAL_CONTRACTS.md exactly.
 */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable String id);

    @GetMapping("/users/{id}/exists")
    boolean userExists(@PathVariable String id);

    @GetMapping("/users/by-role/{role}/first")
    UserDto firstByRole(@PathVariable String role);

    @GetMapping("/users/by-role/{role}/active")
    List<UserDto> activeByRole(@PathVariable String role,
                               @RequestParam(required = false) String branchId);

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
