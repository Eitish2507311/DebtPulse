package com.debtpulse.analytics.repository;

import com.debtpulse.analytics.entity.RecoveryReport;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Dynamic filter predicates for the paginated report list (DP5-22). Each optional filter contributes
 * a predicate only when supplied, so callers compose exactly the filters they pass.
 */
public final class ReportSpecifications {

    private ReportSpecifications() {}

    public static Specification<RecoveryReport> withFilters(String scope,
                                                            LocalDateTime from,
                                                            LocalDateTime to) {
        Specification<RecoveryReport> spec = Specification.where(null);
        if (scope != null && !scope.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("scope"), scope));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("generatedDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("generatedDate"), to));
        }
        return spec;
    }
}
