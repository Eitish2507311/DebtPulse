package com.debtpulse.settlement.mapper;

import com.debtpulse.settlement.dto.response.ApprovalStepResponse;
import com.debtpulse.settlement.dto.response.SettlementResponse;
import com.debtpulse.settlement.entity.ApprovalStep;
import com.debtpulse.settlement.entity.SettlementProposal;
import com.debtpulse.settlement.service.ApprovalPolicy;
import org.springframework.stereotype.Component;

import java.util.List;

/** Converts settlement entities to their response projections. */
@Component
public class SettlementMapper {

    private final ApprovalPolicy approvalPolicy;

    public SettlementMapper(ApprovalPolicy approvalPolicy) {
        this.approvalPolicy = approvalPolicy;
    }

    public SettlementResponse toDto(SettlementProposal s) {
        if (s == null) return null;
        List<ApprovalStepResponse> steps = s.getApprovalSteps() == null ? List.of()
                : s.getApprovalSteps().stream().map(this::toStepDto).toList();
        return new SettlementResponse(
                s.getProposalId(),
                s.getAccountId(),
                s.getOfficerId(),
                s.getTotalOutstanding(),
                s.getSettlementAmount(),
                s.getHaircutPercent(),
                s.getPaymentDeadline(),
                s.getApprovalLevel(),
                approvalPolicy.requiredLevels(s.getHaircutPercent()),
                s.getCurrentStep(),
                s.getApprovedById(),
                s.getStatus(),
                s.getNotes(),
                steps,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    public ApprovalStepResponse toStepDto(ApprovalStep step) {
        if (step == null) return null;
        return new ApprovalStepResponse(
                step.getStepId(),
                step.getApproverId(),
                step.getLevel(),
                step.getDecision(),
                step.getDecidedAt(),
                step.getComments()
        );
    }
}
