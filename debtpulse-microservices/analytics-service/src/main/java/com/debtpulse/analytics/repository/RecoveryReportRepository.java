package com.debtpulse.analytics.repository;

import com.debtpulse.analytics.entity.RecoveryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Report persistence. {@link JpaSpecificationExecutor} backs the paginated {@code /reports} list,
 * which supports optional dynamic filtering (scope + generated-date range, DP5-22).
 */
public interface RecoveryReportRepository extends JpaRepository<RecoveryReport, String>,
        JpaSpecificationExecutor<RecoveryReport> {
}
