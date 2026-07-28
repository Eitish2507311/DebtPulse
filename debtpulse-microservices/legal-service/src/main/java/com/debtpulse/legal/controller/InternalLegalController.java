package com.debtpulse.legal.controller;

import com.debtpulse.legal.service.LegalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal stats API consumed by analytics-service via Feign. Not exposed through the public
 * gateway routes; reachable only service-to-service (with propagated identity).
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal - Legal", description = "Service-to-service legal stats (Feign)")
public class InternalLegalController {

    private final LegalService legalService;

    public InternalLegalController(LegalService legalService) {
        this.legalService = legalService;
    }

    @GetMapping("/legal/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(legalService.stats());
    }
}
