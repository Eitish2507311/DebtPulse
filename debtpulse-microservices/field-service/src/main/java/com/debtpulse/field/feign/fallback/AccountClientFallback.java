package com.debtpulse.field.feign.fallback;

import com.debtpulse.field.feign.AccountClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link AccountClient} — safe defaults when account-service is down. */
@Component
public class AccountClientFallback implements AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClientFallback.class);

    @Override
    public boolean accountExists(String id) {
        log.warn("account-service unavailable — accountExists({}) falling back to false", id);
        return false;
    }

    @Override
    public void markCollateralVerified(String assetId) {
        log.warn("account-service unavailable — markCollateralVerified({}) skipped (no-op)", assetId);
    }
}
