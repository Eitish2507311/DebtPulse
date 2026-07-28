package com.debtpulse.field.controller;

import com.debtpulse.field.dto.response.AssetVerificationDto;
import com.debtpulse.field.service.AssetVerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class AssetVerificationControllerTest {

    private MockMvc mockMvc;
    private AssetVerificationService service;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = Mockito.mock(AssetVerificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AssetVerificationController(service)).build();
    }

    @Test
    void create_returns201WithReport() throws Exception {
        when(service.create(any())).thenReturn(new AssetVerificationDto(
                "RPT-1", "VIS-1", "AST-1", "GOOD", "Warehouse 3",
                new BigDecimal("150000.00"), "Intact", "USR-9", LocalDate.now(), null));

        mockMvc.perform(post("/api/asset-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "visitId", "VIS-1",
                                "assetId", "AST-1",
                                "condition", "GOOD",
                                "realisableValue", 150000.00))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value("RPT-1"))
                .andExpect(jsonPath("$.condition").value("GOOD"));
    }

    @Test
    void create_missingAssetId_returns400() throws Exception {
        mockMvc.perform(post("/api/asset-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "visitId", "VIS-1",
                                "condition", "GOOD"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200() throws Exception {
        when(service.getById(eq("RPT-1"))).thenReturn(new AssetVerificationDto(
                "RPT-1", "VIS-1", "AST-1", "FAIR", "Site A",
                new BigDecimal("90000.00"), "Minor wear", "USR-9", LocalDate.now(), null));

        mockMvc.perform(get("/api/asset-verifications/RPT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("RPT-1"))
                .andExpect(jsonPath("$.assetId").value("AST-1"));
    }
}
