package com.debtpulse.legal.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.legal.dto.request.CourtHearingRequest;
import com.debtpulse.legal.dto.request.LegalCaseRequest;
import com.debtpulse.legal.dto.request.RecoveryOrderRequest;
import com.debtpulse.legal.dto.response.CourtHearingDto;
import com.debtpulse.legal.dto.response.LegalCaseDto;
import com.debtpulse.legal.dto.response.RecoveryOrderDto;
import com.debtpulse.common.enums.OrderStatus;
import com.debtpulse.legal.service.LegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public legal-proceedings API (2.6). Class-level RBAC restricts the whole surface to legal
 * staff and managers; filing cases is further limited to legal officers and admins.
 */
@RestController
@RequestMapping("/api/legal")
@PreAuthorize("hasAnyRole('ADMIN','LEGAL_OFFICER','PORTFOLIO_MANAGER')")
@Tag(name = "Legal", description = "Legal cases, court hearings and recovery orders")
public class LegalController {

    private final LegalService legalService;

    public LegalController(LegalService legalService) {
        this.legalService = legalService;
    }

    // ---- cases ----

    @PostMapping("/cases")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL_OFFICER')")
    @Operation(summary = "File a new legal case")
    public ResponseEntity<LegalCaseDto> initiateCase(@Valid @RequestBody LegalCaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legalService.initiateCase(req));
    }

    @GetMapping("/cases")
    @Operation(summary = "List legal cases (paginated)")
    public ResponseEntity<PageResponse<LegalCaseDto>> listCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("filingDate").descending());
        return ResponseEntity.ok(PageResponse.of(legalService.listCases(pageable)));
    }

    @GetMapping("/cases/{id}")
    @Operation(summary = "Get a legal case by id")
    public ResponseEntity<LegalCaseDto> getCase(@PathVariable String id) {
        return ResponseEntity.ok(legalService.getCase(id));
    }

    @PutMapping("/cases/{id}")
    @Operation(summary = "Update a legal case (status, court details, etc.)")
    public ResponseEntity<LegalCaseDto> updateCase(@PathVariable String id,
                                                   @Valid @RequestBody LegalCaseRequest req) {
        return ResponseEntity.ok(legalService.updateCase(id, req));
    }

    // ---- hearings ----

    @PostMapping("/hearings")
    @Operation(summary = "Record a court hearing for a case")
    public ResponseEntity<CourtHearingDto> addHearing(@Valid @RequestBody CourtHearingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legalService.addHearing(req));
    }

    @GetMapping("/cases/{caseId}/hearings")
    @Operation(summary = "List hearings for a case")
    public ResponseEntity<List<CourtHearingDto>> listHearings(@PathVariable String caseId) {
        return ResponseEntity.ok(legalService.listHearings(caseId));
    }

    @GetMapping("/hearings")
    @Operation(summary = "List all court hearings across every case")
    public ResponseEntity<List<CourtHearingDto>> listAllHearings() {
        return ResponseEntity.ok(legalService.listAllHearings());
    }

    // ---- orders ----

    @PostMapping("/orders")
    @Operation(summary = "Issue a recovery order for a case")
    public ResponseEntity<RecoveryOrderDto> issueOrder(@Valid @RequestBody RecoveryOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legalService.issueOrder(req));
    }

    @GetMapping("/orders")
    @Operation(summary = "List recovery orders")
    public ResponseEntity<List<RecoveryOrderDto>> listOrders() {
        return ResponseEntity.ok(legalService.listOrders());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get a recovery order by id")
    public ResponseEntity<RecoveryOrderDto> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(legalService.getOrder(id));
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Advance a recovery order's lifecycle status")
    public ResponseEntity<RecoveryOrderDto> updateOrderStatus(@PathVariable String id,
                                                              @RequestParam OrderStatus status) {
        return ResponseEntity.ok(legalService.updateOrderStatus(id, status));
    }

    @DeleteMapping("/orders/{id}")
    @Operation(summary = "Delete a recovery order")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        legalService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
