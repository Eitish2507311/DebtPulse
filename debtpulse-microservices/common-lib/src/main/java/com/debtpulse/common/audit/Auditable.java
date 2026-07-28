package com.debtpulse.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an auditable business action. The {@link com.debtpulse.common.audit.AuditAspect}
 * intercepts it and emits a structured {@link AuditEvent} (who / what / when / where / outcome)
 * through the configured {@link AuditPublisher} — a reusable, declarative alternative to scattering
 * hand-written audit calls through the code.
 *
 * <pre>{@code
 *   @Auditable(action = "SETTLEMENT_DECISION", entity = "SettlementProposal", entityId = "#id")
 *   public SettlementResponse decide(String id, ApprovalDecisionRequest req) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Business action name, e.g. LOGIN, CREATE, STATUS_CHANGE, SETTLEMENT_DECISION. */
    String action();

    /** Logical entity/aggregate the action applies to, e.g. User, DelinquentAccount. */
    String entity() default "";

    /**
     * SpEL expression resolving the affected record id. Method parameters are available by name
     * (e.g. {@code #id}, {@code #req.accountId()}) and the return value as {@code #result}.
     */
    String entityId() default "";
}
