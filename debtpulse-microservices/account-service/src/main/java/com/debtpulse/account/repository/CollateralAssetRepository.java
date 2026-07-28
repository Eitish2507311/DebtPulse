package com.debtpulse.account.repository;

import com.debtpulse.account.entity.CollateralAsset;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Collateral-asset persistence. Uses {@link PagingAndSortingRepository} + {@link CrudRepository}
 * for CRUD and a derived finder, plus {@link JpaSpecificationExecutor} for the
 * dynamic Specification-based filtering behind the paginated list endpoint (DP5-33).
 */
public interface CollateralAssetRepository extends PagingAndSortingRepository<CollateralAsset, String>,
        CrudRepository<CollateralAsset, String>,
        JpaSpecificationExecutor<CollateralAsset> {

    List<CollateralAsset> findByAccountId(String accountId);
}
