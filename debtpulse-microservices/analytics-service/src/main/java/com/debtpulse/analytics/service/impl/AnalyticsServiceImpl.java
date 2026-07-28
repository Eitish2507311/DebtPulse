package com.debtpulse.analytics.service.impl;

import com.debtpulse.analytics.entity.RecoveryReport;
import com.debtpulse.analytics.feign.StatsClients.*;
import com.debtpulse.analytics.repository.RecoveryReportRepository;
import com.debtpulse.analytics.repository.ReportSpecifications;
import com.debtpulse.analytics.service.AnalyticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final AccountStatsClient accountClient;
    private final ContactStatsClient contactClient;
    private final SettlementStatsClient settlementClient;
    private final LegalStatsClient legalClient;
    private final FieldStatsClient fieldClient;
    private final RecoveryReportRepository reportRepo;
    private final ObjectMapper objectMapper;

    public AnalyticsServiceImpl(AccountStatsClient accountClient, ContactStatsClient contactClient,
                                SettlementStatsClient settlementClient, LegalStatsClient legalClient,
                                FieldStatsClient fieldClient, RecoveryReportRepository reportRepo,
                                ObjectMapper objectMapper) {
        this.accountClient = accountClient;
        this.contactClient = contactClient;
        this.settlementClient = settlementClient;
        this.legalClient = legalClient;
        this.fieldClient = fieldClient;
        this.reportRepo = reportRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("portfolio", accountClient.accountStats());
        dash.put("ptp", contactClient.ptpStats());
        dash.put("contacts", contactClient.contactStats());
        dash.put("settlements", settlementClient.settlementStats());
        dash.put("legal", legalClient.legalStats());
        dash.put("fieldVisits", fieldClient.visitStats());
        dash.put("recoveryRate", recoveryRate());
        return dash;
    }

    @Override
    public Map<String, Object> bucketDistribution() {
        Map<String, Object> stats = accountClient.accountStats();
        Object byBucket = stats.getOrDefault("byBucket", Map.of());
        return Map.of("byBucket", byBucket, "totalAccounts", stats.getOrDefault("totalAccounts", 0));
    }

    @Override
    public Map<String, Object> ptpMetrics() {
        return contactClient.ptpStats();
    }

    @Override
    public Map<String, Object> settlementMetrics() {
        return settlementClient.settlementStats();
    }

    @Override
    public Map<String, Object> recoveryRate() {
        Map<String, Object> account = accountClient.accountStats();
        Map<String, Object> settlement = settlementClient.settlementStats();
        double totalOverdue = toDouble(account.get("totalOverdue"));
        double settledAccounts = toDouble(account.get("settledAccounts"));
        double totalAccounts = toDouble(account.get("totalAccounts"));
        double paid = toDouble(settlement.get("paidSettlements"));
        double recoveryRatePct = totalAccounts == 0 ? 0.0 : round((settledAccounts / totalAccounts) * 100.0);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalOverdue", totalOverdue);
        res.put("settledAccounts", settledAccounts);
        res.put("totalAccounts", totalAccounts);
        res.put("paidSettlements", paid);
        res.put("recoveryRatePercent", recoveryRatePct);
        return res;
    }

    @Override
    public Map<String, Object> bucketMigration() {
        // Phase 1 approximation: current bucket distribution snapshot (no historical migration store yet).
        Map<String, Object> res = new LinkedHashMap<>(bucketDistribution());
        res.put("note", "Historical bucket migration tracking is deferred (Phase 2); showing current distribution.");
        return res;
    }

    @Override
    public Map<String, Object> cashCollected() {
        Map<String, Object> ptp = contactClient.ptpStats();
        Map<String, Object> settlement = settlementClient.settlementStats();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ptpKept", ptp.getOrDefault("keptPtp", 0));
        res.put("settlementsPaid", settlement.getOrDefault("paidSettlements", 0));
        return res;
    }

    @Override
    public Map<String, Object> fieldVisitSuccess() {
        return fieldClient.visitStats();
    }

    @Override
    public Map<String, Object> legalConversion() {
        return legalClient.legalStats();
    }

    @Override
    public RecoveryReport generateReport(String scope) {
        String json;
        try {
            json = objectMapper.writeValueAsString(dashboard());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize dashboard metrics", e);
            json = "{}";
        }
        RecoveryReport report = RecoveryReport.builder()
                .scope(scope == null ? "Portfolio" : scope)
                .metrics(json)
                .build();
        RecoveryReport saved = reportRepo.save(report);
        log.info("Recovery report generated id={} scope={}", saved.getReportId(), saved.getScope());
        return saved;
    }

    @Override
    public Page<RecoveryReport> listReports(String scope, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return reportRepo.findAll(ReportSpecifications.withFilters(scope, from, to), pageable);
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
