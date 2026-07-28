package com.debtpulse.settlement.controller;

import com.debtpulse.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal stats API consumed by analytics-service over Feign
 * (see INTERNAL_CONTRACTS: {@code GET /api/internal/settlements/stats}).
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal - Settlements", description = "Service-to-service settlement stats (Feign)")
public class InternalSettlementController {

    private final SettlementService settlementService;

    public InternalSettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/settlements/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(settlementService.stats());
    }
}
