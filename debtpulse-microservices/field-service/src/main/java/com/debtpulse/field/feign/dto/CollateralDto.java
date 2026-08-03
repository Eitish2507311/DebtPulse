package com.debtpulse.field.feign.dto;

import java.math.BigDecimal;

/**
 * Minimal read view of a collateral asset from account-service
 * ({@code GET /api/internal/collateral-assets/{assetId}}) — only the fields field-service needs to
 * validate an asset-verification report (a realisable value can't exceed the appraised estimate).
 */
public record CollateralDto(
        String assetId,
        BigDecimal estimatedValue
) {}
