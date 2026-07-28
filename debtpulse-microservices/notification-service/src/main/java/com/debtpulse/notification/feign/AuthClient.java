package com.debtpulse.notification.feign;

import com.debtpulse.notification.feign.dto.AuditLogRequest;
import com.debtpulse.notification.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for auth-service internal endpoints. notification-service uses it to record its
 * state-changing actions (send / mark-read / dismiss) in the central audit trail.
 */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
