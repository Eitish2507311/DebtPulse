package com.debtpulse.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Payload for POST /api/auth/refresh and /api/auth/logout — carries the refresh token. */
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
