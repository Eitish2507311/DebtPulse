package com.debtpulse.contact.mapper;

import com.debtpulse.contact.dto.response.BorrowerContactDto;
import com.debtpulse.contact.entity.BorrowerContact;
import org.springframework.stereotype.Component;

/** Converts between {@link BorrowerContact} and {@link BorrowerContactDto}. */
@Component
public class BorrowerContactMapper {

    public BorrowerContactDto toDto(BorrowerContact e) {
        if (e == null) return null;
        return new BorrowerContactDto(
                e.getContactRecordId(),
                e.getAccountId(),
                e.getContactType() == null ? null : e.getContactType().name(),
                e.getName(),
                e.getPhone(),
                e.getRelationship(),
                e.getStatus() == null ? null : e.getStatus().name(),
                e.getCreatedAt()
        );
    }
}
