package com.debtpulse.account.service.impl;

import com.debtpulse.common.enums.AssetType;
import com.debtpulse.common.enums.VerificationStatus;
import com.debtpulse.account.dto.request.CollateralAssetRequest;
import com.debtpulse.account.entity.CollateralAsset;
import com.debtpulse.account.mapper.CollateralAssetMapper;
import com.debtpulse.account.repository.CollateralAssetRepository;
import com.debtpulse.account.repository.CollateralAssetSpecifications;
import com.debtpulse.account.repository.DelinquentAccountRepository;
import com.debtpulse.account.service.CollateralAssetService;
import com.debtpulse.account.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollateralAssetServiceImpl implements CollateralAssetService {

    private static final Logger log = LoggerFactory.getLogger(CollateralAssetServiceImpl.class);

    private final CollateralAssetRepository repo;
    private final CollateralAssetMapper mapper;
    private final DelinquentAccountRepository accountRepo;

    public CollateralAssetServiceImpl(CollateralAssetRepository repo,
                                      CollateralAssetMapper mapper,
                                      DelinquentAccountRepository accountRepo) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountRepo = accountRepo;
    }

    @Override
    public CollateralAsset create(CollateralAssetRequest req) {
        if (!accountRepo.existsById(req.accountId())) {
            throw new ResourceNotFoundException("Account not found: " + req.accountId());
        }
        CollateralAsset entity = mapper.toEntity(req);
        // Registered collateral is considered verified at origination; a field visit re-verifies later.
        entity.setVerificationStatus(VerificationStatus.VERIFIED);
        entity.setLastVerifiedDate(req.lastVerifiedDate() != null
                ? req.lastVerifiedDate().atStartOfDay() : java.time.LocalDateTime.now());
        CollateralAsset saved = repo.save(entity);
        log.info("Created collateral asset id={} for account={} type={}",
                saved.getAssetId(), saved.getAccountId(), saved.getAssetType());
        return saved;
    }

    @Override
    public Page<CollateralAsset> list(String accountId,
                                      AssetType assetType,
                                      VerificationStatus verificationStatus,
                                      Pageable pageable) {
        return repo.findAll(
                CollateralAssetSpecifications.withFilters(accountId, assetType, verificationStatus),
                pageable);
    }

    @Override
    public List<CollateralAsset> getByAccount(String accountId) {
        return repo.findByAccountId(accountId);
    }

    @Override
    public CollateralAsset get(String id) {
        return find(id);
    }

    @Override
    public CollateralAsset update(String id, CollateralAssetRequest req) {
        CollateralAsset asset = find(id);
        if (req.assetType() != null) asset.setAssetType(req.assetType());
        if (req.description() != null) asset.setDescription(req.description());
        if (req.estimatedValue() != null) asset.setEstimatedValue(req.estimatedValue());
        if (req.lastVerifiedDate() != null) asset.setLastVerifiedDate(req.lastVerifiedDate().atStartOfDay());
        return repo.save(asset);
    }

    @Override
    public void delete(String id) {
        CollateralAsset asset = find(id);
        repo.delete(asset);
        log.info("Deleted collateral asset id={} (account={})", id, asset.getAccountId());
    }

    @Override
    public CollateralAsset markVerified(String assetId) {
        CollateralAsset asset = find(assetId);
        asset.setVerificationStatus(VerificationStatus.VERIFIED);
        asset.setLastVerifiedDate(LocalDateTime.now());
        CollateralAsset saved = repo.save(asset);
        log.info("Collateral asset id={} marked VERIFIED", assetId);
        return saved;
    }

    private CollateralAsset find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collateral asset not found: " + id));
    }
}
