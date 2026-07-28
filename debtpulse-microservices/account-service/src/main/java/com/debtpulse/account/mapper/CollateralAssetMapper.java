package com.debtpulse.account.mapper;

import com.debtpulse.common.enums.VerificationStatus;
import com.debtpulse.account.dto.request.CollateralAssetRequest;
import com.debtpulse.account.entity.CollateralAsset;
import org.springframework.stereotype.Component;

/** Builds {@link CollateralAsset} entities from request payloads. */
@Component
public class CollateralAssetMapper {

    public CollateralAsset toEntity(CollateralAssetRequest req) {
        return CollateralAsset.builder()
                .accountId(req.accountId())
                .assetType(req.assetType())
                .description(req.description())
                .estimatedValue(req.estimatedValue())
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();
    }
}
