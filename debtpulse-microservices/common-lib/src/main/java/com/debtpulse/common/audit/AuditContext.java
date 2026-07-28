package com.debtpulse.common.audit;

/**
 * Per-request (thread-local) flag marking that an audit record has already been emitted for the
 * current request by a manual {@code authClient.audit(...)} call or the {@link AuditAspect}
 * ({@code @Auditable}). The {@link HttpAuditAspect} consults it so its automatic, controller-level
 * auditing does not produce a DUPLICATE row for a method that already audits itself.
 *
 * <p>The {@link HttpAuditAspect} owns the lifecycle: it resets the flag before the controller runs
 * and clears it afterwards, so the thread-local never leaks across pooled request threads.</p>
 */
public final class AuditContext {

    private static final ThreadLocal<Boolean> RECORDED = new ThreadLocal<>();

    private AuditContext() {
    }

    /** Signal that this request's action has already been audited (manual or {@code @Auditable}). */
    public static void markRecorded() {
        RECORDED.set(Boolean.TRUE);
    }

    /** True if an audit record was already emitted for the current request. */
    public static boolean wasRecorded() {
        return Boolean.TRUE.equals(RECORDED.get());
    }

    /** Clear the flag (called at the start and end of each request by {@link HttpAuditAspect}). */
    public static void reset() {
        RECORDED.remove();
    }
}
