package com.debtpulse.settlement.repository;

import com.debtpulse.common.enums.SettlementStatus;
import com.debtpulse.settlement.entity.SettlementProposal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Settlement persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * with derived finders (no Specifications/dynamic filtering needed).
 */
public interface SettlementProposalRepository
        extends PagingAndSortingRepository<SettlementProposal, String>,
        CrudRepository<SettlementProposal, String> {

    List<SettlementProposal> findByStatus(SettlementStatus status);

    boolean existsByAccountIdAndStatusIn(String accountId, Collection<SettlementStatus> statuses);

    List<SettlementProposal> findByStatusNotIn(Collection<SettlementStatus> statuses);

    List<SettlementProposal> findByStatusAndPaymentDeadlineBefore(SettlementStatus status, LocalDate date);

    long countByStatus(SettlementStatus status);
}
