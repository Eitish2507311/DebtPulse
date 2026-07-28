package com.debtpulse.settlement.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.ApprovalDecision;
import com.debtpulse.common.enums.ApprovalLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single approver verdict within a settlement's maker-checker workflow (2.5).
 * Owned by this service and tied to its {@link SettlementProposal} via {@code @ManyToOne}.
 */
@Entity
@Table(name = "approval_step")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStep {

    @Id
    @BusinessId(prefix = "APS")
    private String stepId;

    /** Back-reference to the owning proposal; {@code @JsonIgnore} avoids infinite recursion. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private SettlementProposal settlement;

    /** auth-service user id of the approver. */
    private String approverId;

    @Enumerated(EnumType.STRING)
    private ApprovalLevel level;

    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;

    private LocalDateTime decidedAt;

    @Column(length = 1000)
    private String comments;
}
