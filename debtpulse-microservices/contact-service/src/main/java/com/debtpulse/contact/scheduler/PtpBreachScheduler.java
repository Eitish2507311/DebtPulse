package com.debtpulse.contact.scheduler;

import com.debtpulse.contact.service.PtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily job (08:00) that marks ACTIVE promise-to-pay commitments whose commitment date has
 * lapsed as BROKEN and notifies the owning agent (category PTP). Delegates the work to
 * {@link PtpService#markBreachedPtps()} so the business logic stays testable.
 */
@Component
public class PtpBreachScheduler {

    private static final Logger log = LoggerFactory.getLogger(PtpBreachScheduler.class);

    private final PtpService ptpService;

    public PtpBreachScheduler(PtpService ptpService) {
        this.ptpService = ptpService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void sweepBreachedPtps() {
        log.info("Running PTP breach sweep");
        int count = ptpService.markBreachedPtps();
        log.info("PTP breach sweep complete: {} PTP(s) marked BROKEN", count);
    }
}
