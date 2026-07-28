package com.debtpulse.settlement.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.settlement.dto.request.RestructuringRequest;
import com.debtpulse.settlement.dto.response.RestructuringResponse;
import com.debtpulse.settlement.service.RestructuringService;
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

@RestController
@RequestMapping("/api/restructuring")
@Tag(name = "Restructuring", description = "Loan restructuring proposals")
public class RestructuringController {

    private final RestructuringService restructuringService;

    public RestructuringController(RestructuringService restructuringService) {
        this.restructuringService = restructuringService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER')")
    @Operation(summary = "Create a restructuring proposal")
    public ResponseEntity<RestructuringResponse> create(@Valid @RequestBody RestructuringRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restructuringService.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER','L1_APPROVER','L2_APPROVER','L3_APPROVER','PORTFOLIO_MANAGER')")
    @Operation(summary = "List restructuring proposals (paginated)")
    public ResponseEntity<PageResponse<RestructuringResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageResponse.of(restructuringService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER','L1_APPROVER','L2_APPROVER','L3_APPROVER','PORTFOLIO_MANAGER')")
    public ResponseEntity<RestructuringResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(restructuringService.getById(id));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER','L1_APPROVER','L2_APPROVER','L3_APPROVER','PORTFOLIO_MANAGER')")
    public ResponseEntity<List<RestructuringResponse>> byAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(restructuringService.byAccount(accountId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER')")
    @Operation(summary = "Update a DRAFT restructuring proposal")
    public ResponseEntity<RestructuringResponse> update(@PathVariable String id,
                                                        @Valid @RequestBody RestructuringRequest req) {
        return ResponseEntity.ok(restructuringService.update(id, req));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','L1_APPROVER','L2_APPROVER','L3_APPROVER')")
    @Operation(summary = "Approve a restructuring proposal")
    public ResponseEntity<RestructuringResponse> approve(@PathVariable String id) {
        return ResponseEntity.ok(restructuringService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','L1_APPROVER','L2_APPROVER','L3_APPROVER')")
    @Operation(summary = "Reject a restructuring proposal (returned to DRAFT)")
    public ResponseEntity<RestructuringResponse> reject(@PathVariable String id) {
        return ResponseEntity.ok(restructuringService.reject(id));
    }
}
