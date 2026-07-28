package com.debtpulse.auth.service.impl;

import com.debtpulse.common.audit.Auditable;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.response.AuthResponse;
import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.AuthService;
import com.debtpulse.auth.service.TokenService;
import com.debtpulse.auth.service.TokenService.IssuedTokens;
import com.debtpulse.auth.exception.BusinessRuleException;
import com.debtpulse.auth.exception.UnauthorizedActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepo;
    private final TokenService tokenService;
    private final PasswordEncoder encoder;

    /** Failed-login attempts before the account is temporarily locked (DP5-10). Configurable. */
    private final int maxFailedAttempts;
    /** How long an account stays locked once the threshold is hit. Configurable. */
    private final long lockDurationMs;

    public AuthServiceImpl(UserRepository userRepo, TokenService tokenService,
                           PasswordEncoder encoder,
                           @Value("${auth.lockout.max-attempts:5}") int maxFailedAttempts,
                           @Value("${auth.lockout.lock-duration-ms:900000}") long lockDurationMs) {
        this.userRepo = userRepo;
        this.tokenService = tokenService;
        this.encoder = encoder;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMs = lockDurationMs;
    }

    @Override
    @Auditable(action = "LOGIN", entity = "User", entityId = "#result.userId()")
    public AuthResponse login(String email, String password) {
        log.info("Login attempt for: {}", email);

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedActionException("Invalid credentials"));

        // DP5-10: reject while a temporary lock is in effect (fixed window, auto-unlocks).
        if (isLocked(user)) {
            log.warn("Login blocked — account {} is locked until {}", email, user.getLockedUntil());
            throw new UnauthorizedActionException(
                    "Account temporarily locked due to repeated failed logins. Try again later.");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new UnauthorizedActionException("Invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedActionException("Account is " + user.getStatus());
        }

        resetLockState(user);
        log.info("Login success for {} [{}]", email, user.getRole());
        return toResponse("Login successful", tokenService.issue(user));
    }

    /** True while the account carries an unexpired lock window. */
    private boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Records a failed login. Once {@code maxFailedAttempts} is reached the account is locked for
     * {@code lockDurationMs} and the counter resets, so the next window starts clean after unlock.
     */
    private void registerFailedAttempt(User user) {
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plus(Duration.ofMillis(lockDurationMs)));
            user.setFailedLoginAttempts(0);
            log.warn("Account {} locked after {} failed attempts (until {})",
                    user.getEmail(), attempts, user.getLockedUntil());
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepo.save(user);
    }

    /** Clears any residual counter / expired lock on a successful login. */
    private void resetLockState(User user) {
        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0)
                || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepo.save(user);
        }
    }

    @Override
    @Auditable(action = "TOKEN_REFRESH", entity = "User", entityId = "#result.userId()")
    public AuthResponse refresh(String refreshToken) {
        return toResponse("Token refreshed", tokenService.refresh(refreshToken));
    }

    @Override
    @Auditable(action = "LOGOUT", entity = "User")
    public void logout(String refreshToken) {
        tokenService.logout(refreshToken);
    }

    private AuthResponse toResponse(String message, IssuedTokens t) {
        return new AuthResponse(message, t.accessToken(), t.refreshToken(), t.expiresInSeconds(),
                t.userId(), t.role(), t.name(), t.branchId());
    }

    @Override
    public AuthResponse register(RegisterRequest req) {
        log.info("Register attempt for: {}", req.email());

        if (userRepo.existsByEmail(req.email())) {
            throw new BusinessRuleException("Email already registered: " + req.email(), "DUPLICATE_EMAIL");
        }

        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .role(req.role())
                .branchId(req.branchId() != null ? req.branchId() : "B01")
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepo.save(user);
        log.info("User registered: {} [{}] id={}", saved.getEmail(), saved.getRole(), saved.getUserId());

        return toResponse("User registered successfully", tokenService.issue(saved));
    }
}
