package com.debtpulse.settlement.dto.response;

import com.debtpulse.common.enums.ApprovalDecision;
import com.debtpulse.common.enums.ApprovalLevel;

import java.time.LocalDateTime;

/** A single approver verdict projection within a {@link SettlementResponse}. */
public record ApprovalStepResponse(
        String stepId,
        String approverId,
        ApprovalLevel level,
        ApprovalDecision decision,
        LocalDateTime decidedAt,
        String comments
) {}
