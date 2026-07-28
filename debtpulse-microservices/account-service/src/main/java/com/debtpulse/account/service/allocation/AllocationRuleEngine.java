package com.debtpulse.account.service.allocation;

import com.debtpulse.account.entity.AllocationRule;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.common.enums.AllocationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure, side-effect-free allocation decision logic (no I/O — trivially unit-testable). The service
 * layer fetches candidates/loads and persists; the engine only decides <em>which</em> rule applies
 * and <em>who</em> to assign. Rules are matched by bucket, DPD/grace, bucket-stagnation, and branch,
 * highest {@code priority} first; targeting is delegated to a per-strategy handler.
 */
@Component
public class AllocationRuleEngine {

    private final AllocationStrategyResolver strategyResolver;

    public AllocationRuleEngine(AllocationStrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    /** True if the rule's conditions all hold for the account. */
    public boolean matches(AllocationRule rule, DelinquentAccount account) {
        if (rule == null || !rule.isActive() || account == null) {
            return false;
        }
        if (rule.getBucket() != null && rule.getBucket() != account.getBucket()) {
            return false;
        }
        int dpd = account.getDpd() == null ? 0 : account.getDpd();
        if (rule.getMinDpd() != null && dpd < rule.getMinDpd()) {
            return false;
        }
        if (rule.getGracePeriodDays() != null && dpd < rule.getGracePeriodDays()) {
            return false;
        }
        if (rule.getDaysInBucketThreshold() != null) {
            Integer days = account.getDaysInCurrentBucket();
            if (days == null || days < rule.getDaysInBucketThreshold()) {
                return false;
            }
        }
        if (rule.getBranchId() != null && !rule.getBranchId().equals(account.getBranchId())) {
            return false;
        }
        return true;
    }

    /** The highest-priority active rule that matches the account, if any. */
    public Optional<AllocationRule> selectRule(DelinquentAccount account, List<AllocationRule> rulesByPriorityDesc) {
        return rulesByPriorityDesc.stream().filter(r -> matches(r, account)).findFirst();
    }

    /** Choose a target user for the given strategy from the (pre-filtered) candidates. */
    public Optional<String> chooseTarget(AllocationStrategy strategy, List<String> candidateIds,
                                         Map<String, Long> loadByUser) {
        return strategyResolver.choose(strategy, candidateIds, loadByUser);
    }
}
