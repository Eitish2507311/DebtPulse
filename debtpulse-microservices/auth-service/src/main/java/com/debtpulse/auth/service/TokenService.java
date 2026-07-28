package com.debtpulse.auth.service;

import com.debtpulse.auth.entity.User;

/**
 * Issues and manages the access + refresh token pair and the server-side session.
 *
 * <p>Access token: short-lived stateless JWT. Refresh token: long-lived, hashed-at-rest, single-use
 * (rotates on every refresh). Idle sessions are enforced server-side via a sliding window on the
 * refresh record's last-activity timestamp.</p>
 */
public interface TokenService {

    /** Carrier for a freshly issued pair plus the identity needed to build an AuthResponse. */
    record IssuedTokens(String accessToken, String refreshToken, long expiresInSeconds,
                        String userId, String role, String name, String branchId) {}

    /** Start a new session for {@code user} and issue the first token pair. */
    IssuedTokens issue(User user);

    /** Validate + rotate a refresh token, returning a new pair (old token is revoked). */
    IssuedTokens refresh(String rawRefreshToken);

    /** End the session the refresh token belongs to (idempotent). */
    void logout(String rawRefreshToken);
}
