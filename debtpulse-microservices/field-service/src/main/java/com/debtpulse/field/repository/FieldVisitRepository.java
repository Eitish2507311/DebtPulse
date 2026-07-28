package com.debtpulse.field.repository;

import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.entity.FieldVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Field-visit persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * for CRUD, paging, derived finders and counts, plus {@link JpaSpecificationExecutor} for the
 * dynamic Specification-based filtering the list endpoint needs.
 */
public interface FieldVisitRepository extends PagingAndSortingRepository<FieldVisit, String>,
        CrudRepository<FieldVisit, String>, JpaSpecificationExecutor<FieldVisit> {

    Page<FieldVisit> findByAccountId(String accountId, Pageable pageable);

    List<FieldVisit> findByOfficerId(String officerId);

    List<FieldVisit> findByStatusAndScheduledDate(VisitStatus status, LocalDate scheduledDate);

    long countByStatus(VisitStatus status);
}
