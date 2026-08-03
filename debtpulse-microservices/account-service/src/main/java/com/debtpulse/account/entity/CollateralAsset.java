package com.debtpulse.account.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.AssetType;
import com.debtpulse.common.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A collateral asset pledged against a delinquent account (2.2). {@code accountId} is a plain
 * string reference to the owning {@link DelinquentAccount} within this same service.
 */
@Entity
@Table(name = "collateral_asset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollateralAsset {

    @Id
    @BusinessId(prefix = "COL")
    private String assetId;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    private String description;

    private BigDecimal estimatedValue;

    // Verified at loan origination (the asset was appraised when the secured loan was granted).
    // A field officer re-verifies it on a physical visit, which refreshes lastVerifiedDate.
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.VERIFIED;

    private LocalDateTime lastVerifiedDate;
}
