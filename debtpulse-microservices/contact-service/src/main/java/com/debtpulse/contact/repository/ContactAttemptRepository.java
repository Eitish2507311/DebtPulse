package com.debtpulse.contact.repository;

import com.debtpulse.common.enums.ContactOutcome;
import com.debtpulse.contact.entity.ContactAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Contact-attempt persistence. Paging + derived finders/counters, plus
 * {@link JpaSpecificationExecutor} because the list endpoint needs dynamic
 * Specification-based filtering.
 */
public interface ContactAttemptRepository extends PagingAndSortingRepository<ContactAttempt, String>,
        CrudRepository<ContactAttempt, String>, JpaSpecificationExecutor<ContactAttempt> {

    Page<ContactAttempt> findByAccountId(String accountId, Pageable pageable);

    long count();

    long countByOutcome(ContactOutcome outcome);
}
