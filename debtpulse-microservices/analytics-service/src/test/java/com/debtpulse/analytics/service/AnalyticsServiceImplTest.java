package com.debtpulse.analytics.service;

import com.debtpulse.analytics.entity.RecoveryReport;
import com.debtpulse.analytics.feign.StatsClients.*;
import com.debtpulse.analytics.repository.RecoveryReportRepository;
import com.debtpulse.analytics.service.impl.AnalyticsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private AccountStatsClient accountClient;
    @Mock private ContactStatsClient contactClient;
    @Mock private SettlementStatsClient settlementClient;
    @Mock private LegalStatsClient legalClient;
    @Mock private FieldStatsClient fieldClient;
    @Mock private RecoveryReportRepository reportRepo;

    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(accountClient, contactClient, settlementClient,
                legalClient, fieldClient, reportRepo, new ObjectMapper());
    }

    @Test
    void dashboard_aggregatesAllSources() {
        when(accountClient.accountStats()).thenReturn(Map.of("totalAccounts", 10, "settledAccounts", 2, "totalOverdue", 5000));
        when(contactClient.ptpStats()).thenReturn(Map.of("totalPtp", 4));
        when(contactClient.contactStats()).thenReturn(Map.of("totalContacts", 20));
        when(settlementClient.settlementStats()).thenReturn(Map.of("paidSettlements", 1));
        when(legalClient.legalStats()).thenReturn(Map.of("totalCases", 3));
        when(fieldClient.visitStats()).thenReturn(Map.of("totalVisits", 6));

        Map<String, Object> dash = service.dashboard();

        assertThat(dash).containsKeys("portfolio", "ptp", "contacts", "settlements", "legal", "fieldVisits", "recoveryRate");
    }

    @Test
    void recoveryRate_computesPercent() {
        when(accountClient.accountStats()).thenReturn(Map.of("totalAccounts", 10, "settledAccounts", 3, "totalOverdue", 1000));
        when(settlementClient.settlementStats()).thenReturn(Map.of("paidSettlements", 2));

        Map<String, Object> rr = service.recoveryRate();

        assertThat(rr.get("recoveryRatePercent")).isEqualTo(30.0);
    }

    @Test
    void generateReport_persists() {
        when(accountClient.accountStats()).thenReturn(Map.of("totalAccounts", 1));
        when(contactClient.ptpStats()).thenReturn(Map.of());
        when(contactClient.contactStats()).thenReturn(Map.of());
        when(settlementClient.settlementStats()).thenReturn(Map.of());
        when(legalClient.legalStats()).thenReturn(Map.of());
        when(fieldClient.visitStats()).thenReturn(Map.of());
        when(reportRepo.save(any(RecoveryReport.class))).thenAnswer(inv -> inv.getArgument(0));

        RecoveryReport report = service.generateReport("Branch");

        assertThat(report.getScope()).isEqualTo("Branch");
        assertThat(report.getMetrics()).contains("portfolio");
        verify(reportRepo).save(any(RecoveryReport.class));
    }
}
