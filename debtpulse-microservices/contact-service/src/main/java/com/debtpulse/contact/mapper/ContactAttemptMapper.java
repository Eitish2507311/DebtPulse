package com.debtpulse.contact.mapper;

import com.debtpulse.contact.dto.response.ContactAttemptDto;
import com.debtpulse.contact.entity.ContactAttempt;
import org.springframework.stereotype.Component;

/** Converts between {@link ContactAttempt} and {@link ContactAttemptDto}. */
@Component
public class ContactAttemptMapper {

    public ContactAttemptDto toDto(ContactAttempt e) {
        if (e == null) return null;
        return new ContactAttemptDto(
                e.getContactId(),
                e.getAccountId(),
                e.getAgentId(),
                e.getContactDate(),
                e.getChannel() == null ? null : e.getChannel().name(),
                e.getOutcome() == null ? null : e.getOutcome().name(),
                e.getNotes(),
                e.getStatus() == null ? null : e.getStatus().name(),
                e.getCreatedAt()
        );
    }
}
