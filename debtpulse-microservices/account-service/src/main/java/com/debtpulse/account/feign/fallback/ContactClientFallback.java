package com.debtpulse.account.feign.fallback;

import com.debtpulse.account.feign.ContactClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Safe defaults when contact-service is unavailable (Resilience4j circuit open). */
@Component
public class ContactClientFallback implements ContactClient {

    private static final Logger log = LoggerFactory.getLogger(ContactClientFallback.class);

    @Override
    public long activePtpCount(String accountId) {
        log.warn("contact-service unavailable — activePtpCount({}) falling back to 0", accountId);
        return 0L;
    }
}
