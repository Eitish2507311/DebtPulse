package com.debtpulse.account.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.AccountStatus;
import com.debtpulse.common.enums.DpdBucket;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A delinquent loan account (2.2 Delinquent Loan Portfolio Management).
 *
 * <p>Owned by account-service. Cross-service references ({@code assignedAgentId}) are plain
 * user-id strings — no {@code @ManyToOne} to entities owned by other services.</p>
 */
@Entity
@Table(name = "delinquent_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DelinquentAccount {

    @Id
    @BusinessId(prefix = "ACC")
    private String accountId;

    @Column(unique = true)
    private String loanRef;

    private String borrowerName;

    private String phone;

    private String address;

    private String branchId;

    private BigDecimal principalAmount;

    private BigDecimal totalOverdue;

    private Integer dpd;

    @Enumerated(EnumType.STRING)
    private DpdBucket bucket;

    /** True for a secured loan (backed by collateral); false for unsecured (credit card, personal loan). */
    @Builder.Default
    private boolean secured = false;

    @Builder.Default
    private Integer daysInCurrentBucket = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    /** Collections agent (auth-service userId) currently owning the account; null when unassigned. */
    private String assignedAgentId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
