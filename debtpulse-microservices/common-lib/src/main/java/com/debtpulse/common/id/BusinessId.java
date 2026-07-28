package com.debtpulse.common.id;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String {@code @Id} to be populated with a human-readable, sortable business identifier
 * of the form {@code PREFIX-YEAR-NNNNNN} (e.g. {@code ACC-2026-000001}).
 *
 * <p>Backed by {@link BusinessIdGenerator}, which allocates a gap-tolerant, per-prefix, per-year
 * sequence from the {@code id_sequence} table using an atomic MySQL upsert — safe across multiple
 * service instances. A value already set on the entity (e.g. seed data) is preserved as-is.</p>
 *
 * <pre>{@code
 *   @Id
 *   @BusinessId(prefix = "ACC")
 *   private String accountId;
 * }</pre>
 */
@IdGeneratorType(BusinessIdGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface BusinessId {

    /** Short uppercase entity prefix, e.g. {@code ACC}, {@code SET}, {@code LEG}. */
    String prefix();
}
