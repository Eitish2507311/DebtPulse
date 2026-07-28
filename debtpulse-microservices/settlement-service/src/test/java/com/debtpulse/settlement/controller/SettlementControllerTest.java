package com.debtpulse.settlement.controller;

import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.common.enums.SettlementStatus;
import com.debtpulse.settlement.dto.response.SettlementResponse;
import com.debtpulse.settlement.service.SettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
class SettlementControllerTest {

    private MockMvc mockMvc;
    private SettlementService settlementService;
    private final ObjectMapper om = new ObjectMapper();

    private SettlementResponse sample() {
        return new SettlementResponse(
                "S-100", "ACC-1", "USR-OFF",
                new BigDecimal("100000"), new BigDecimal("60000"), new BigDecimal("40.00"),
                LocalDate.now().plusDays(30),
                ApprovalLevel.L3, List.of(ApprovalLevel.L1, ApprovalLevel.L2, ApprovalLevel.L3), ApprovalLevel.L1,
                null, SettlementStatus.DRAFT, "please approve", List.of(), null, null);
    }

    @BeforeEach
    void setUp() {
        settlementService = Mockito.mock(SettlementService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SettlementController(settlementService)).build();
    }

    @Test
    void getById_returns200() throws Exception {
        when(settlementService.getById(eq("S-100"))).thenReturn(sample());

        mockMvc.perform(get("/api/settlements/S-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalId").value("S-100"))
                .andExpect(jsonPath("$.haircutPercent").value(40.00));
    }

    @Test
    void create_returns201() throws Exception {
        when(settlementService.create(any())).thenReturn(sample());

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", "ACC-1");
        body.put("totalOutstanding", 100000);
        body.put("settlementAmount", 60000);
        body.put("paymentDeadline", LocalDate.now().plusDays(30).toString());
        body.put("notes", "please approve");

        mockMvc.perform(post("/api/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalId").value("S-100"))
                .andExpect(jsonPath("$.requiredApprovalChain[0]").value("L1"))
                .andExpect(jsonPath("$.currentStep").value("L1"));
    }

    @Test
    void create_missingAccountId_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("totalOutstanding", 100000);
        body.put("settlementAmount", 60000);
        body.put("paymentDeadline", LocalDate.now().plusDays(30).toString());
        body.put("approvalLevel", "L1");

        mockMvc.perform(post("/api/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
