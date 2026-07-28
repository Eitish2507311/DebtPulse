package com.debtpulse.settlement.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A one-time settlement offer against a delinquent account (2.5 Settlement &amp; Restructuring).
 *
 * <p>Cross-service references ({@code accountId}, {@code officerId}, {@code approvedById})
 * are stored as plain id strings — the referenced entities are owned by other services.
 * The {@link ApprovalStep} audit trail lives within this service, so a real JPA
 * {@code @OneToMany}/{@code @ManyToOne} relationship is used there.</p>
 */
@Entity
@Table(name = "settlement_proposal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SettlementProposal {

    @Id
    @BusinessId(prefix = "SET")
    private String proposalId;

    /** account-service account id (plain reference). */
    private String accountId;

    /** auth-service user id of the settlement officer who raised the proposal. */
    private String officerId;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalOutstanding;

    @Column(precision = 15, scale = 2)
    private BigDecimal settlementAmount;

    @Column(precision = 6, scale = 2)
    private BigDecimal haircutPercent;

    private LocalDate paymentDeadline;

    /** Highest approval level required, derived from the haircut (the last link of the chain). */
    @Enumerated(EnumType.STRING)
    private ApprovalLevel approvalLevel;

    /** The level currently awaiting a decision while PENDING_APPROVAL (null in DRAFT / terminal states). */
    @Enumerated(EnumType.STRING)
    private ApprovalLevel currentStep;

    /** Optional officer remarks captured on creation. */
    @Column(length = 1000)
    private String notes;

    /** auth-service user id of the approver who gave the FINAL approval (nullable until fully approved). */
    private String approvedById;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.DRAFT;

    @Builder.Default
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalStep> approvalSteps = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Adds a step and wires the back-reference so the cascade persists it. */
    public void addApprovalStep(ApprovalStep step) {
        step.setSettlement(this);
        this.approvalSteps.add(step);
    }
}
