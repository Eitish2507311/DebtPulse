package com.debtpulse.account.mapper;

import com.debtpulse.account.dto.request.AllocationRuleRequest;
import com.debtpulse.account.entity.AllocationRule;
import org.springframework.stereotype.Component;

/** Builds {@link AllocationRule} entities from request payloads. */
@Component
public class AllocationRuleMapper {

    public AllocationRule toEntity(AllocationRuleRequest req) {
        return AllocationRule.builder()
                .name(req.name())
                .strategy(req.strategy())
                .bucket(req.bucket())
                .targetRole(req.targetRole())
                .daysInBucketThreshold(req.daysInBucketThreshold())
                .minDpd(req.minDpd())
                .gracePeriodDays(req.gracePeriodDays())
                .capacityLimit(req.capacityLimit())
                .branchId(req.branchId())
                .priority(req.priority() != null ? req.priority() : 0)
                .autoEscalate(req.autoEscalate() != null && req.autoEscalate())
                .active(req.active() == null || req.active())
                .build();
    }
}
