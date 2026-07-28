package com.debtpulse.contact.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.ContactChannel;
import com.debtpulse.common.enums.ContactOutcome;
import com.debtpulse.common.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A single borrower contact attempt (2.3 Contact &amp; Follow-Up Management).
 *
 * <p>{@code accountId} and {@code agentId} are plain string references to records owned by
 * account-service and auth-service respectively — never JPA relations.</p>
 */
@Entity
@Table(name = "contact_attempt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContactAttempt {

    @Id
    @BusinessId(prefix = "CON")
    private String contactId;

    private String accountId;

    private String agentId;

    private LocalDateTime contactDate;

    @Enumerated(EnumType.STRING)
    private ContactChannel channel;

    @Enumerated(EnumType.STRING)
    private ContactOutcome outcome;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContactStatus status = ContactStatus.LOGGED;

    @CreatedDate
    private LocalDateTime createdAt;
}
