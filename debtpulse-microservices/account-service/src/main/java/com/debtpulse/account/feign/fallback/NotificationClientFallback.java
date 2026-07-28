package com.debtpulse.account.feign.fallback;

import com.debtpulse.account.feign.NotificationClient;
import com.debtpulse.account.feign.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link NotificationClient} — escalation alerts are best-effort. */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void notify(NotificationRequest req) {
        log.warn("notification-service unavailable — escalation notification dropped for user={} category={}",
                req.userId(), req.category());
    }
}
