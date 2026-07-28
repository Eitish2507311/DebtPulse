package com.debtpulse.settlement.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.RestructuringStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A loan restructuring plan (revised tenure/EMI with optional waiver) for an account (2.5).
 * Cross-service references are plain id strings.
 */
@Entity
@Table(name = "restructuring_proposal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RestructuringProposal {

    @Id
    @BusinessId(prefix = "RST")
    private String restructureId;

    private String accountId;

    private String officerId;

    private Integer revisedTenure;

    @Column(precision = 15, scale = 2)
    private BigDecimal revisedEmi;

    @Column(precision = 15, scale = 2)
    private BigDecimal waiverAmount;

    private LocalDate startDate;

    private String approvedById;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RestructuringStatus status = RestructuringStatus.DRAFT;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
