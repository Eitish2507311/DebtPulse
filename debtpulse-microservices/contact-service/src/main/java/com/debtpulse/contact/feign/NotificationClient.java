package com.debtpulse.contact.feign;

import com.debtpulse.contact.feign.dto.NotificationRequest;
import com.debtpulse.contact.feign.fallback.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for notification-service internal endpoints (INTERNAL_CONTRACTS.md).
 * Used to alert agents about PTP events.
 */
@FeignClient(name = "notification-service", path = "/api/internal", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest req);
}
