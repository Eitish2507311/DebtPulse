package com.debtpulse.legal.repository;

import com.debtpulse.legal.entity.CourtHearing;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

/** Court hearing persistence with derived finders for listing and scheduled alerts. */
public interface CourtHearingRepository extends CrudRepository<CourtHearing, String> {

    List<CourtHearing> findByLegalCase_CaseId(String caseId);

    List<CourtHearing> findByHearingDate(LocalDate hearingDate);

    List<CourtHearing> findByNextHearingDate(LocalDate nextHearingDate);
}
