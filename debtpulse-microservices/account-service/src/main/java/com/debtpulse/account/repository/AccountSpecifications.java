package com.debtpulse.account.repository;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.entity.DelinquentAccount;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filter predicates for the paginated account list. Each optional filter contributes a
 * predicate only when its value is present, so callers compose exactly the filters they supply.
 */
public final class AccountSpecifications {

    private AccountSpecifications() {}

    public static Specification<DelinquentAccount> withFilters(DpdBucket bucket,
                                                               AccountStatus status,
                                                               String agentId,
                                                               Integer dpdMin,
                                                               Integer dpdMax) {
        Specification<DelinquentAccount> spec = Specification.where(null);
        if (bucket != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("bucket"), bucket));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (agentId != null && !agentId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("assignedAgentId"), agentId));
        }
        if (dpdMin != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("dpd"), dpdMin));
        }
        if (dpdMax != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("dpd"), dpdMax));
        }
        return spec;
    }
}
