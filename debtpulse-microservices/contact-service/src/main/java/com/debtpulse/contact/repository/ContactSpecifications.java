package com.debtpulse.contact.repository;

import com.debtpulse.common.enums.ContactChannel;
import com.debtpulse.common.enums.ContactOutcome;
import com.debtpulse.contact.entity.ContactAttempt;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dynamic filter predicates for the paginated contact-attempt list. Each optional filter contributes
 * a predicate only when its value is present, so callers compose exactly the filters they supply.
 */
public final class ContactSpecifications {

    private ContactSpecifications() {}

    public static Specification<ContactAttempt> withFilters(String accountId,
                                                            String agentId,
                                                            ContactChannel channel,
                                                            ContactOutcome outcome,
                                                            LocalDate from,
                                                            LocalDate to) {
        Specification<ContactAttempt> spec = Specification.where(null);
        if (accountId != null && !accountId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accountId"), accountId));
        }
        if (agentId != null && !agentId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("agentId"), agentId));
        }
        if (channel != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("channel"), channel));
        }
        if (outcome != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("outcome"), outcome));
        }
        // contactDate is a LocalDateTime; widen the LocalDate range to cover the whole day.
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("contactDate"), from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("contactDate"), to.atTime(LocalTime.MAX)));
        }
        return spec;
    }
}
