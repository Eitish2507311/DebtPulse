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
        // Fail-safe: report a non-zero count so the escalation engine treats the account as
        // PTP-protected and leaves it in place while contact-service is unavailable — never escalate
        // an account whose promise-to-pay status we could not confirm.
        log.warn("contact-service unavailable — activePtpCount({}) failing safe to 1 (treat as active PTP)", accountId);
        return 1L;
    }
}
