package com.debtpulse.account.dto.request;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.validation.Phone;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Partial-update payload — every field is optional (null = leave unchanged).
 *
 * <p>{@code bucket} is intentionally NOT editable here: it is a <em>derived</em> field
 * (a supplied {@code dpd} reclassifies it) so it can't drift out of sync with the DPD.
 * {@code daysInCurrentBucket} is directly editable (it was previously missing).
 * {@code assignedAgentId} may be set here for a manual admin re-assignment (blank clears it);
 * a dedicated PATCH endpoint also exists for auditable single-purpose changes.</p>
 */
public record UpdateAccountRequest(
        String borrowerName,

        @Phone
        String phone,
        String address,
        String branchId,

        @PositiveOrZero(message = "Principal amount cannot be negative")
        BigDecimal principalAmount,

        @PositiveOrZero(message = "Total overdue cannot be negative")
        BigDecimal totalOverdue,

        @PositiveOrZero(message = "DPD cannot be negative")
        Integer dpd,

        @PositiveOrZero(message = "Days in current bucket cannot be negative")
        Integer daysInCurrentBucket,

        AccountStatus status,

        /** Manual (admin) re-assignment of the owning agent; blank clears the assignment. */
        String assignedAgentId
) {}
