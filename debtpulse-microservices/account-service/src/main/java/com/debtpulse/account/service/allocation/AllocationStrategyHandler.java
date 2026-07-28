package com.debtpulse.account.service.allocation;

import com.debtpulse.common.enums.AllocationStrategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chooses which candidate user an account should be assigned to, for one allocation strategy.
 * Candidates are already filtered upstream (by role, branch and capacity); the handler only decides
 * distribution. Open/Closed: a new strategy is a new handler bean — no change to the engine.
 */
public interface AllocationStrategyHandler {

    AllocationStrategy strategy();

    /**
     * @param candidateIds eligible target user ids (non-empty when called)
     * @param loadByUser   current assigned-account count per user id
     * @return the chosen user id, or empty if none can be chosen
     */
    Optional<String> choose(List<String> candidateIds, Map<String, Long> loadByUser);
}
