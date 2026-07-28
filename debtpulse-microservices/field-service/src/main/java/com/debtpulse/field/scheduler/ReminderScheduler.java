package com.debtpulse.field.scheduler;

import com.debtpulse.common.enums.VisitStatus;
import com.debtpulse.field.entity.FieldVisit;
import com.debtpulse.field.feign.NotificationClient;
import com.debtpulse.field.feign.dto.NotificationRequest;
import com.debtpulse.field.repository.FieldVisitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends a daily reminder (category {@code FIELD_VISIT}) to each field officer for every
 * SCHEDULED visit due the next day. Runs at 19:00 every day.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final String CATEGORY = "FIELD_VISIT";

    private final FieldVisitRepository repo;
    private final NotificationClient notificationClient;

    public ReminderScheduler(FieldVisitRepository repo, NotificationClient notificationClient) {
        this.repo = repo;
        this.notificationClient = notificationClient;
    }

    @Scheduled(cron = "0 0 19 * * ?")
    public void sendTomorrowVisitReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<FieldVisit> due = repo.findByStatusAndScheduledDate(VisitStatus.SCHEDULED, tomorrow);
        log.info("Field-visit reminder job: {} visit(s) scheduled for {}", due.size(), tomorrow);

        for (FieldVisit visit : due) {
            notificationClient.notify(new NotificationRequest(visit.getOfficerId(),
                    "Reminder: you have a field visit tomorrow (" + tomorrow + ") for account "
                            + visit.getAccountId(), CATEGORY));
        }
    }
}
