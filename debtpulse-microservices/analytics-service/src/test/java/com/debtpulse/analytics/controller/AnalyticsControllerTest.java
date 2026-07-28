package com.debtpulse.analytics.controller;

import com.debtpulse.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalyticsControllerTest {

    private MockMvc mockMvc;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(AnalyticsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(service)).build();
    }

    @Test
    void dashboard_returns200() throws Exception {
        when(service.dashboard()).thenReturn(Map.of("portfolio", Map.of("totalAccounts", 5)));
        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolio.totalAccounts").value(5));
    }

    @Test
    void recoveryRate_returns200() throws Exception {
        when(service.recoveryRate()).thenReturn(Map.of("recoveryRatePercent", 42.0));
        mockMvc.perform(get("/api/analytics/recovery-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryRatePercent").value(42.0));
    }
}
