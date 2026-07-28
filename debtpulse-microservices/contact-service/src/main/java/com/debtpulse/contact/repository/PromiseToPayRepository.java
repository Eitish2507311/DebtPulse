package com.debtpulse.contact.repository;

import com.debtpulse.common.enums.PtpStatus;
import com.debtpulse.contact.entity.PromiseToPay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Promise-to-pay persistence. Paging + derived finders/counters, plus
 * {@link JpaSpecificationExecutor} because the list endpoint needs dynamic
 * Specification-based filtering.
 */
public interface PromiseToPayRepository extends PagingAndSortingRepository<PromiseToPay, String>,
        CrudRepository<PromiseToPay, String>, JpaSpecificationExecutor<PromiseToPay> {

    Page<PromiseToPay> findByAccountId(String accountId, Pageable pageable);

    long count();

    long countByStatus(PtpStatus status);

    long countByAccountIdAndStatus(String accountId, PtpStatus status);

    /** ACTIVE PTPs whose commitment date has already passed — the daily breach sweep. */
    List<PromiseToPay> findByStatusAndCommitmentDateBefore(PtpStatus status, LocalDate date);
}
