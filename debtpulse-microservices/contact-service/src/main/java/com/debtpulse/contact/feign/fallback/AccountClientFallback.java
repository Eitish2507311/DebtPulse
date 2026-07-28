package com.debtpulse.contact.feign.fallback;

import com.debtpulse.contact.feign.AccountClient;
import com.debtpulse.contact.feign.dto.AccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resilience4j fallback for {@link AccountClient}. When account-service is unavailable,
 * account existence is reported as {@code false} (fail-closed) so callers surface a clear
 * business-rule error rather than proceeding on unverified data.
 */
@Component
public class AccountClientFallback implements AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClientFallback.class);

    @Override
    public boolean accountExists(String id) {
        log.warn("account-service unavailable — treating account {} as non-existent (fallback)", id);
        return false;
    }

    @Override
    public AccountDto getAccount(String id) {
        log.warn("account-service unavailable — returning null account for {} (fallback)", id);
        return null;
    }
}
