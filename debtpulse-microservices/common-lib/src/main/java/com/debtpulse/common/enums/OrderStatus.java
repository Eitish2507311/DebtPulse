package com.debtpulse.common.enums;

/** Execution state of a recovery order (2.6). */
public enum OrderStatus {
    ISSUED,
    IN_EXECUTION,
    EXECUTED,
    CHALLENGED,
    VACATED
}
