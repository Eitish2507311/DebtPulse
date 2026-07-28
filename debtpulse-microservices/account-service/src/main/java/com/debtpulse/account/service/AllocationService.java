package com.debtpulse.account.service;

import com.debtpulse.account.dto.request.AllocationRuleRequest;
import com.debtpulse.account.entity.AllocationRule;
import com.debtpulse.account.entity.DelinquentAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/** Allocation-rule CRUD plus the allocation and escalation engines (2.2). */
public interface AllocationService {

    Page<AllocationRule> listRules(Pageable pageable);

    AllocationRule getRule(String id);

    AllocationRule createRule(AllocationRuleRequest request);

    AllocationRule updateRule(String id, AllocationRuleRequest request);

    void deleteRule(String id);

    /** Assign every unassigned ACTIVE account to an active collections agent (least-loaded). */
    Map<String, Object> executeAllocation();

    /**
     * Pick a least-loaded active collections agent for a single account and set its
     * {@code assignedAgentId} in memory (the caller persists). Leaves it null if no agent exists.
     */
    DelinquentAccount autoAllocate(DelinquentAccount account);

    /**
     * Reassign accounts that have stagnated past an active rule's bucket/day threshold to a user of
     * the rule's target role — skipping accounts that still have an active Promise-To-Pay.
     *
     * @return number of accounts reassigned
     */
    int reassignForEscalation();
}
