package com.debtpulse.contact.feign;

import com.debtpulse.contact.feign.dto.AccountDto;
import com.debtpulse.contact.feign.fallback.AccountClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for account-service internal endpoints (INTERNAL_CONTRACTS.md).
 * Used to validate that a referenced account exists before logging contact/PTP records.
 * The shared {@code FeignClientInterceptor} (registered globally by {@code FeignConfig})
 * propagates identity headers on every call.
 */
@FeignClient(name = "account-service", path = "/api/internal", fallback = AccountClientFallback.class)
public interface AccountClient {

    @GetMapping("/accounts/{id}/exists")
    boolean accountExists(@PathVariable String id);

    @GetMapping("/accounts/{id}")
    AccountDto getAccount(@PathVariable String id);
}
