package com.debtpulse.account.repository;

import com.debtpulse.account.entity.AllocationRule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Allocation-rule persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * (not JpaRepository) since only CRUD, paging and derived finders are needed.
 */
public interface AllocationRuleRepository extends PagingAndSortingRepository<AllocationRule, String>,
        CrudRepository<AllocationRule, String> {

    List<AllocationRule> findByActiveTrueOrderByPriorityDesc();
}
