package com.debtpulse.account.repository;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.entity.DelinquentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Delinquent-account persistence. Uses {@link JpaRepository} + {@link JpaSpecificationExecutor}
 * because the list endpoint needs dynamic Specification-based filtering.
 */
public interface DelinquentAccountRepository extends JpaRepository<DelinquentAccount, String>,
        JpaSpecificationExecutor<DelinquentAccount> {

    List<DelinquentAccount> findByStatus(AccountStatus status);

    List<DelinquentAccount> findByStatusAndAssignedAgentIdIsNull(AccountStatus status);

    long countByAssignedAgentId(String assignedAgentId);

    long countByStatus(AccountStatus status);

    long countByBucket(DpdBucket bucket);

    @Query("select coalesce(sum(a.totalOverdue), 0) from DelinquentAccount a")
    BigDecimal sumTotalOverdue();
}
