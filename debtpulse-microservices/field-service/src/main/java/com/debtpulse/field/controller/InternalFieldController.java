package com.debtpulse.field.controller;

import com.debtpulse.field.service.FieldVisitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal field-metrics API consumed by other microservices (e.g. analytics-service) via Feign.
 * Not exposed through the public gateway routes; reachable only service-to-service.
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal - Field", description = "Service-to-service field metrics (Feign)")
public class InternalFieldController {

    private final FieldVisitService fieldVisitService;

    public InternalFieldController(FieldVisitService fieldVisitService) {
        this.fieldVisitService = fieldVisitService;
    }

    @GetMapping("/visits/stats")
    public ResponseEntity<Map<String, Object>> visitStats() {
        return ResponseEntity.ok(fieldVisitService.stats());
    }
}
