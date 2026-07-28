package com.debtpulse.settlement.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.settlement.dto.request.ApprovalDecisionRequest;
import com.debtpulse.settlement.dto.request.SettlementRequest;
import com.debtpulse.settlement.dto.response.SettlementResponse;
import com.debtpulse.settlement.service.SettlementService;
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
@RequestMapping("/api/settlements")
@PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER','PORTFOLIO_MANAGER','L1_APPROVER','L2_APPROVER','L3_APPROVER')")
@Tag(name = "Settlements", description = "Settlement proposals with maker-checker approvals")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SETTLEMENT_OFFICER')")
    @Operation(summary = "Create a settlement proposal (haircut computed server-side)")
    public ResponseEntity<SettlementResponse> create(@Valid @RequestBody SettlementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.create(req));
    }

    @PatchMapping("/{id}/submit")
    @Operation(summary = "Submit a DRAFT settlement for approval")
    public ResponseEntity<SettlementResponse> submit(@PathVariable String id) {
        return ResponseEntity.ok(settlementService.submit(id));
    }

    @GetMapping
    @Operation(summary = "List settlements (paginated)")
    public ResponseEntity<PageResponse<SettlementResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageResponse.of(settlementService.list(pageable)));
    }

    @GetMapping("/outstanding")
    @Operation(summary = "Settlements not yet PAID/REJECTED/EXPIRED")
    public ResponseEntity<List<SettlementResponse>> outstanding() {
        return ResponseEntity.ok(settlementService.outstanding());
    }

    @GetMapping("/past-deadline")
    @Operation(summary = "APPROVED settlements whose payment deadline has passed")
    public ResponseEntity<List<SettlementResponse>> pastDeadline() {
        return ResponseEntity.ok(settlementService.pastDeadline());
    }

    @GetMapping("/approval-queue")
    @Operation(summary = "Settlements awaiting approval")
    public ResponseEntity<List<SettlementResponse>> approvalQueue() {
        return ResponseEntity.ok(settlementService.approvalQueue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(settlementService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT settlement")
    public ResponseEntity<SettlementResponse> update(@PathVariable String id,
                                                     @Valid @RequestBody SettlementRequest req) {
        return ResponseEntity.ok(settlementService.update(id, req));
    }

    @PostMapping("/{id}/decide")
    @Operation(summary = "Approve/reject a settlement. Select the level (L1/L2/L3) you are acting as — it "
            + "must match the step currently pending (else 400); maker-checker: approver != raising officer.")
    public ResponseEntity<SettlementResponse> decide(@PathVariable String id,
                                                     @RequestParam ApprovalLevel level,
                                                     @Valid @RequestBody ApprovalDecisionRequest req) {
        return ResponseEntity.ok(settlementService.decide(id, level, req));
    }

    @PatchMapping("/{id}/mark-paid")
    @Operation(summary = "Mark an APPROVED settlement as PAID")
    public ResponseEntity<SettlementResponse> markPaid(@PathVariable String id) {
        return ResponseEntity.ok(settlementService.markPaid(id));
    }
}
