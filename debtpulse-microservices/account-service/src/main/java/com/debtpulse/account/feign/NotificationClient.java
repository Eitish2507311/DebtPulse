package com.debtpulse.account.feign;

import com.debtpulse.account.feign.dto.NotificationRequest;
import com.debtpulse.account.feign.fallback.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for notification-service. Used to alert an officer when an account is escalated
 * to them by the allocation engine (category {@code ESCALATION}).
 */
@FeignClient(name = "notification-service", path = "/api/internal", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest req);
}
