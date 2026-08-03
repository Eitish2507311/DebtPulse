package com.debtpulse.account.controller;

import com.debtpulse.account.dto.response.AccountDto;
import com.debtpulse.account.entity.CollateralAsset;
import com.debtpulse.account.mapper.AccountMapper;
import com.debtpulse.account.service.AccountService;
import com.debtpulse.account.service.CollateralAssetService;
import com.debtpulse.common.enums.AccountStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal account API consumed by other microservices via Feign. Not exposed through the public
 * gateway routes; reachable only service-to-service with propagated identity. Paths and DTO field
 * names match INTERNAL_CONTRACTS.md exactly.
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Internal - Accounts", description = "Service-to-service account lookups (Feign)")
public class InternalAccountController {

    private final AccountService accountService;
    private final CollateralAssetService assetService;
    private final AccountMapper mapper;

    public InternalAccountController(AccountService accountService,
                                     CollateralAssetService assetService,
                                     AccountMapper mapper) {
        this.accountService = accountService;
        this.assetService = assetService;
        this.mapper = mapper;
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String id) {
        return ResponseEntity.ok(mapper.toDto(accountService.getById(id)));
    }

    @GetMapping("/accounts/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable String id) {
        return ResponseEntity.ok(accountService.exists(id));
    }

    /**
     * Service-to-service lifecycle cascade (DP5-18 / DP5-20): settlement-service moves an account to
     * SETTLED once a settlement is paid; legal-service moves it to LEGAL when a case is opened.
     * Callers treat this as best-effort — a failure here must not roll back the caller's own action.
     */
    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String id, @RequestParam AccountStatus status) {
        accountService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/collateral-assets/{assetId}")
    public ResponseEntity<CollateralAsset> getCollateral(@PathVariable String assetId) {
        return ResponseEntity.ok(assetService.get(assetId));
    }

    @PostMapping("/collateral-assets/{assetId}/mark-verified")
    public ResponseEntity<Void> markVerified(@PathVariable String assetId) {
        assetService.markVerified(assetId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(accountService.stats());
    }
}
