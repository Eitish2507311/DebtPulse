package com.debtpulse.legal.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.HearingOutcome;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A scheduled/recorded court hearing for a {@link LegalCase} (2.6).
 *
 * <p>The {@code legalCase} back-reference is within this service so a real {@code @ManyToOne}
 * is used, but it is {@link JsonIgnore}d to avoid serialization recursion — callers see the
 * case via its id through the DTO layer.</p>
 */
@Entity
@Table(name = "court_hearing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CourtHearing {

    @Id
    @BusinessId(prefix = "HRG")
    private String hearingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id")
    @JsonIgnore
    private LegalCase legalCase;

    private LocalDate hearingDate;

    @Enumerated(EnumType.STRING)
    private HearingOutcome hearingOutcome;

    private LocalDate nextHearingDate;

    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;
}
