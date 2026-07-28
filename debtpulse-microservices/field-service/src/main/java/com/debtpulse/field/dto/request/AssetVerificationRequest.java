package com.debtpulse.field.dto.request;

import com.debtpulse.common.enums.AssetCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload to create or update an asset-verification report. */
public record AssetVerificationRequest(

        @NotBlank(message = "Visit id is required")
        String visitId,

        @NotBlank(message = "Asset id is required")
        String assetId,

        @NotNull(message = "Asset condition is required")
        AssetCondition condition,

        String currentLocation,

        @PositiveOrZero(message = "Realisable value cannot be negative")
        BigDecimal realisableValue,

        String remarks,

        String verifiedById,

        LocalDate verificationDate
) {}
