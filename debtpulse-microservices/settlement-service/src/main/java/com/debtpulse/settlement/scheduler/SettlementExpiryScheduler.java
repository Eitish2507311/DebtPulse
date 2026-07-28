package com.debtpulse.settlement.scheduler;

import com.debtpulse.common.enums.SettlementStatus;
import com.debtpulse.settlement.entity.SettlementProposal;
import com.debtpulse.settlement.feign.AuthClient;
import com.debtpulse.settlement.feign.NotificationClient;
import com.debtpulse.settlement.feign.dto.AuditLogRequest;
import com.debtpulse.settlement.feign.dto.NotificationRequest;
import com.debtpulse.settlement.repository.SettlementProposalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Nightly job (01:00) that expires APPROVED settlements whose payment deadline has passed
 * and notifies the raising officer so they can follow up.
 */
@Component
public class SettlementExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementExpiryScheduler.class);
    private static final String SOURCE = "settlement-service";

    private final SettlementProposalRepository repo;
    private final NotificationClient notificationClient;
    private final AuthClient authClient;

    public SettlementExpiryScheduler(SettlementProposalRepository repo,
                                     NotificationClient notificationClient,
                                     AuthClient authClient) {
        this.repo = repo;
        this.notificationClient = notificationClient;
        this.authClient = authClient;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void expireOverdueSettlements() {
        List<SettlementProposal> overdue =
                repo.findByStatusAndPaymentDeadlineBefore(SettlementStatus.APPROVED, LocalDate.now());
        if (overdue.isEmpty()) {
            log.info("Settlement expiry job: no APPROVED settlements past deadline");
            return;
        }
        log.info("Settlement expiry job: expiring {} settlement(s)", overdue.size());
        for (SettlementProposal proposal : overdue) {
            proposal.setStatus(SettlementStatus.EXPIRED);
            repo.save(proposal);
            if (proposal.getOfficerId() != null) {
                notificationClient.notify(new NotificationRequest(
                        proposal.getOfficerId(),
                        "Settlement proposal " + proposal.getProposalId()
                                + " has EXPIRED (payment deadline passed).",
                        "SETTLEMENT"));
            }
            authClient.audit(new AuditLogRequest(
                    "SYSTEM", "SETTLEMENT_EXPIRED", "SettlementProposal",
                    proposal.getProposalId(), SOURCE));
        }
    }
}
