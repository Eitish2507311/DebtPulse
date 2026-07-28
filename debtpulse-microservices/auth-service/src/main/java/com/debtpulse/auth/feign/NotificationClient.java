package com.debtpulse.auth.feign;

import com.debtpulse.auth.feign.dto.NotificationRequest;
import com.debtpulse.auth.feign.fallback.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for notification-service. auth-service uses it to deliver security notifications
 * out-of-band (e.g. a password-reset token/link, category {@code SECURITY}) — the token must never
 * be returned in an API response.
 */
@FeignClient(name = "notification-service", path = "/api/internal", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/notifications")
    void notify(@RequestBody NotificationRequest req);
}
