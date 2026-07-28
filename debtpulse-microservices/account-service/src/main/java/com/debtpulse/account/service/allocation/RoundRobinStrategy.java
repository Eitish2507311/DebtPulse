package com.debtpulse.account.service.allocation;

import com.debtpulse.common.enums.AllocationStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rotates assignments evenly across candidates. The rotation offset is derived from the total
 * accounts already assigned to the candidate set, so repeated calls (with the load map updated after
 * each assignment) distribute round-robin without needing a persisted pointer.
 */
@Component
public class RoundRobinStrategy implements AllocationStrategyHandler {

    @Override
    public AllocationStrategy strategy() {
        return AllocationStrategy.ROUND_ROBIN;
    }

    @Override
    public Optional<String> choose(List<String> candidateIds, Map<String, Long> loadByUser) {
        if (candidateIds.isEmpty()) {
            return Optional.empty();
        }
        List<String> ordered = new ArrayList<>(candidateIds);
        ordered.sort(java.util.Comparator.naturalOrder()); // deterministic order
        long total = ordered.stream().mapToLong(id -> loadByUser.getOrDefault(id, 0L)).sum();
        int index = (int) (total % ordered.size());
        return Optional.of(ordered.get(index));
    }
}
