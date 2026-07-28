package com.debtpulse.account.service.allocation;

import com.debtpulse.common.enums.AllocationStrategy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Branch-targeted allocation. The candidate set is pre-filtered to the account's branch by the
 * engine (via {@code activeByRole(role, branchId)}); within that branch this picks the least-loaded
 * candidate, keeping work with branch-local staff.
 */
@Component
public class BranchBasedStrategy implements AllocationStrategyHandler {

    @Override
    public AllocationStrategy strategy() {
        return AllocationStrategy.BRANCH_BASED;
    }

    @Override
    public Optional<String> choose(List<String> candidateIds, Map<String, Long> loadByUser) {
        return candidateIds.stream()
                .min(Comparator.<String>comparingLong(id -> loadByUser.getOrDefault(id, 0L))
                        .thenComparing(Comparator.naturalOrder()));
    }
}
