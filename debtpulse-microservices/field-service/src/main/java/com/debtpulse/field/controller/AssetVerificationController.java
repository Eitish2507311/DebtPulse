package com.debtpulse.field.controller;

import com.debtpulse.common.dto.PageResponse;
import com.debtpulse.field.dto.request.AssetVerificationRequest;
import com.debtpulse.field.dto.response.AssetVerificationDto;
import com.debtpulse.field.service.AssetVerificationService;
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
@RequestMapping("/api/asset-verifications")
@Tag(name = "Asset Verifications", description = "Field verification of pledged collateral assets")
public class AssetVerificationController {

    private final AssetVerificationService service;

    public AssetVerificationController(AssetVerificationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER')")
    @Operation(summary = "Create an asset-verification report (flags collateral VERIFIED)")
    public ResponseEntity<AssetVerificationDto> create(@Valid @RequestBody AssetVerificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "List asset-verification reports (paginated)")
    public ResponseEntity<PageResponse<AssetVerificationDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("verificationDate").descending());
        return ResponseEntity.ok(PageResponse.of(service.list(pageable)));
    }

    @GetMapping("/visit/{visitId}")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "List verification reports for a given visit")
    public ResponseEntity<List<AssetVerificationDto>> byVisit(@PathVariable String visitId) {
        return ResponseEntity.ok(service.byVisit(visitId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER')")
    @Operation(summary = "Get an asset-verification report by id")
    public ResponseEntity<AssetVerificationDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER')")
    @Operation(summary = "Update an asset-verification report")
    public ResponseEntity<AssetVerificationDto> update(@PathVariable String id,
                                                       @Valid @RequestBody AssetVerificationRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }
}
