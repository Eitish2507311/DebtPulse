package com.debtpulse.settlement.dto.response;

import com.debtpulse.common.enums.ApprovalLevel;
import com.debtpulse.common.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Full settlement projection including the derived approval chain and its step trail. */
public record SettlementResponse(
        String proposalId,
        String accountId,
        String officerId,
        BigDecimal totalOutstanding,
        BigDecimal settlementAmount,
        BigDecimal haircutPercent,
        LocalDate paymentDeadline,
        /** Highest level required (derived from haircut). */
        ApprovalLevel approvalLevel,
        /** Full ordered chain that must approve, e.g. [L1, L2, L3] — derived from the haircut. */
        List<ApprovalLevel> requiredApprovalChain,
        /** Level currently awaiting a decision (null in DRAFT / terminal states). */
        ApprovalLevel currentStep,
        String approvedById,
        SettlementStatus status,
        String notes,
        List<ApprovalStepResponse> approvalSteps,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
