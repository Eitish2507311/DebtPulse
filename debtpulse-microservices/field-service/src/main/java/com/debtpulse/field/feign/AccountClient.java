package com.debtpulse.field.feign;

import com.debtpulse.field.feign.dto.AccountDto;
import com.debtpulse.field.feign.dto.CollateralDto;
import com.debtpulse.field.feign.fallback.AccountClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for account-service internal endpoints. Used to validate that a scheduled
 * visit targets a real account and to flag a collateral asset VERIFIED once it has been
 * physically checked in the field.
 */
@FeignClient(name = "account-service", path = "/api/internal", fallback = AccountClientFallback.class)
public interface AccountClient {

    @GetMapping("/accounts/{id}/exists")
    boolean accountExists(@PathVariable String id);

    @GetMapping("/accounts/{id}")
    AccountDto getAccount(@PathVariable String id);

    @GetMapping("/collateral-assets/{assetId}")
    CollateralDto getCollateral(@PathVariable String assetId);

    @PostMapping("/collateral-assets/{assetId}/mark-verified")
    void markCollateralVerified(@PathVariable String assetId);
}
