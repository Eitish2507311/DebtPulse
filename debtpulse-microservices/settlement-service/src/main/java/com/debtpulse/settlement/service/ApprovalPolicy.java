package com.debtpulse.settlement.service;

import com.debtpulse.common.enums.ApprovalLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single source of truth for the settlement approval matrix. The required approval chain is derived
 * purely from the haircut percentage — never supplied by the client — so authority can never be
 * under-stated. Thresholds are configurable (config-server) rather than hard-coded.
 *
 * <pre>
 *   haircut &lt; l2Threshold            -&gt; [L1]
 *   l2Threshold &le; haircut &lt; l3Threshold -&gt; [L1, L2]
 *   haircut &ge; l3Threshold            -&gt; [L1, L2, L3]
 * </pre>
 *
 * Approvals are cumulative and sequential: every level in the chain must approve, in order.
 */
@Component
public class ApprovalPolicy {

    private final BigDecimal l2Threshold;
    private final BigDecimal l3Threshold;

    public ApprovalPolicy(
            @Value("${settlement.approval.l2-threshold:10}") BigDecimal l2Threshold,
            @Value("${settlement.approval.l3-threshold:25}") BigDecimal l3Threshold) {
        this.l2Threshold = l2Threshold;
        this.l3Threshold = l3Threshold;
    }

    /** The ordered chain of approval levels required for the given haircut. Never empty. */
    public List<ApprovalLevel> requiredLevels(BigDecimal haircut) {
        BigDecimal h = haircut == null ? BigDecimal.ZERO : haircut;
        if (h.compareTo(l2Threshold) < 0) {
            return List.of(ApprovalLevel.L1);
        }
        if (h.compareTo(l3Threshold) < 0) {
            return List.of(ApprovalLevel.L1, ApprovalLevel.L2);
        }
        return List.of(ApprovalLevel.L1, ApprovalLevel.L2, ApprovalLevel.L3);
    }

    /** The highest (final) level required for the given haircut. */
    public ApprovalLevel highestLevel(BigDecimal haircut) {
        List<ApprovalLevel> chain = requiredLevels(haircut);
        return chain.get(chain.size() - 1);
    }

    /** The level that must act after {@code current}, or {@code null} if {@code current} is the last. */
    public ApprovalLevel nextLevel(BigDecimal haircut, ApprovalLevel current) {
        List<ApprovalLevel> chain = requiredLevels(haircut);
        int idx = chain.indexOf(current);
        return (idx >= 0 && idx < chain.size() - 1) ? chain.get(idx + 1) : null;
    }
}
