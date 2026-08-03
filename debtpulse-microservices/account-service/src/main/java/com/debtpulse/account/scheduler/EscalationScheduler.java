package com.debtpulse.account.scheduler;

import com.debtpulse.account.service.AllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Escalation job. Delegates to the allocation engine to reassign accounts that have stagnated past
 * an escalation rule's threshold (skipping those with an active Promise-To-Pay). The cadence is
 * externalised via {@code allocation.escalation.cron} (default 02:00 nightly) so it can be tuned per
 * environment without a code change; on-demand runs are available via {@code POST /api/allocations/escalate}.
 */
@Component
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final AllocationService allocationService;

    public EscalationScheduler(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Scheduled(cron = "${allocation.escalation.cron:0 0 2 * * ?}")
    public void runEscalation() {
        log.info("EscalationScheduler: starting escalation run");
        int reassigned = allocationService.reassignForEscalation();
        log.info("EscalationScheduler: reassigned {} account(s)", reassigned);
    }
}
