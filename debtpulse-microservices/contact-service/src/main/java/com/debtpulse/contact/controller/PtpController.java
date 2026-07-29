package com.debtpulse.contact.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.common.enums.PtpStatus;
import com.debtpulse.contact.dto.request.PtpRequest;
import com.debtpulse.contact.dto.response.PtpDto;
import com.debtpulse.contact.service.PtpService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/ptp")
@Tag(name = "Promise-to-Pay", description = "PTP commitments and fulfilment (2.3)")
public class PtpController {

    private final PtpService ptpService;

    public PtpController(PtpService ptpService) {
        this.ptpService = ptpService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    @Operation(summary = "Record a promise-to-pay")
    public ResponseEntity<PtpDto> create(@Valid @RequestBody PtpRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ptpService.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    @Operation(summary = "List PTPs (paginated, optional accountId/agentId/status/date-range filters)")
    public ResponseEntity<PageResponse<PtpDto>> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) PtpStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "commitmentDate"));
        return ResponseEntity.ok(PageResponse.of(
                ptpService.list(accountId, agentId, status, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT','PORTFOLIO_MANAGER')")
    public ResponseEntity<PtpDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(ptpService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    @Operation(summary = "Edit an existing promise-to-pay (amount / dates)")
    public ResponseEntity<PtpDto> update(@PathVariable String id, @Valid @RequestBody PtpRequest req) {
        return ResponseEntity.ok(ptpService.update(id, req));
    }

    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    @Operation(summary = "Record a payment against a PTP (KEPT if it covers the amount, else PARTIAL)")
    public ResponseEntity<PtpDto> recordPayment(@PathVariable String id,
                                                @RequestParam BigDecimal actualPaidAmount) {
        return ResponseEntity.ok(ptpService.recordPayment(id, actualPaidAmount));
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','COLLECTIONS_AGENT')")
    @Operation(summary = "Reschedule a PTP to a new commitment date")
    public ResponseEntity<PtpDto> reschedule(@PathVariable String id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                             LocalDate commitmentDate) {
        return ResponseEntity.ok(ptpService.reschedule(id, commitmentDate));
    }
}
