package com.debtpulse.notification.repository;

import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.NotifStatus;
import com.debtpulse.notification.entity.Notification;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Dynamic filter predicates for the current user's paginated notification list. The recipient
 * predicate is mandatory (the list is always scoped to the authenticated caller); each optional
 * filter contributes a predicate only when its value is present, so callers compose exactly the
 * filters they supply.
 */
public final class NotificationSpecifications {

    private NotificationSpecifications() {}

    public static Specification<Notification> withFilters(String userId,
                                                          NotifCategory category,
                                                          NotifStatus status,
                                                          LocalDateTime from,
                                                          LocalDateTime to) {
        Specification<Notification> spec =
                Specification.where((root, q, cb) -> cb.equal(root.get("userId"), userId));
        if (category != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category"), category));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdDate"), to));
        }
        return spec;
    }
}
