package com.debtpulse.account.service.allocation;

import com.debtpulse.account.entity.AllocationRule;
import com.debtpulse.account.entity.DelinquentAccount;
import com.debtpulse.common.enums.AllocationStrategy;
import com.debtpulse.common.enums.DpdBucket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AllocationRuleEngineTest {

    private final AllocationRuleEngine engine = new AllocationRuleEngine(new AllocationStrategyResolver(
            List.of(new LeastLoadedStrategy(), new RoundRobinStrategy(), new BranchBasedStrategy())));

    private AllocationRule rule() {
        return AllocationRule.builder().ruleId("R").strategy(AllocationStrategy.LEAST_LOADED)
                .targetRole("LEGAL_OFFICER").priority(0).active(true).build();
    }

    private DelinquentAccount account() {
        return DelinquentAccount.builder().accountId("A").bucket(DpdBucket.NPA)
                .dpd(200).daysInCurrentBucket(20).branchId("B01").build();
    }

    @Test
    void matches_allConditions() {
        AllocationRule r = rule();
        r.setBucket(DpdBucket.NPA); r.setDaysInBucketThreshold(10); r.setMinDpd(180); r.setBranchId("B01");
        assertThat(engine.matches(r, account())).isTrue();
    }

    @Test
    void noMatch_onBucket() {
        AllocationRule r = rule(); r.setBucket(DpdBucket.X90);
        assertThat(engine.matches(r, account())).isFalse();
    }

    @Test
    void noMatch_whenBelowDaysInBucketThreshold() {
        AllocationRule r = rule(); r.setBucket(DpdBucket.NPA); r.setDaysInBucketThreshold(30);
        assertThat(engine.matches(r, account())).isFalse(); // account has 20 < 30
    }

    @Test
    void noMatch_whenWithinGracePeriod() {
        AllocationRule r = rule(); r.setGracePeriodDays(250); // account dpd 200 < 250
        assertThat(engine.matches(r, account())).isFalse();
    }

    @Test
    void noMatch_onBranch() {
        AllocationRule r = rule(); r.setBranchId("B99");
        assertThat(engine.matches(r, account())).isFalse();
    }

    @Test
    void inactiveRule_neverMatches() {
        AllocationRule r = rule(); r.setActive(false);
        assertThat(engine.matches(r, account())).isFalse();
    }

    @Test
    void selectRule_picksHighestPriorityMatch() {
        AllocationRule low = rule(); low.setRuleId("low"); low.setPriority(1); low.setBucket(DpdBucket.NPA);
        AllocationRule high = rule(); high.setRuleId("high"); high.setPriority(10); high.setBucket(DpdBucket.NPA);
        // repository returns priority-desc, so 'high' comes first
        assertThat(engine.selectRule(account(), List.of(high, low)))
                .get().extracting(AllocationRule::getRuleId).isEqualTo("high");
    }

    @Test
    void selectRule_emptyWhenNoneMatch() {
        AllocationRule r = rule(); r.setBucket(DpdBucket.X30);
        assertThat(engine.selectRule(account(), List.of(r))).isEmpty();
    }

    @Test
    void chooseTarget_leastLoaded_picksLowestLoad() {
        assertThat(engine.chooseTarget(AllocationStrategy.LEAST_LOADED,
                List.of("u1", "u2", "u3"), Map.of("u1", 5L, "u2", 2L, "u3", 9L)))
                .contains("u2");
    }

    @Test
    void chooseTarget_roundRobin_rotatesByTotalLoad() {
        // total load 3, 3 candidates → index 3 % 3 = 0 → first by natural order
        assertThat(engine.chooseTarget(AllocationStrategy.ROUND_ROBIN,
                List.of("uC", "uA", "uB"), Map.of("uA", 1L, "uB", 1L, "uC", 1L)))
                .contains("uA");
    }

    @Test
    void chooseTarget_emptyCandidates_returnsEmpty() {
        assertThat(engine.chooseTarget(AllocationStrategy.LEAST_LOADED, List.of(), Map.of())).isEmpty();
    }
}
