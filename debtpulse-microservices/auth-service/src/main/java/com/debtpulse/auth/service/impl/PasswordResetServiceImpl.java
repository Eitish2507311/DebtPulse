package com.debtpulse.auth.service.impl;

import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.PasswordResetService;
import com.debtpulse.auth.service.support.ForgotPasswordRateLimiter;
import com.debtpulse.common.audit.Auditable;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.exception.BusinessRuleException;
import com.debtpulse.auth.exception.RateLimitExceededException;
import com.debtpulse.auth.exception.ResourceNotFoundException;
import com.debtpulse.auth.exception.UnauthorizedActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final ForgotPasswordRateLimiter rateLimiter;

    /** DP5-39 — reset-token validity window, in minutes (configurable, default 15). */
    private final long tokenExpiryMinutes;

    public PasswordResetServiceImpl(UserRepository userRepo, PasswordEncoder encoder,
                                    ForgotPasswordRateLimiter rateLimiter,
                                    @Value("${auth.password-reset.token-expiry-minutes:15}") long tokenExpiryMinutes) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.rateLimiter = rateLimiter;
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    @Override
    // entityId is the attempted email (available on both outcomes). It is logged regardless of whether
    // the email exists, so it does NOT create an "email exists?" side channel in the audit trail.
    @Auditable(action = "PASSWORD_RESET_REQUEST", entity = "User", entityId = "#email")
    public Map<String, String> forgotPassword(String email) {
        // DP5-39: throttle by email BEFORE any lookup, so the limit can't be used to enumerate
        // accounts and a burst of requests can't be used to brute-force / spam a mailbox.
        if (!rateLimiter.tryAcquire(email)) {
            log.warn("Password reset rate limit exceeded for {}", email);
            throw new RateLimitExceededException(
                    "Too many password reset requests. Please try again later.", "RESET_RATE_LIMIT_EXCEEDED");
        }
        Map<String, String> res = new LinkedHashMap<>();
        userRepo.findByEmail(email).ifPresent(user -> {
            // Only ACTIVE accounts may reset. Inactive/suspended users are treated like a
            // non-existent account (no token issued) — and the generic message below still
            // avoids leaking which emails exist or which are locked.
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Password reset requested for non-active account {} (status={}) — ignored",
                        email, user.getStatus());
                return;
            }
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
            userRepo.save(user);
            // DEV-ONLY: the reset token is returned directly in the response (no out-of-band delivery).
            // WARNING: this is an account-takeover path — anyone who can call this endpoint gets the
            // token to reset that account's password. Acceptable ONLY for a local/dev build.
            res.put("resetToken", token);
            log.info("Password reset token issued for {}", email);
        });
        // Always the same message to avoid leaking which emails exist.
        res.put("message", "If the email exists, a reset token has been issued (valid "
                + tokenExpiryMinutes + " minutes).");
        return res;
    }

    @Override
    @Auditable(action = "PASSWORD_RESET", entity = "User")
    public Map<String, String> resetPassword(String token, String newPassword) {
        User user = userRepo.findByResetToken(token)
                .orElseThrow(() -> new BusinessRuleException("Invalid reset token", "INVALID_TOKEN"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Reset token has expired", "TOKEN_EXPIRED");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active", "ACCOUNT_NOT_ACTIVE");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepo.save(user);
        log.info("Password reset completed for {}", user.getEmail());
        return Map.of("message", "Password has been reset successfully");
    }

    @Override
    @Auditable(action = "PASSWORD_CHANGE", entity = "User", entityId = "#userId")
    public Map<String, String> changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedActionException("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(user);
        log.info("Password changed for {}", user.getEmail());
        return Map.of("message", "Password changed successfully");
    }
}
