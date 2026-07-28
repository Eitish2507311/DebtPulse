package com.debtpulse.settlement.feign;

import com.debtpulse.settlement.feign.dto.AuditLogRequest;
import com.debtpulse.settlement.feign.dto.UserDto;
import com.debtpulse.settlement.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Resolves users/approvers and records audits against auth-service. */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable String id);

    @GetMapping("/users/by-role/{role}/first")
    UserDto firstByRole(@PathVariable String role);

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
