package com.debtpulse.legal.service;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.HearingOutcome;
import com.debtpulse.common.enums.OrderStatus;
import com.debtpulse.legal.exception.BusinessRuleException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Legal lifecycle state machine. A case/order may only move along a legally-meaningful path — e.g. a
 * case cannot jump straight from {@code FILED} to {@code SETTLED}; it must pass through
 * {@code HEARING_SCHEDULED} (recorded via a court hearing) first. Terminal states accept no further
 * change. Re-setting the same status is always allowed (a no-op edit that touches other fields).
 */
public final class LegalStatusPolicy {

    private static final Map<CaseStatus, Set<CaseStatus>> CASE = new EnumMap<>(CaseStatus.class);
    private static final Map<OrderStatus, Set<OrderStatus>> ORDER = new EnumMap<>(OrderStatus.class);

    static {
        CASE.put(CaseStatus.FILED, EnumSet.of(CaseStatus.PENDING, CaseStatus.HEARING_SCHEDULED, CaseStatus.WITHDRAWN));
        CASE.put(CaseStatus.PENDING, EnumSet.of(CaseStatus.HEARING_SCHEDULED, CaseStatus.WITHDRAWN));
        CASE.put(CaseStatus.HEARING_SCHEDULED,
                EnumSet.of(CaseStatus.PENDING, CaseStatus.DECREED, CaseStatus.SETTLED, CaseStatus.WITHDRAWN));
        CASE.put(CaseStatus.DECREED, EnumSet.of(CaseStatus.SETTLED));
        CASE.put(CaseStatus.WITHDRAWN, EnumSet.noneOf(CaseStatus.class)); // terminal
        CASE.put(CaseStatus.SETTLED, EnumSet.noneOf(CaseStatus.class));   // terminal

        ORDER.put(OrderStatus.ISSUED, EnumSet.of(OrderStatus.IN_EXECUTION, OrderStatus.CHALLENGED, OrderStatus.VACATED));
        ORDER.put(OrderStatus.IN_EXECUTION, EnumSet.of(OrderStatus.EXECUTED, OrderStatus.CHALLENGED, OrderStatus.VACATED));
        ORDER.put(OrderStatus.CHALLENGED, EnumSet.of(OrderStatus.IN_EXECUTION, OrderStatus.VACATED));
        ORDER.put(OrderStatus.EXECUTED, EnumSet.noneOf(OrderStatus.class)); // terminal
        ORDER.put(OrderStatus.VACATED, EnumSet.noneOf(OrderStatus.class));  // terminal
    }

    /** Case states that still accept a court hearing (open cases). Concluded cases do not. */
    private static final Set<CaseStatus> OPEN_FOR_HEARING =
            EnumSet.of(CaseStatus.FILED, CaseStatus.PENDING, CaseStatus.HEARING_SCHEDULED);

    private LegalStatusPolicy() {}

    /** True while a case is still open enough to schedule/hold a hearing (not decreed, settled or withdrawn). */
    public static boolean isOpenForHearing(CaseStatus status) {
        return OPEN_FOR_HEARING.contains(status);
    }

    /** @throws BusinessRuleException if a hearing may not be recorded because the case is already concluded. */
    public static void assertHearingAllowed(CaseStatus status) {
        if (!isOpenForHearing(status)) {
            throw new BusinessRuleException(
                    "Cannot record a hearing: the case is " + status + " and already concluded.",
                    "CASE_CONCLUDED");
        }
    }

    /**
     * The case status a hearing outcome drives the case to. A hearing that is only scheduled (no outcome
     * yet) moves the case to {@code HEARING_SCHEDULED}; a recorded outcome advances it:
     * {@code ORDER_PASSED → DECREED}, {@code SETTLED → SETTLED}, {@code DISMISSED → WITHDRAWN},
     * and {@code ADJOURNED / PARTIALLY_HEARD → HEARING_SCHEDULED} (still in progress). The engine only
     * proposes a target; {@link #assertCaseTransition} still validates it against the current state.
     */
    public static CaseStatus caseStatusForOutcome(HearingOutcome outcome) {
        if (outcome == null) {
            return CaseStatus.HEARING_SCHEDULED; // scheduling a hearing, not yet held
        }
        return switch (outcome) {
            case ORDER_PASSED -> CaseStatus.DECREED;
            case SETTLED -> CaseStatus.SETTLED;
            case DISMISSED -> CaseStatus.WITHDRAWN;
            case ADJOURNED, PARTIALLY_HEARD -> CaseStatus.HEARING_SCHEDULED;
        };
    }

    /** @throws BusinessRuleException if {@code from → to} is not a permitted case transition. */
    public static void assertCaseTransition(CaseStatus from, CaseStatus to) {
        if (from == to) return;
        if (!CASE.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessRuleException(
                    "Illegal case status change: " + from + " → " + to
                            + ". Allowed from " + from + ": " + CASE.getOrDefault(from, Set.of()),
                    "INVALID_CASE_TRANSITION");
        }
    }

    /** @throws BusinessRuleException if {@code from → to} is not a permitted recovery-order transition. */
    public static void assertOrderTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return;
        if (!ORDER.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessRuleException(
                    "Illegal recovery-order status change: " + from + " → " + to
                            + ". Allowed from " + from + ": " + ORDER.getOrDefault(from, Set.of()),
                    "INVALID_ORDER_TRANSITION");
        }
    }
}
