package com.debtpulse.field.mapper;

import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.dto.request.ScheduleVisitRequest;
import com.debtpulse.field.dto.response.FieldVisitDto;
import com.debtpulse.field.entity.FieldVisit;
import org.springframework.stereotype.Component;

/** Converts between the {@link FieldVisit} entity and its DTOs. */
@Component
public class FieldVisitMapper {

    public FieldVisitDto toDto(FieldVisit v) {
        if (v == null) return null;
        return new FieldVisitDto(
                v.getVisitId(),
                v.getAccountId(),
                v.getOfficerId(),
                v.getScheduledDate(),
                v.getVisitDate(),
                v.getBorrowerMet(),
                v.getAssetSighted(),
                v.getOutcomeSummary(),
                v.getNextActionRequired(),
                v.getStatus() == null ? null : v.getStatus().name(),
                v.getCreatedAt()
        );
    }

    /** Builds a fresh SCHEDULED visit from a schedule request. */
    public FieldVisit toEntity(ScheduleVisitRequest req) {
        return FieldVisit.builder()
                .accountId(req.accountId())
                .officerId(req.officerId())
                .scheduledDate(req.scheduledDate())
                .nextActionRequired(req.nextActionRequired())
                .status(VisitStatus.SCHEDULED)
                .build();
    }
}
