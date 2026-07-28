package com.debtpulse.legal.feign;

import com.debtpulse.legal.feign.dto.NotificationRequest;
import com.debtpulse.legal.feign.fallback.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Feign client for notification-service (INTERNAL_CONTRACTS). */
@FeignClient(name = "notification-service", path = "/api/internal", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest req);
}
