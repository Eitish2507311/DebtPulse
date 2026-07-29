package com.debtpulse.analytics.controller;

import com.debtpulse.analytics.entity.RecoveryReport;
import com.debtpulse.analytics.service.AnalyticsService;
import com.debtpulse.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'PORTFOLIO_MANAGER')")
@Tag(name = "Analytics", description = "Recovery analytics & reporting dashboards (2.7)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // The summary dashboard + bucket distribution are read-only org KPIs shown on EVERY role's
    // home dashboard, so collections agents may read them too (kept in sync across roles). The
    // detailed analytics/report endpoints below stay manager-only via the class-level rule.
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','PORTFOLIO_MANAGER','COLLECTIONS_AGENT')")
    @Operation(summary = "Consolidated recovery dashboard aggregated from all services")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(analyticsService.dashboard());
    }

    @GetMapping("/bucket-distribution")
    @PreAuthorize("hasAnyRole('ADMIN','PORTFOLIO_MANAGER','COLLECTIONS_AGENT')")
    public ResponseEntity<Map<String, Object>> bucketDistribution() {
        return ResponseEntity.ok(analyticsService.bucketDistribution());
    }

    @GetMapping("/ptp-metrics")
    public ResponseEntity<Map<String, Object>> ptpMetrics() {
        return ResponseEntity.ok(analyticsService.ptpMetrics());
    }

    @GetMapping("/settlement-metrics")
    public ResponseEntity<Map<String, Object>> settlementMetrics() {
        return ResponseEntity.ok(analyticsService.settlementMetrics());
    }

    @GetMapping("/recovery-rate")
    public ResponseEntity<Map<String, Object>> recoveryRate() {
        return ResponseEntity.ok(analyticsService.recoveryRate());
    }

    @GetMapping("/bucket-migration")
    public ResponseEntity<Map<String, Object>> bucketMigration() {
        return ResponseEntity.ok(analyticsService.bucketMigration());
    }

    @GetMapping("/cash-collected")
    public ResponseEntity<Map<String, Object>> cashCollected() {
        return ResponseEntity.ok(analyticsService.cashCollected());
    }

    @GetMapping("/field-visit-success")
    public ResponseEntity<Map<String, Object>> fieldVisitSuccess() {
        return ResponseEntity.ok(analyticsService.fieldVisitSuccess());
    }

    @GetMapping("/legal-conversion")
    public ResponseEntity<Map<String, Object>> legalConversion() {
        return ResponseEntity.ok(analyticsService.legalConversion());
    }

    @PostMapping("/reports/generate")
    @Operation(summary = "Generate and persist a recovery report snapshot")
    public ResponseEntity<RecoveryReport> generateReport(@RequestParam(required = false) String scope) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analyticsService.generateReport(scope));
    }

    @GetMapping("/reports")
    @Operation(summary = "List persisted report snapshots (paginated; optional scope + generated-date range)")
    public ResponseEntity<PageResponse<RecoveryReport>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedDate").descending());
        LocalDateTime fromTs = from != null ? from.atStartOfDay() : null;
        LocalDateTime toTs = to != null ? to.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(PageResponse.of(analyticsService.listReports(scope, fromTs, toTs, pageable)));
    }
}
