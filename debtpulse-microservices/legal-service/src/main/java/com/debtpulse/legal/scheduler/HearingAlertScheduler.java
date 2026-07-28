package com.debtpulse.legal.scheduler;

import com.debtpulse.legal.entity.CourtHearing;
import com.debtpulse.legal.entity.LegalCase;
import com.debtpulse.legal.feign.NotificationClient;
import com.debtpulse.legal.feign.dto.NotificationRequest;
import com.debtpulse.legal.repository.CourtHearingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Daily job that alerts the owning legal officer about hearings due in exactly three days.
 *
 * <p>Runs at 07:30 every morning. Both the primary {@code hearingDate} and any scheduled
 * {@code nextHearingDate} that fall on {@code today + 3} are covered, so officers get a
 * reminder whether the hearing was recorded directly or scheduled as a follow-up.</p>
 */
@Component
public class HearingAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(HearingAlertScheduler.class);
    private static final String CATEGORY_LEGAL = "LEGAL";
    private static final int LEAD_DAYS = 3;

    private final CourtHearingRepository hearingRepo;
    private final NotificationClient notificationClient;

    public HearingAlertScheduler(CourtHearingRepository hearingRepo, NotificationClient notificationClient) {
        this.hearingRepo = hearingRepo;
        this.notificationClient = notificationClient;
    }

    @Scheduled(cron = "0 30 7 * * ?")
    @Transactional(readOnly = true)
    public void alertUpcomingHearings() {
        LocalDate target = LocalDate.now().plusDays(LEAD_DAYS);

        // De-duplicate by hearing id: a hearing could match on both hearingDate and nextHearingDate.
        Map<String, CourtHearing> due = new LinkedHashMap<>();
        hearingRepo.findByHearingDate(target).forEach(h -> due.put(h.getHearingId(), h));
        hearingRepo.findByNextHearingDate(target).forEach(h -> due.put(h.getHearingId(), h));

        if (due.isEmpty()) {
            log.info("HearingAlertScheduler: no hearings due on {}", target);
            return;
        }

        int sent = 0;
        for (CourtHearing hearing : due.values()) {
            LegalCase legalCase = hearing.getLegalCase();
            String officerId = legalCase == null ? null : legalCase.getLegalOfficerId();
            if (officerId == null || officerId.isBlank()) {
                log.warn("HearingAlertScheduler: hearing {} has no legal officer to notify", hearing.getHearingId());
                continue;
            }
            String caseNumber = legalCase.getCaseNumber();
            notificationClient.notify(new NotificationRequest(
                    officerId,
                    "Upcoming hearing on " + target + " for case " + caseNumber,
                    CATEGORY_LEGAL));
            sent++;
        }
        log.info("HearingAlertScheduler: notified {} officer(s) for {} hearing(s) due on {}",
                sent, due.size(), target);
    }
}
