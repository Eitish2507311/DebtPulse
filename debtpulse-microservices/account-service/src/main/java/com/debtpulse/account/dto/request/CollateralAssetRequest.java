package com.debtpulse.account.dto.request;

import com.debtpulse.common.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload to register or update a collateral asset against an account. */
public record CollateralAssetRequest(
        @NotBlank(message = "Account id is required")
        String accountId,

        @NotNull(message = "Asset type is required")
        AssetType assetType,

        String description,

        @NotNull(message = "Estimated value is required")
        @PositiveOrZero(message = "Estimated value cannot be negative")
        BigDecimal estimatedValue,

        /** Date the asset was last verified (defaults to now). Set to the origination appraisal date. */
        LocalDate lastVerifiedDate
) {}
