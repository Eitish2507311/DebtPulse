package com.debtpulse.account.dto.request;

import com.debtpulse.common.enums.AssetType;
import com.debtpulse.common.validation.Phone;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Payload to onboard a delinquent account. Bucket is derived server-side from {@code dpd}.
 *
 * <p>A loan may be secured or unsecured. When {@code secured} is true the collateral fields
 * ({@code assetType}, {@code estimatedValue}) are required and the asset is created together with
 * the account in one transaction — a secured loan is never persisted without its collateral.</p>
 */
public record CreateAccountRequest(
        @NotBlank(message = "Loan reference is required")
        String loanRef,

        @NotBlank(message = "Borrower name is required")
        String borrowerName,

        @Phone
        String phone,

        String address,

        String branchId,

        @NotNull(message = "Principal amount is required")
        @Positive(message = "Principal amount must be positive")
        BigDecimal principalAmount,

        @NotNull(message = "Total overdue is required")
        @PositiveOrZero(message = "Total overdue cannot be negative")
        BigDecimal totalOverdue,

        @NotNull(message = "DPD is required")
        @PositiveOrZero(message = "DPD cannot be negative")
        Integer dpd,

        /** Whether the loan is backed by collateral. Defaults to false (unsecured). */
        Boolean secured,

        /** Collateral asset type — required when {@code secured} is true. */
        AssetType assetType,

        String assetDescription,

        /** Collateral estimated value — required when {@code secured} is true. */
        @PositiveOrZero(message = "Estimated value cannot be negative")
        BigDecimal estimatedValue
) {}
