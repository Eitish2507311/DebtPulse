package com.debtpulse.legal.controller;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.CaseType;
import com.debtpulse.legal.dto.response.LegalCaseDto;
import com.debtpulse.legal.service.LegalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer test using standalone MockMvc (no security context) so the mapping,
 * validation and JSON contract are exercised in isolation with a mocked service.
 */
class LegalControllerTest {

    private MockMvc mockMvc;
    private LegalService legalService;
    private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        legalService = Mockito.mock(LegalService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalController(legalService)).build();
    }

    private LegalCaseDto sampleCase() {
        return new LegalCaseDto("CASE-1", "ACC-001", "USR-003", CaseType.CIVIL_SUIT,
                LocalDate.of(2026, 7, 1), "City Civil Court", "CS/2026/123",
                CaseStatus.FILED, LocalDateTime.now());
    }

    @Test
    void initiateCase_returns201WithBody() throws Exception {
        when(legalService.initiateCase(any())).thenReturn(sampleCase());

        mockMvc.perform(post("/api/legal/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "accountId", "ACC-001",
                                "caseType", "CIVIL_SUIT",
                                "filingDate", "2026-07-01",
                                "courtName", "City Civil Court",
                                "caseNumber", "CS/2026/123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value("CASE-1"))
                .andExpect(jsonPath("$.status").value("FILED"));
    }

    @Test
    void initiateCase_missingAccountId_returns400() throws Exception {
        mockMvc.perform(post("/api/legal/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "caseType", "CIVIL_SUIT",
                                "filingDate", "2026-07-01",
                                "courtName", "City Civil Court",
                                "caseNumber", "CS/2026/123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCase_returns200() throws Exception {
        when(legalService.getCase(eq("CASE-1"))).thenReturn(sampleCase());

        mockMvc.perform(get("/api/legal/cases/CASE-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andExpect(jsonPath("$.caseNumber").value("CS/2026/123"));
    }
}
