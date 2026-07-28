package com.debtpulse.auth.dto.request;

import com.debtpulse.common.validation.CorporateEmail;
import com.debtpulse.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/** Container for the three password-flow request payloads. */
public final class PasswordRequests {

    private PasswordRequests() {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required")
            @CorporateEmail
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Reset token is required")
            String token,

            @NotBlank(message = "New password is required")
            @StrongPassword
            String newPassword
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @StrongPassword
            String newPassword
    ) {}
}
