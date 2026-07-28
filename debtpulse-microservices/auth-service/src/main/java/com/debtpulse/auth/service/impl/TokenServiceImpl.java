package com.debtpulse.auth.service.impl;

import com.debtpulse.auth.config.JwtTokenProvider;
import com.debtpulse.auth.entity.RefreshToken;
import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.exception.ResourceNotFoundException;
import com.debtpulse.auth.exception.UnauthorizedActionException;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.RefreshTokenStore;
import com.debtpulse.auth.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenStore store;
    private final JwtTokenProvider jwt;
    private final UserRepository userRepo;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;
    private final long idleTimeoutMs;

    public TokenServiceImpl(RefreshTokenStore store, JwtTokenProvider jwt, UserRepository userRepo,
                            @Value("${jwt.expiry-ms}") long accessExpiryMs,
                            @Value("${jwt.refresh-expiry-ms:604800000}") long refreshExpiryMs,
                            @Value("${session.idle-timeout-ms:1800000}") long idleTimeoutMs) {
        this.store = store;
        this.jwt = jwt;
        this.userRepo = userRepo;
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
        this.idleTimeoutMs = idleTimeoutMs;
    }

    @Override
    @Transactional
    public IssuedTokens issue(User user) {
        return issueForSession(user, UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public IssuedTokens refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken existing = store.findByHash(hash)
                .orElseThrow(() -> new UnauthorizedActionException("Invalid or expired refresh token"));

        // Reuse detection: a token that was already rotated (revoked) is being replayed → treat the
        // whole session as compromised and revoke every token for the user.
        if (existing.isRevoked()) {
            log.warn("Refresh-token reuse detected for user={} session={} — revoking all sessions",
                    existing.getUserId(), existing.getSessionId());
            store.revokeAllForUser(existing.getUserId());
            throw new UnauthorizedActionException("Refresh token has already been used");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing.getExpiresAt() == null || existing.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedActionException("Refresh token has expired");
        }
        // Sliding idle window: no activity for longer than the idle timeout ends the session.
        if (existing.getLastActivityAt() != null
                && existing.getLastActivityAt().plusNanos(idleTimeoutMs * 1_000_000).isBefore(now)) {
            store.revokeSession(existing.getSessionId());
            throw new UnauthorizedActionException("Session timed out due to inactivity");
        }

        User user = userRepo.findById(existing.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + existing.getUserId()));

        // Rotate: mint the new token in the same session, then revoke+link the old one.
        IssuedTokens issued = issueForSession(user, existing.getSessionId());
        existing.setRevoked(true);
        existing.setReplacedById(sha256(issued.refreshToken()));
        store.save(existing);
        return issued;
    }

    @Override
    @Transactional
    public String logout(String rawRefreshToken) {
        // Idempotent — an unknown/already-revoked token still returns success (userId null).
        return store.findByHash(sha256(rawRefreshToken)).map(rt -> {
            store.revokeSession(rt.getSessionId());
            log.info("Logout: session {} revoked for user {}", rt.getSessionId(), rt.getUserId());
            return rt.getUserId();
        }).orElse(null);
    }

    // ---- helpers ----

    private IssuedTokens issueForSession(User user, String sessionId) {
        String rawRefresh = newRawToken();
        LocalDateTime now = LocalDateTime.now();
        store.save(RefreshToken.builder()
                .userId(user.getUserId())
                .tokenHash(sha256(rawRefresh))
                .sessionId(sessionId)
                .issuedAt(now)
                .expiresAt(now.plusNanos(refreshExpiryMs * 1_000_000))
                .lastActivityAt(now)
                .revoked(false)
                .build());

        return new IssuedTokens(jwt.generateToken(user), rawRefresh, accessExpiryMs / 1000,
                user.getUserId(), user.getRole().name(), user.getFullName(), user.getBranchId());
    }

    /** 256 bits of entropy, URL-safe. High entropy means a plain SHA-256 hash at rest is sufficient. */
    private String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
