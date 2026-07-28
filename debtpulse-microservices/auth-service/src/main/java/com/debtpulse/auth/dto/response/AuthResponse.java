package com.debtpulse.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Login / refresh / registration result.
 *
 * <p>{@code token} is the short-lived access JWT (kept under this name for backward compatibility).
 * {@code refreshToken} is the long-lived, rotating credential used against {@code /api/auth/refresh};
 * {@code expiresIn} is the access-token lifetime in seconds.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String message,
        String token,
        String refreshToken,
        Long expiresIn,
        String userId,
        String role,
        String name,
        String branchId
) {}
