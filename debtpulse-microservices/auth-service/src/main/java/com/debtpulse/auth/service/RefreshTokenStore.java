package com.debtpulse.auth.service;

import com.debtpulse.auth.entity.RefreshToken;

import java.util.Optional;

/**
 * Port for refresh-token persistence (hexagonal / dependency-inversion). The application
 * (TokenService) depends only on this abstraction, so the backing technology can change without
 * touching business logic. A DB-backed {@code JpaRefreshTokenStore} is the current adapter; a
 * Redis adapter can be dropped in later by implementing this same interface and selecting it via config.
 */
public interface RefreshTokenStore {

    RefreshToken save(RefreshToken token);

    /** Looks up a token by its SHA-256 hash, whether or not it is revoked (needed for reuse detection). */
    Optional<RefreshToken> findByHash(String tokenHash);

    /** Revoke every token in a login session (used by logout). */
    void revokeSession(String sessionId);

    /** Revoke every active token for a user (used on suspected token reuse / global logout). */
    void revokeAllForUser(String userId);
}
