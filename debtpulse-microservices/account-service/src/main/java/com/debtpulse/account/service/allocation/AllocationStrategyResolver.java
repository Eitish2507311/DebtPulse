package com.debtpulse.account.service.allocation;

import com.debtpulse.common.enums.AllocationStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry of {@link AllocationStrategyHandler}s, keyed by strategy (populated by Spring). */
@Component
public class AllocationStrategyResolver {

    private final Map<AllocationStrategy, AllocationStrategyHandler> handlers = new EnumMap<>(AllocationStrategy.class);

    public AllocationStrategyResolver(List<AllocationStrategyHandler> handlerBeans) {
        handlerBeans.forEach(h -> handlers.put(h.strategy(), h));
    }

    public Optional<String> choose(AllocationStrategy strategy, List<String> candidateIds, Map<String, Long> loadByUser) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Optional.empty();
        }
        AllocationStrategyHandler handler = handlers.getOrDefault(strategy, handlers.get(AllocationStrategy.LEAST_LOADED));
        return handler == null ? Optional.empty() : handler.choose(candidateIds, loadByUser);
    }
}
