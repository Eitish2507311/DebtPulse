package com.debtpulse.field.repository;

import com.debtpulse.field.entity.AssetVerificationReport;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Asset-verification-report persistence. Uses {@link PagingAndSortingRepository} +
 * {@link CrudRepository} — only CRUD, paging and derived finders are needed.
 */
public interface AssetVerificationReportRepository
        extends PagingAndSortingRepository<AssetVerificationReport, String>,
        CrudRepository<AssetVerificationReport, String> {

    List<AssetVerificationReport> findByVisitId(String visitId);
}
