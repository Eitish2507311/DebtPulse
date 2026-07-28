package com.debtpulse.legal.feign.fallback;

import com.debtpulse.legal.feign.NotificationClient;
import com.debtpulse.legal.feign.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link NotificationClient} — drops the notification when the service is down. */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void notify(NotificationRequest req) {
        log.warn("notification-service unavailable — notification dropped for user={}",
                req == null ? null : req.userId());
    }
}
