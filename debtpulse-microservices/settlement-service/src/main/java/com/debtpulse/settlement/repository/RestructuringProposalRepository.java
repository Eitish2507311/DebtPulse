package com.debtpulse.settlement.repository;

import com.debtpulse.settlement.entity.RestructuringProposal;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/** Restructuring persistence (CRUD + paging + derived finders). */
public interface RestructuringProposalRepository
        extends PagingAndSortingRepository<RestructuringProposal, String>,
        CrudRepository<RestructuringProposal, String> {

    List<RestructuringProposal> findByAccountId(String accountId);
}
