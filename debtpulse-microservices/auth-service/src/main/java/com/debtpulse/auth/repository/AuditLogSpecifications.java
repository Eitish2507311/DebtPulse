package com.debtpulse.auth.repository;

import com.debtpulse.auth.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dynamic filter predicates for the paginated audit-log list. Each optional filter contributes a
 * predicate only when its value is present, so callers compose exactly the filters they supply.
 * The {@code timestamp} is a {@link java.time.LocalDateTime}, so the date-range bounds are widened
 * to cover the whole day: {@code from} to start-of-day and {@code to} to end-of-day.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> withFilters(String userId,
                                                      String entityType,
                                                      String action,
                                                      LocalDate from,
                                                      LocalDate to) {
        Specification<AuditLog> spec = Specification.where(null);
        if (userId != null && !userId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to.atTime(LocalTime.MAX)));
        }
        return spec;
    }
}
