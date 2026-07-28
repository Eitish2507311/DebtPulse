package com.debtpulse.account.service.allocation;

import com.debtpulse.common.enums.AllocationStrategy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Assigns to the candidate currently holding the fewest accounts (ties broken by id for determinism). */
@Component
public class LeastLoadedStrategy implements AllocationStrategyHandler {

    @Override
    public AllocationStrategy strategy() {
        return AllocationStrategy.LEAST_LOADED;
    }

    @Override
    public Optional<String> choose(List<String> candidateIds, Map<String, Long> loadByUser) {
        return candidateIds.stream()
                .min(Comparator.<String>comparingLong(id -> loadByUser.getOrDefault(id, 0L))
                        .thenComparing(Comparator.naturalOrder()));
    }
}
