package com.debtpulse.analytics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Feign clients that pull aggregated {@code /stats} metrics from every domain service.
 * Each has a Resilience4j fallback (see {@link StatsFallbacks}) so a single service being
 * down degrades the dashboard gracefully (empty metrics) instead of failing the whole call.
 */
public final class StatsClients {

    private StatsClients() {}

    @FeignClient(name = "account-service", path = "/api/internal",
            contextId = "accountStatsClient", fallback = StatsFallbacks.AccountFallback.class)
    public interface AccountStatsClient {
        @GetMapping("/accounts/stats")
        Map<String, Object> accountStats();
    }

    @FeignClient(name = "contact-service", path = "/api/internal",
            contextId = "contactStatsClient", fallback = StatsFallbacks.ContactFallback.class)
    public interface ContactStatsClient {
        @GetMapping("/ptp/stats")
        Map<String, Object> ptpStats();

        @GetMapping("/contacts/stats")
        Map<String, Object> contactStats();
    }

    @FeignClient(name = "settlement-service", path = "/api/internal",
            contextId = "settlementStatsClient", fallback = StatsFallbacks.SettlementFallback.class)
    public interface SettlementStatsClient {
        @GetMapping("/settlements/stats")
        Map<String, Object> settlementStats();
    }

    @FeignClient(name = "legal-service", path = "/api/internal",
            contextId = "legalStatsClient", fallback = StatsFallbacks.LegalFallback.class)
    public interface LegalStatsClient {
        @GetMapping("/legal/stats")
        Map<String, Object> legalStats();
    }

    @FeignClient(name = "field-service", path = "/api/internal",
            contextId = "fieldStatsClient", fallback = StatsFallbacks.FieldFallback.class)
    public interface FieldStatsClient {
        @GetMapping("/visits/stats")
        Map<String, Object> visitStats();
    }
}
