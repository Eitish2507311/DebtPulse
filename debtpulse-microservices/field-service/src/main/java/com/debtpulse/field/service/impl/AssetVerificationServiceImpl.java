package com.debtpulse.field.service.impl;

import com.debtpulse.field.exception.ResourceNotFoundException;
import com.debtpulse.common.security.AuthContext;
import com.debtpulse.common.audit.AuditContext;
import com.debtpulse.field.dto.request.AssetVerificationRequest;
import com.debtpulse.field.dto.response.AssetVerificationDto;
import com.debtpulse.field.entity.AssetVerificationReport;
import com.debtpulse.field.feign.AccountClient;
import com.debtpulse.field.feign.AuthClient;
import com.debtpulse.field.feign.dto.AuditLogRequest;
import com.debtpulse.field.mapper.AssetVerificationMapper;
import com.debtpulse.field.repository.AssetVerificationReportRepository;
import com.debtpulse.field.service.AssetVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssetVerificationServiceImpl implements AssetVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AssetVerificationServiceImpl.class);
    private static final String SOURCE = "field-service";

    private final AssetVerificationReportRepository repo;
    private final AssetVerificationMapper mapper;
    private final AccountClient accountClient;
    private final AuthClient authClient;

    public AssetVerificationServiceImpl(AssetVerificationReportRepository repo,
                                        AssetVerificationMapper mapper,
                                        AccountClient accountClient, AuthClient authClient) {
        this.repo = repo;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.authClient = authClient;
    }

    @Override
    public AssetVerificationDto create(AssetVerificationRequest req) {
        AssetVerificationReport report = mapper.toEntity(req);
        if (report.getVerifiedById() == null || report.getVerifiedById().isBlank()) {
            report.setVerifiedById(AuthContext.currentUserId());
        }
        if (report.getVerificationDate() == null) {
            report.setVerificationDate(LocalDate.now());
        }
        AssetVerificationReport saved = repo.save(report);
        log.info("Asset verification report created id={} asset={} condition={}",
                saved.getReportId(), saved.getAssetId(), saved.getCondition());

        // Tell account-service to flag the collateral asset as VERIFIED.
        accountClient.markCollateralVerified(saved.getAssetId());

        audit("CREATE_ASSET_VERIFICATION", saved.getReportId());
        return mapper.toDto(saved);
    }

    @Override
    public Page<AssetVerificationDto> list(Pageable pageable) {
        return repo.findAll(pageable).map(mapper::toDto);
    }

    @Override
    public List<AssetVerificationDto> byVisit(String visitId) {
        return repo.findByVisitId(visitId).stream().map(mapper::toDto).toList();
    }

    @Override
    public AssetVerificationDto getById(String id) {
        return mapper.toDto(find(id));
    }

    @Override
    public AssetVerificationDto update(String id, AssetVerificationRequest req) {
        AssetVerificationReport report = find(id);
        report.setVisitId(req.visitId());
        report.setAssetId(req.assetId());
        report.setCondition(req.condition());
        report.setCurrentLocation(req.currentLocation());
        report.setRealisableValue(req.realisableValue());
        report.setRemarks(req.remarks());
        if (req.verifiedById() != null) report.setVerifiedById(req.verifiedById());
        if (req.verificationDate() != null) report.setVerificationDate(req.verificationDate());

        AssetVerificationReport saved = repo.save(report);
        log.info("Asset verification report updated id={}", saved.getReportId());
        audit("UPDATE_ASSET_VERIFICATION", saved.getReportId());
        return mapper.toDto(saved);
    }

    private AssetVerificationReport find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset verification report not found: " + id));
    }

    private void audit(String action, String recordId) {
        authClient.audit(new AuditLogRequest(AuthContext.currentUserId(), action,
                "AssetVerificationReport", recordId, SOURCE));
        AuditContext.markRecorded();
    }
}
