package com.debtpulse.field.service;

import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.dto.request.CompleteVisitRequest;
import com.debtpulse.field.dto.request.ScheduleVisitRequest;
import com.debtpulse.field.dto.response.FieldVisitDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Field-visit scheduling, lifecycle transitions and portfolio statistics. */
public interface FieldVisitService {

    FieldVisitDto schedule(ScheduleVisitRequest request);

    Page<FieldVisitDto> list(String accountId, String officerId, VisitStatus status,
                             LocalDate from, LocalDate to, Pageable pageable);

    List<FieldVisitDto> myVisits(String officerId);

    FieldVisitDto complete(String id, CompleteVisitRequest request);

    FieldVisitDto markMissed(String id);

    /** keys: totalVisits, completedVisits, missedVisits, fieldVisitSuccessRate. */
    Map<String, Object> stats();
}
