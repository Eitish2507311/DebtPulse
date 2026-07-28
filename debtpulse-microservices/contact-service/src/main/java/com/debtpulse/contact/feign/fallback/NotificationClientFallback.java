package com.debtpulse.contact.feign.fallback;

import com.debtpulse.contact.feign.NotificationClient;
import com.debtpulse.contact.feign.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resilience4j fallback for {@link NotificationClient}. Notifications are best-effort:
 * a failure is logged but never blocks the primary business operation.
 */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void notify(NotificationRequest req) {
        log.warn("notification-service unavailable — notification dropped for user={} category={} (fallback)",
                req.userId(), req.category());
    }
}
