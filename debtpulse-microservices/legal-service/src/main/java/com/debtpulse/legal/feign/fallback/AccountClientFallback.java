package com.debtpulse.legal.feign.fallback;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.legal.feign.AccountClient;
import com.debtpulse.legal.feign.dto.AccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilience4j fallback for {@link AccountClient} — returns safe defaults when account-service is down. */
@Component
public class AccountClientFallback implements AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClientFallback.class);

    @Override
    public AccountDto getAccount(String id) {
        log.warn("account-service unavailable — getAccount({}) fell back to null", id);
        return null;
    }

    @Override
    public boolean accountExists(String id) {
        log.warn("account-service unavailable — accountExists({}) fell back to false", id);
        return false;
    }

    @Override
    public void updateStatus(String id, AccountStatus status) {
        // Best-effort cascade — dropping it must not fail legal-case creation.
        log.warn("account-service unavailable — updateStatus({}, {}) skipped", id, status);
    }
}
