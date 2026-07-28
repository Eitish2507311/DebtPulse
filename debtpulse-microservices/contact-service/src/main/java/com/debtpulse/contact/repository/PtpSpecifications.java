package com.debtpulse.contact.repository;

import com.debtpulse.common.enums.PtpStatus;
import com.debtpulse.contact.entity.PromiseToPay;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Dynamic filter predicates for the paginated promise-to-pay list. Each optional filter contributes
 * a predicate only when its value is present, so callers compose exactly the filters they supply.
 */
public final class PtpSpecifications {

    private PtpSpecifications() {}

    public static Specification<PromiseToPay> withFilters(String accountId,
                                                          String agentId,
                                                          PtpStatus status,
                                                          LocalDate from,
                                                          LocalDate to) {
        Specification<PromiseToPay> spec = Specification.where(null);
        if (accountId != null && !accountId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accountId"), accountId));
        }
        if (agentId != null && !agentId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("agentId"), agentId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        // commitmentDate is a LocalDate, so the range bounds apply directly.
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("commitmentDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("commitmentDate"), to));
        }
        return spec;
    }
}
