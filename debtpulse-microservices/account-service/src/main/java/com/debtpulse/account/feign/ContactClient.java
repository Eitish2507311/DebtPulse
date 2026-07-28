package com.debtpulse.account.feign;

import com.debtpulse.account.feign.fallback.ContactClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for contact-service. Used during escalation to skip accounts that still have an
 * active Promise-To-Pay. Path and query match INTERNAL_CONTRACTS.md exactly.
 */
@FeignClient(name = "contact-service", path = "/api/internal", fallback = ContactClientFallback.class)
public interface ContactClient {

    @GetMapping("/ptp/active-count")
    long activePtpCount(@RequestParam String accountId);
}
