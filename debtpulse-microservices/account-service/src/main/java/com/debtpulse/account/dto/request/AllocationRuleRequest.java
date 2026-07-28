package com.debtpulse.account.dto.request;

import com.debtpulse.common.enums.AllocationStrategy;
import com.debtpulse.common.enums.DpdBucket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Payload to create or update an allocation/escalation rule. */
public record AllocationRuleRequest(
        @NotBlank(message = "Rule name is required")
        String name,

        @NotNull(message = "Strategy is required")
        AllocationStrategy strategy,

        DpdBucket bucket,

        @NotBlank(message = "Target role is required")
        String targetRole,

        @PositiveOrZero(message = "Days-in-bucket threshold cannot be negative")
        Integer daysInBucketThreshold,

        @PositiveOrZero(message = "Minimum DPD cannot be negative")
        Integer minDpd,

        @PositiveOrZero(message = "Grace period cannot be negative")
        Integer gracePeriodDays,

        @PositiveOrZero(message = "Capacity limit cannot be negative")
        Integer capacityLimit,

        String branchId,

        Integer priority,

        /** {@code true} = escalation rule (nightly, moves stagnating accounts up a role);
         *  {@code false}/null = initial-allocation rule (assigns to a collection agent). */
        Boolean autoEscalate,

        Boolean active
) {}
