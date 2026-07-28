package com.debtpulse.auth.controller;

import com.debtpulse.auth.entity.AuditLog;
import com.debtpulse.auth.service.AuditService;
import com.debtpulse.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'PORTFOLIO_MANAGER')")
@Tag(name = "Audit Logs", description = "Read-only access to the central audit trail")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AuditLog>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(PageResponse.of(
                auditService.getAll(userId, entityType, action, from, to, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getById(@PathVariable String id) {
        return ResponseEntity.ok(auditService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<AuditLog>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(PageResponse.of(auditService.getByUser(userId, pageable)));
    }

    @GetMapping("/entity/{entityType}/{recordId}")
    public ResponseEntity<List<AuditLog>> getByEntity(@PathVariable String entityType,
                                                      @PathVariable String recordId) {
        return ResponseEntity.ok(auditService.getByEntity(entityType, recordId));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export the most recent audit entries as CSV")
    public ResponseEntity<String> exportCsv() {
        Pageable pageable = PageRequest.of(0, 5000, Sort.by("timestamp").descending());
        StringBuilder sb = new StringBuilder("auditId,userId,action,entityType,recordId,sourceService,timestamp\n");
        for (AuditLog a : auditService.getAll(null, null, null, null, null, pageable).getContent()) {
            sb.append(csv(a.getAuditId())).append(',')
              .append(csv(a.getUserId())).append(',')
              .append(csv(a.getAction())).append(',')
              .append(csv(a.getEntityType())).append(',')
              .append(csv(a.getRecordId())).append(',')
              .append(csv(a.getSourceService())).append(',')
              .append(a.getTimestamp()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString());
    }

    private String csv(String v) {
        if (v == null) return "";
        return v.contains(",") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
