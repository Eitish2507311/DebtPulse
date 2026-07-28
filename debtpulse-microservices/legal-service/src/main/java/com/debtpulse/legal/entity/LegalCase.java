package com.debtpulse.legal.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.CaseStatus;
import com.debtpulse.common.enums.CaseType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A legal proceeding initiated against a delinquent account (2.6 Legal Proceedings Management).
 *
 * <p>Owned by legal-service. {@code accountId} and {@code legalOfficerId} are plain-id string
 * references to entities owned by other services (account-service / auth-service).</p>
 */
@Entity
@Table(name = "legal_case")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LegalCase {

    @Id
    @BusinessId(prefix = "LEG")
    private String caseId;

    /** Cross-service reference to the delinquent account (account-service). */
    private String accountId;

    /** Cross-service reference to the owning legal officer (auth-service userId). */
    private String legalOfficerId;

    @Enumerated(EnumType.STRING)
    private CaseType caseType;

    private LocalDate filingDate;

    private String courtName;

    private String caseNumber;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CaseStatus status = CaseStatus.FILED;

    @CreatedDate
    private LocalDateTime createdAt;
}
