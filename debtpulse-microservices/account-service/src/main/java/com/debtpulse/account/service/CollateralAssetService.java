package com.debtpulse.account.service;

import com.debtpulse.common.enums.AssetType;
import com.debtpulse.common.enums.VerificationStatus;
import com.debtpulse.account.dto.request.CollateralAssetRequest;
import com.debtpulse.account.entity.CollateralAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Collateral-asset management (2.2). */
public interface CollateralAssetService {

    CollateralAsset create(CollateralAssetRequest request);

    /** Paginated list of all collateral assets, with optional filters (DP5-33). */
    Page<CollateralAsset> list(String accountId,
                               AssetType assetType,
                               VerificationStatus verificationStatus,
                               Pageable pageable);

    List<CollateralAsset> getByAccount(String accountId);

    CollateralAsset get(String id);

    CollateralAsset update(String id, CollateralAssetRequest request);

    /** Remove a collateral asset (DP5-33). */
    void delete(String id);

    /** Set VERIFIED and stamp {@code lastVerifiedDate = now} (invoked over Feign by field-service). */
    CollateralAsset markVerified(String assetId);
}
