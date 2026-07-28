package com.debtpulse.contact.repository;

import com.debtpulse.contact.entity.BorrowerContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Borrower-contact persistence. Paging + derived finders only, so it extends
 * {@link PagingAndSortingRepository} + {@link CrudRepository} rather than JpaRepository.
 */
public interface BorrowerContactRepository extends PagingAndSortingRepository<BorrowerContact, String>,
        CrudRepository<BorrowerContact, String> {

    List<BorrowerContact> findByAccountId(String accountId);
}
