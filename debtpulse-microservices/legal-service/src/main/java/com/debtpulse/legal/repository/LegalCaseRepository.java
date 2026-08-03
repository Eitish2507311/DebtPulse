package com.debtpulse.legal.repository;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.legal.entity.LegalCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Legal case persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * (not JpaRepository) since only CRUD, paging and derived finders/counts are needed.
 */
public interface LegalCaseRepository extends PagingAndSortingRepository<LegalCase, String>,
        CrudRepository<LegalCase, String> {

    long countByStatus(CaseStatus status);

    /** Cases in a given lifecycle state — powers the status-filtered list (e.g. the Hearing Scheduled tab). */
    Page<LegalCase> findByStatus(CaseStatus status, Pageable pageable);

    /** Cases for a given account — powers the Cases-tab search by Account ID. */
    Page<LegalCase> findByAccountId(String accountId, Pageable pageable);

    /** Cases in a given state AND for a given account (both filters applied). */
    Page<LegalCase> findByStatusAndAccountId(CaseStatus status, String accountId, Pageable pageable);

    /** Case-number uniqueness guards. */
    boolean existsByCaseNumber(String caseNumber);

    boolean existsByCaseNumberAndCaseIdNot(String caseNumber, String caseId);
}
