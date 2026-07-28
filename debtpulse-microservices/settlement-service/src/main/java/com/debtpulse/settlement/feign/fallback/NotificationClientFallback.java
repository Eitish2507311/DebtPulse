package com.debtpulse.settlement.feign.fallback;

import com.debtpulse.settlement.feign.NotificationClient;
import com.debtpulse.settlement.feign.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Safe default when notification-service is unavailable (circuit open). */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void notify(NotificationRequest req) {
        log.warn("notification-service unavailable — notification to userId={} dropped",
                req == null ? null : req.userId());
    }
}
