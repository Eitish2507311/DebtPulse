package com.debtpulse.auth.service.impl;

import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.feign.NotificationClient;
import com.debtpulse.auth.feign.dto.NotificationRequest;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.PasswordResetService;
import com.debtpulse.auth.service.support.ForgotPasswordRateLimiter;
import com.debtpulse.common.audit.Auditable;
import com.debtpulse.common.enums.NotifCategory;
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
    private final NotificationClient notificationClient;

    /** DP5-39 — reset-token validity window, in minutes (configurable, default 15). */
    private final long tokenExpiryMinutes;

    public PasswordResetServiceImpl(UserRepository userRepo, PasswordEncoder encoder,
                                    ForgotPasswordRateLimiter rateLimiter,
                                    NotificationClient notificationClient,
                                    @Value("${auth.password-reset.token-expiry-minutes:15}") long tokenExpiryMinutes) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.rateLimiter = rateLimiter;
        this.notificationClient = notificationClient;
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    @Override
    @Auditable(action = "PASSWORD_RESET_REQUEST", entity = "User")
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
            // Deliver the token strictly out-of-band (never in the API response). Best-effort:
            // a delivery failure must not change the caller-visible response, so it can't be used
            // to probe whether the email exists.
            deliverResetToken(user, token);
            log.info("Password reset token issued for {}", email);
        });
        // Always the same generic message, whether or not the email matched an ACTIVE account.
        res.put("message", "If the email exists, a password reset link has been sent.");
        return res;
    }

    /** Sends the reset token to the user via notification-service ({@code SECURITY} category). */
    private void deliverResetToken(User user, String token) {
        try {
            String message = "Your DebtPulse password reset token is: " + token
                    + " (valid for " + tokenExpiryMinutes + " minutes). "
                    + "If you did not request this, please ignore this message.";
            notificationClient.notify(new NotificationRequest(
                    user.getUserId(), message, NotifCategory.SECURITY.name()));
        } catch (Exception e) {
            // Swallow — delivery is best-effort and must never leak email existence or fail the request.
            log.warn("Failed to dispatch password-reset notification for user {}: {}",
                    user.getUserId(), e.getMessage());
        }
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
