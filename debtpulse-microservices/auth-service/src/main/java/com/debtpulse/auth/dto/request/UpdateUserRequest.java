package com.debtpulse.auth.dto.request;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.validation.CorporateEmail;
import com.debtpulse.common.validation.Phone;
import jakarta.validation.constraints.Size;

/** Partial update for a user (admin only). Null fields are left unchanged. */
public record UpdateUserRequest(
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @CorporateEmail
        String email,

        @Phone
        String phone,

        Role role,

        String branchId
) {}
