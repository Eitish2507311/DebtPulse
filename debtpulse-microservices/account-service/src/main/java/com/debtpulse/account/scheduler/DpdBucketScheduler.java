package com.debtpulse.account.scheduler;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.account.repository.DelinquentAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly DPD ageing job (00:30). Every ACTIVE account gains one day-past-due; its bucket is
 * re-classified. When the bucket changes the days-in-current-bucket counter resets, otherwise it
 * increments — feeding the escalation engine.
 */
@Component
public class DpdBucketScheduler {

    private static final Logger log = LoggerFactory.getLogger(DpdBucketScheduler.class);

    private final DelinquentAccountRepository repo;

    public DpdBucketScheduler(DelinquentAccountRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 30 0 * * ?")
    public void ageDpdBuckets() {
        List<DelinquentAccount> active = repo.findByStatus(AccountStatus.ACTIVE);
        log.info("DpdBucketScheduler: ageing {} active account(s)", active.size());

        for (DelinquentAccount account : active) {
            int dpd = (account.getDpd() == null ? 0 : account.getDpd()) + 1;
            account.setDpd(dpd);

            DpdBucket previous = account.getBucket();
            DpdBucket current = DpdBucket.classify(dpd);
            account.setBucket(current);

            if (current != previous) {
                account.setDaysInCurrentBucket(0);
            } else {
                int days = account.getDaysInCurrentBucket() == null ? 0 : account.getDaysInCurrentBucket();
                account.setDaysInCurrentBucket(days + 1);
            }
        }
        repo.saveAll(active);
        log.info("DpdBucketScheduler: completed ageing run");
    }
}
