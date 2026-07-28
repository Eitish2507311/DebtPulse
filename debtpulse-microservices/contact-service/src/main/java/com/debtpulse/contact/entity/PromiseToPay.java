package com.debtpulse.contact.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.PtpStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A promise-to-pay (PTP) commitment made by a borrower (2.3 Contact &amp; Follow-Up Management).
 *
 * <p>{@code accountId} / {@code agentId} are plain string references. A daily scheduler marks
 * ACTIVE PTPs whose {@code commitmentDate} has lapsed as {@link PtpStatus#BROKEN}.</p>
 */
@Entity
@Table(name = "promise_to_pay")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PromiseToPay {

    @Id
    @BusinessId(prefix = "PTP")
    private String ptpId;

    private String accountId;

    private String agentId;

    private LocalDate ptpDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal ptpAmount;

    private LocalDate commitmentDate;

    /** Amount actually paid against the commitment; null until a payment is recorded. */
    @Column(precision = 15, scale = 2)
    private BigDecimal actualPaidAmount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PtpStatus status = PtpStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;
}
