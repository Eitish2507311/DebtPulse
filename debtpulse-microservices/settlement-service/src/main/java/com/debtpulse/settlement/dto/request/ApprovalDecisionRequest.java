package com.debtpulse.settlement.dto.request;

import com.debtpulse.common.enums.ApprovalDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An approver's verdict on a settlement (maker-checker checker step).
 *
 * <p>The tier the approver is acting as is supplied as the {@code level} request parameter on the
 * endpoint (an enum, so Swagger renders it as a dropdown) — not in this body.</p>
 */
public record ApprovalDecisionRequest(
        @NotNull(message = "Decision is required")
        ApprovalDecision decision,

        @Size(max = 1000, message = "Comments must be at most 1000 characters")
        String comments
) {}
