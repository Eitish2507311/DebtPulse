package com.debtpulse.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt a state-changing controller method OUT of {@link HttpAuditAspect}'s automatic auditing.
 * Rarely needed — the aspect already skips methods that audit themselves (via {@link AuditContext})
 * or are annotated {@link Auditable}. Use this only for a mutating endpoint that should intentionally
 * produce no audit row.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipAutoAudit {
}
