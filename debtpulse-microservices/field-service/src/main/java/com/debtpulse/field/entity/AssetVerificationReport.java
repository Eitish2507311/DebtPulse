package com.debtpulse.field.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.AssetCondition;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The outcome of physically verifying a pledged collateral asset during a field visit
 * (2.4 Field Recovery Management).
 *
 * <p>Owned by field-service. {@code visitId} references a {@link FieldVisit} in this service;
 * {@code assetId} references a CollateralAsset owned by account-service (plain id string);
 * {@code verifiedById} is an auth-service user reference. On creation this service asks
 * account-service to flag the collateral as VERIFIED.</p>
 */
@Entity
@Table(name = "asset_verification_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AssetVerificationReport {

    @Id
    @BusinessId(prefix = "AVR")
    private String reportId;

    /** The field visit during which the asset was verified. */
    private String visitId;

    /** The collateral asset being verified (account-service reference). */
    private String assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_condition")
    private AssetCondition condition;

    private String currentLocation;

    @Column(precision = 15, scale = 2)
    private BigDecimal realisableValue;

    @Column(length = 1000)
    private String remarks;

    /** The field officer who performed the verification (auth-service user reference). */
    private String verifiedById;

    private LocalDate verificationDate;

    @CreatedDate
    private LocalDateTime createdAt;
}
