package com.debtpulse.field.feign;

import com.debtpulse.field.feign.dto.NotificationRequest;
import com.debtpulse.field.feign.fallback.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for notification-service. Used to alert field officers about newly scheduled
 * or completed visits (category {@code FIELD_VISIT}).
 */
@FeignClient(name = "notification-service", path = "/api/internal", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest req);
}
