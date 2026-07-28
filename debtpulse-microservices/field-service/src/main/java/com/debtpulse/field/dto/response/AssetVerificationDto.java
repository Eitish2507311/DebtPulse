package com.debtpulse.field.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read projection of an {@link com.debtpulse.field.entity.AssetVerificationReport}. */
public record AssetVerificationDto(
        String reportId,
        String visitId,
        String assetId,
        String condition,
        String currentLocation,
        BigDecimal realisableValue,
        String remarks,
        String verifiedById,
        LocalDate verificationDate,
        LocalDateTime createdAt
) {}
