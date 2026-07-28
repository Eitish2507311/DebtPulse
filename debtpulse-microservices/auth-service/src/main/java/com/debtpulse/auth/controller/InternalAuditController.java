package com.debtpulse.auth.controller;

import com.debtpulse.auth.dto.request.AuditLogRequest;
import com.debtpulse.auth.entity.AuditLog;
import com.debtpulse.auth.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal audit-recording API. Every other microservice POSTs its business actions here
 * over Feign so the audit trail is centralised in one place.
 */
@RestController
@RequestMapping("/api/internal/audit-logs")
@Tag(name = "Internal - Audit", description = "Service-to-service audit recording (Feign)")
public class InternalAuditController {

    private final AuditService auditService;

    public InternalAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<AuditLog> record(@Valid @RequestBody AuditLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditService.record(request));
    }
}
