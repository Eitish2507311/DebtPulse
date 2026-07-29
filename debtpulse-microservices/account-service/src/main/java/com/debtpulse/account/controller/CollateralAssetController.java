package com.debtpulse.account.controller;

import com.debtpulse.common.enums.AssetType;
import com.debtpulse.common.enums.VerificationStatus;
import com.debtpulse.account.dto.request.CollateralAssetRequest;
import com.debtpulse.account.entity.CollateralAsset;
import com.debtpulse.account.service.CollateralAssetService;
import com.debtpulse.common.dto.PageResponse;
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
@RequestMapping("/api/collateral-assets")
@Tag(name = "Collateral Assets", description = "Collateral pledged against delinquent accounts")
public class CollateralAssetController {

    private final CollateralAssetService assetService;

    public CollateralAssetController(CollateralAssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','COLLECTIONS_AGENT')")
    @Operation(summary = "Register a collateral asset against an account")
    public ResponseEntity<CollateralAsset> create(@Valid @RequestBody CollateralAssetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER','COLLECTIONS_AGENT')")
    @Operation(summary = "List all collateral assets (paginated, filterable by account/type/verification)")
    public ResponseEntity<PageResponse<CollateralAsset>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) VerificationStatus verificationStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "assetId"));
        return ResponseEntity.ok(PageResponse.of(
                assetService.list(accountId, assetType, verificationStatus, pageable)));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER','COLLECTIONS_AGENT')")
    @Operation(summary = "List collateral assets for an account")
    public ResponseEntity<List<CollateralAsset>> getByAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(assetService.getByAccount(accountId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','PORTFOLIO_MANAGER','COLLECTIONS_AGENT')")
    public ResponseEntity<CollateralAsset> getById(@PathVariable String id) {
        return ResponseEntity.ok(assetService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FIELD_OFFICER','COLLECTIONS_AGENT')")
    public ResponseEntity<CollateralAsset> update(@PathVariable String id,
                                                  @Valid @RequestBody CollateralAssetRequest req) {
        return ResponseEntity.ok(assetService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a collateral asset")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
