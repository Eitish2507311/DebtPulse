package com.debtpulse.auth.repository;

import com.debtpulse.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Audit-trail persistence. Extends {@link PagingAndSortingRepository} + {@link CrudRepository}
 * for paging and derived finders, plus {@link JpaSpecificationExecutor} so the list endpoint can
 * apply dynamic Specification-based filtering.
 */
public interface AuditLogRepository extends PagingAndSortingRepository<AuditLog, String>,
        CrudRepository<AuditLog, String>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    List<AuditLog> findByEntityTypeAndRecordId(String entityType, String recordId);
}
