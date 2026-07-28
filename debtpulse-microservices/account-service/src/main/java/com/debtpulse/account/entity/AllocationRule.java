package com.debtpulse.account.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.AllocationStrategy;
import com.debtpulse.common.enums.DpdBucket;
import jakarta.persistence.*;
import lombok.*;

/**
 * A configurable rule that governs how accounts are allocated to agents or escalated to a
 * higher role once they stagnate in a bucket (2.2). {@code targetRole} holds a role NAME
 * string (e.g. {@code COLLECTIONS_AGENT}, {@code PORTFOLIO_MANAGER}).
 */
@Entity
@Table(name = "allocation_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRule {

    @Id
    @BusinessId(prefix = "ALR")
    private String ruleId;

    private String name;

    @Enumerated(EnumType.STRING)
    private AllocationStrategy strategy;

    @Enumerated(EnumType.STRING)
    private DpdBucket bucket;

    /** Role NAME string this rule allocates/escalates to (e.g. COLLECTIONS_AGENT, LEGAL_OFFICER). */
    private String targetRole;

    /** Minimum days-in-current-bucket before this rule applies (stagnation threshold). */
    private Integer daysInBucketThreshold;

    /** Minimum DPD before this rule applies (optional; bucket already encodes coarse DPD bands). */
    private Integer minDpd;

    /** Grace period in DPD — the rule does not act until the account is at least this many days past due. */
    private Integer gracePeriodDays;

    /** Max accounts a single target user may hold; candidates at/over this are skipped (optional). */
    private Integer capacityLimit;

    /** Restrict matching + targeting to this branch (optional). */
    private String branchId;

    @Builder.Default
    private Integer priority = 0;

    /**
     * Distinguishes the two rule kinds (mirrors the pre-microservices monolith):
     * <ul>
     *   <li>{@code false} (default) — an <b>initial-allocation</b> rule: applied when a fresh or
     *       unassigned account is placed onto a collection agent ({@code targetRole} is normally
     *       {@code COLLECTIONS_AGENT}).</li>
     *   <li>{@code true} — an <b>escalation</b> rule: applied by the nightly escalation job to move
     *       an account that has stagnated in a bucket up to a higher {@code targetRole}
     *       (e.g. {@code PORTFOLIO_MANAGER}, {@code LEGAL_OFFICER}).</li>
     * </ul>
     * Keeping the two kinds in one table but separated by this flag prevents an escalation rule
     * from ever grabbing a freshly imported account, and vice-versa.
     */
    @Builder.Default
    private boolean autoEscalate = false;

    @Builder.Default
    private boolean active = true;
}
