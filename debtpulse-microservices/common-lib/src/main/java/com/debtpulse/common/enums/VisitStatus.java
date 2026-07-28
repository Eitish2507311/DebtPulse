package com.debtpulse.common.enums;

/** Lifecycle of a scheduled field visit (2.4 Field Recovery Management). */
public enum VisitStatus {
    SCHEDULED,
    COMPLETED,
    MISSED,
    BORROWER_ABSENT,
    REFUSED
}
