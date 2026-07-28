package com.debtpulse.analytics.service;

import com.debtpulse.analytics.entity.RecoveryReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

/** Recovery analytics dashboards, aggregated across all domain services (2.7). */
public interface AnalyticsService {

    Map<String, Object> dashboard();

    Map<String, Object> bucketDistribution();

    Map<String, Object> ptpMetrics();

    Map<String, Object> settlementMetrics();

    Map<String, Object> recoveryRate();

    Map<String, Object> bucketMigration();

    Map<String, Object> cashCollected();

    Map<String, Object> fieldVisitSuccess();

    Map<String, Object> legalConversion();

    RecoveryReport generateReport(String scope);

    Page<RecoveryReport> listReports(String scope, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
