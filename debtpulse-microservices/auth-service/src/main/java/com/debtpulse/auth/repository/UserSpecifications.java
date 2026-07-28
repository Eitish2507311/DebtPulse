package com.debtpulse.auth.repository;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.entity.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filter predicates for the paginated user list. Each optional filter contributes a
 * predicate only when its value is present, so callers compose exactly the filters they supply.
 */
public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> withFilters(Role role,
                                                  String branchId,
                                                  UserStatus status) {
        Specification<User> spec = Specification.where(null);
        if (role != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("role"), role));
        }
        if (branchId != null && !branchId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("branchId"), branchId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        return spec;
    }
}
