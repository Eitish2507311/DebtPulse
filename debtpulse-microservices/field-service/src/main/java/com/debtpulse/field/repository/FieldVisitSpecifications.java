package com.debtpulse.field.repository;

import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.entity.FieldVisit;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Dynamic filter predicates for the paginated field-visit list. Each optional filter contributes a
 * predicate only when its value is present, so callers compose exactly the filters they supply.
 */
public final class FieldVisitSpecifications {

    private FieldVisitSpecifications() {}

    public static Specification<FieldVisit> withFilters(String accountId,
                                                        String officerId,
                                                        VisitStatus status,
                                                        LocalDate from,
                                                        LocalDate to) {
        Specification<FieldVisit> spec = Specification.where(null);
        if (accountId != null && !accountId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accountId"), accountId));
        }
        if (officerId != null && !officerId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("officerId"), officerId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("scheduledDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("scheduledDate"), to));
        }
        return spec;
    }
}
