package com.debtpulse.analytics.feign;

import com.debtpulse.analytics.feign.dto.AuditLogRequest;
import com.debtpulse.analytics.feign.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for auth-service internal endpoints. analytics-service uses it only to record
 * state-changing actions (e.g. report generation) in the central audit trail.
 */
@FeignClient(name = "auth-service", path = "/api/internal", fallback = AuthClientFallback.class)
public interface AuthClient {

    @PostMapping("/audit-logs")
    void audit(@RequestBody AuditLogRequest req);
}
