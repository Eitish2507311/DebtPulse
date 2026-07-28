package com.debtpulse.account.repository;

import com.debtpulse.common.enums.AssetType;
import com.debtpulse.common.enums.VerificationStatus;
import com.debtpulse.account.entity.CollateralAsset;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dynamic filter predicates for the paginated collateral-asset list. Each optional filter
 * contributes a predicate only when its value is present, so callers compose exactly the
 * filters they supply.
 */
public final class CollateralAssetSpecifications {

    private CollateralAssetSpecifications() {}

    public static Specification<CollateralAsset> withFilters(String accountId,
                                                             AssetType assetType,
                                                             VerificationStatus verificationStatus) {
        Specification<CollateralAsset> spec = Specification.where(null);
        if (accountId != null && !accountId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accountId"), accountId));
        }
        if (assetType != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("assetType"), assetType));
        }
        if (verificationStatus != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("verificationStatus"), verificationStatus));
        }
        return spec;
    }
}
