package com.debtpulse.auth.feign.fallback;

import com.debtpulse.auth.feign.NotificationClient;
import com.debtpulse.auth.feign.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link NotificationClient} — delivery is best-effort. */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void notify(NotificationRequest req) {
        log.warn("notification-service unavailable — security notification dropped for user={} category={}",
                req.userId(), req.category());
    }
}
