package com.debtpulse.legal.repository;

import com.debtpulse.legal.entity.RecoveryOrder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/** Recovery order persistence. */
public interface RecoveryOrderRepository extends PagingAndSortingRepository<RecoveryOrder, String>,
        CrudRepository<RecoveryOrder, String> {

    List<RecoveryOrder> findByLegalCase_CaseId(String caseId);
}
