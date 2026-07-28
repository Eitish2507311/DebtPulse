package com.debtpulse.analytics.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Circuit-breaker fallbacks: when a domain service is unreachable, return an empty metric
 * map so the dashboard still renders (with that section blank) instead of erroring out.
 */
public final class StatsFallbacks {

    private static final Logger log = LoggerFactory.getLogger(StatsFallbacks.class);

    private StatsFallbacks() {}

    private static Map<String, Object> empty(String svc) {
        log.warn("Falling back to empty metrics — {} unavailable", svc);
        return Map.of();
    }

    @Component
    public static class AccountFallback implements StatsClients.AccountStatsClient {
        public Map<String, Object> accountStats() { return empty("account-service"); }
    }

    @Component
    public static class ContactFallback implements StatsClients.ContactStatsClient {
        public Map<String, Object> ptpStats() { return empty("contact-service ptp"); }
        public Map<String, Object> contactStats() { return empty("contact-service contacts"); }
    }

    @Component
    public static class SettlementFallback implements StatsClients.SettlementStatsClient {
        public Map<String, Object> settlementStats() { return empty("settlement-service"); }
    }

    @Component
    public static class LegalFallback implements StatsClients.LegalStatsClient {
        public Map<String, Object> legalStats() { return empty("legal-service"); }
    }

    @Component
    public static class FieldFallback implements StatsClients.FieldStatsClient {
        public Map<String, Object> visitStats() { return empty("field-service"); }
    }
}
