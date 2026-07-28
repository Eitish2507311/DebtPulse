package com.debtpulse.common.enums;

/** Strategy used by an allocation rule to distribute accounts to agents. */
public enum AllocationStrategy {
    ROUND_ROBIN,
    BRANCH_BASED,
    LEAST_LOADED
}
