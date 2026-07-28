package com.debtpulse.field.mapper;

import com.debtpulse.field.dto.request.AssetVerificationRequest;
import com.debtpulse.field.dto.response.AssetVerificationDto;
import com.debtpulse.field.entity.AssetVerificationReport;
import org.springframework.stereotype.Component;

/** Converts between the {@link AssetVerificationReport} entity and its DTOs. */
@Component
public class AssetVerificationMapper {

    public AssetVerificationDto toDto(AssetVerificationReport r) {
        if (r == null) return null;
        return new AssetVerificationDto(
                r.getReportId(),
                r.getVisitId(),
                r.getAssetId(),
                r.getCondition() == null ? null : r.getCondition().name(),
                r.getCurrentLocation(),
                r.getRealisableValue(),
                r.getRemarks(),
                r.getVerifiedById(),
                r.getVerificationDate(),
                r.getCreatedAt()
        );
    }

    public AssetVerificationReport toEntity(AssetVerificationRequest req) {
        return AssetVerificationReport.builder()
                .visitId(req.visitId())
                .assetId(req.assetId())
                .condition(req.condition())
                .currentLocation(req.currentLocation())
                .realisableValue(req.realisableValue())
                .remarks(req.remarks())
                .verifiedById(req.verifiedById())
                .verificationDate(req.verificationDate())
                .build();
    }
}
