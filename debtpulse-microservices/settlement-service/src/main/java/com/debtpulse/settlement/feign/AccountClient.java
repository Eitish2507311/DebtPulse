package com.debtpulse.settlement.feign;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.settlement.feign.dto.AccountDto;
import com.debtpulse.settlement.feign.fallback.AccountClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Reads account data from account-service (Resilience4j circuit breaker + fallback). */
@FeignClient(name = "account-service", path = "/api/internal", fallback = AccountClientFallback.class)
public interface AccountClient {

    @GetMapping("/accounts/{id}")
    AccountDto getAccount(@PathVariable String id);

    @GetMapping("/accounts/{id}/exists")
    boolean accountExists(@PathVariable String id);

    /** DP5-18 lifecycle cascade: mark the account SETTLED once its settlement is paid. */
    @PatchMapping("/accounts/{id}/status")
    void updateStatus(@PathVariable String id, @RequestParam AccountStatus status);
}
