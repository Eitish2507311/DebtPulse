package com.debtpulse.auth.dto.request;

import com.debtpulse.common.validation.CorporateEmail;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @CorporateEmail
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
