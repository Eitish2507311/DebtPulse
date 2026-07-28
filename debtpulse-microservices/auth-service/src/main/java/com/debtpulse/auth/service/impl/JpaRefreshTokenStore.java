package com.debtpulse.auth.service.impl;

import com.debtpulse.auth.entity.RefreshToken;
import com.debtpulse.auth.repository.RefreshTokenRepository;
import com.debtpulse.auth.service.RefreshTokenStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Default DB-backed {@link RefreshTokenStore} adapter. Swap for a Redis adapter via config later. */
@Component
public class JpaRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository repo;

    public JpaRefreshTokenStore(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return repo.save(token);
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return repo.findByTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId) {
        List<RefreshToken> tokens = repo.findBySessionId(sessionId);
        tokens.forEach(t -> t.setRevoked(true));
        repo.saveAll(tokens);
    }

    @Override
    @Transactional
    public void revokeAllForUser(String userId) {
        List<RefreshToken> tokens = repo.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(t -> t.setRevoked(true));
        repo.saveAll(tokens);
    }
}
