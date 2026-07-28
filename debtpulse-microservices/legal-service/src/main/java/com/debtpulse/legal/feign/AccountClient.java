package com.debtpulse.legal.feign;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.legal.feign.dto.AccountDto;
import com.debtpulse.legal.feign.fallback.AccountClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for account-service internal lookups (INTERNAL_CONTRACTS). */
@FeignClient(name = "account-service", path = "/api/internal", fallback = AccountClientFallback.class)
public interface AccountClient {

    @GetMapping("/accounts/{id}")
    AccountDto getAccount(@PathVariable String id);

    @GetMapping("/accounts/{id}/exists")
    boolean accountExists(@PathVariable String id);

    /** DP5-20 lifecycle cascade: move the account to LEGAL when a legal case is opened against it. */
    @PatchMapping("/accounts/{id}/status")
    void updateStatus(@PathVariable String id, @RequestParam AccountStatus status);
}
