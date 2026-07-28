package com.debtpulse.legal.repository;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.legal.entity.LegalCase;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Legal case persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * (not JpaRepository) since only CRUD, paging and derived finders/counts are needed.
 */
public interface LegalCaseRepository extends PagingAndSortingRepository<LegalCase, String>,
        CrudRepository<LegalCase, String> {

    long countByStatus(CaseStatus status);
}
