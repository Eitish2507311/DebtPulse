package com.debtpulse.auth.dto.request;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.validation.CorporateEmail;
import com.debtpulse.common.validation.Phone;
import com.debtpulse.common.validation.StrongPassword;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @CorporateEmail
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @NotNull(message = "Role is required")
        Role role,

        @NotBlank(message = "Phone is required")
        @Phone
        String phone,

        String branchId
) {}
