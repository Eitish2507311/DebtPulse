package com.debtpulse.field.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A scheduled/completed on-site visit to a delinquent borrower (2.4 Field Recovery Management).
 *
 * <p>Owned by field-service. {@code accountId} (account-service) and {@code officerId}
 * (auth-service) are stored as plain id strings — never {@code @ManyToOne} to another
 * service's entity.</p>
 */
@Entity
@Table(name = "field_visit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FieldVisit {

    @Id
    @BusinessId(prefix = "VIS")
    private String visitId;

    /** The delinquent account being visited (account-service reference). */
    private String accountId;

    /** The field officer assigned to the visit (auth-service user reference). */
    private String officerId;

    private LocalDate scheduledDate;

    /** Actual date the visit took place; null until completed. */
    private LocalDate visitDate;

    private Boolean borrowerMet;

    private Boolean assetSighted;

    @Column(length = 1000)
    private String outcomeSummary;

    private String nextActionRequired;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VisitStatus status = VisitStatus.SCHEDULED;

    @CreatedDate
    private LocalDateTime createdAt;
}
