package com.debtpulse.field.feign.fallback;

import com.debtpulse.field.feign.NotificationClient;
import com.debtpulse.field.feign.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link NotificationClient} — notifications are best-effort. */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void notify(NotificationRequest req) {
        log.warn("notification-service unavailable — notification dropped for user={} category={}",
                req.userId(), req.category());
    }
}
