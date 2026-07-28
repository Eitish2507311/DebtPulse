package com.debtpulse.contact.dto.request;

import com.debtpulse.common.enums.BorrowerContactStatus;
import com.debtpulse.common.enums.BorrowerContactType;
import com.debtpulse.common.validation.Phone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload to create/update a borrower contact record. */
public record BorrowerContactRequest(
        @NotBlank(message = "Account id is required")
        String accountId,

        @NotNull(message = "Contact type is required")
        BorrowerContactType contactType,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Phone is required")
        @Phone
        String phone,

        String relationship,

        BorrowerContactStatus status
) {}
