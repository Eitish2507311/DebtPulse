package com.debtpulse.legal.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.OrderStatus;
import com.debtpulse.common.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A court-issued recovery order for a {@link LegalCase} (2.6).
 *
 * <p>The {@code legalCase} back-reference is within this service so a real {@code @ManyToOne}
 * is used, but it is {@link JsonIgnore}d to avoid serialization recursion — callers see the
 * case via its id through the DTO layer.</p>
 */
@Entity
@Table(name = "recovery_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RecoveryOrder {

    @Id
    @BusinessId(prefix = "ORD")
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id")
    @JsonIgnore
    private LegalCase legalCase;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    private LocalDate issuedDate;

    private LocalDate executionDeadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.ISSUED;

    @CreatedDate
    private LocalDateTime createdAt;
}
