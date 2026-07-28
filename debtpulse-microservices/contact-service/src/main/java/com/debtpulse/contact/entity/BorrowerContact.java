package com.debtpulse.contact.entity;

import com.debtpulse.common.id.BusinessId;

import com.debtpulse.common.enums.BorrowerContactStatus;
import com.debtpulse.common.enums.BorrowerContactType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A stored contact channel for a borrower — a phone number / person associated with an
 * account (2.3 Contact &amp; Follow-Up Management). {@code accountId} is a plain string
 * reference to the account owned by account-service.
 */
@Entity
@Table(name = "borrower_contact")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BorrowerContact {

    @Id
    @BusinessId(prefix = "BOR")
    private String contactRecordId;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private BorrowerContactType contactType;

    private String name;

    private String phone;

    private String relationship;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BorrowerContactStatus status = BorrowerContactStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;
}
