package com.debtpulse.field.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.field.dto.request.CompleteVisitRequest;
import com.debtpulse.field.dto.request.ScheduleVisitRequest;
import com.debtpulse.field.dto.response.FieldVisitDto;
import com.debtpulse.field.service.FieldVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Field Visits", description = "Schedule, track and complete on-site borrower visits")
public class FieldVisitController {

    private final FieldVisitService service;

    public FieldVisitController(FieldVisitService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "Schedule a field visit")
    public ResponseEntity<FieldVisitDto> schedule(@Valid @RequestBody ScheduleVisitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.schedule(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "List field visits (paginated, optional accountId/officerId/status/date-range filters)")
    public ResponseEntity<PageResponse<FieldVisitDto>> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String officerId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledDate").descending());
        return ResponseEntity.ok(PageResponse.of(
                service.list(accountId, officerId, status, from, to, pageable)));
    }

    @GetMapping("/my-visits")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "List the current officer's own field visits")
    public ResponseEntity<List<FieldVisitDto>> myVisits() {
        return ResponseEntity.ok(service.myVisits(AuthContext.currentUserId()));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "Complete a field visit and record its outcome")
    public ResponseEntity<FieldVisitDto> complete(@PathVariable String id,
                                                  @Valid @RequestBody CompleteVisitRequest req) {
        return ResponseEntity.ok(service.complete(id, req));
    }

    @PatchMapping("/{id}/missed")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "Mark a field visit as MISSED")
    public ResponseEntity<FieldVisitDto> missed(@PathVariable String id) {
        return ResponseEntity.ok(service.markMissed(id));
    }
}
