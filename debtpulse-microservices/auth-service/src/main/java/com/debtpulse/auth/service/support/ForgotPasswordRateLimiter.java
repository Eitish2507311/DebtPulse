package com.debtpulse.auth.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for forgot-password requests (DP5-39): at most
 * {@code auth.password-reset.max-requests-per-hour} attempts per email address within any
 * rolling one-hour window.
 *
 * <p>State is held in-process, which is correct for a single auth-service instance. When the
 * platform scales auth-service horizontally this becomes a natural seam to back with Redis
 * (mirrors the {@code RefreshTokenStore} port pattern) — the public {@link #tryAcquire(String)}
 * contract stays the same. Keys are normalised (trim + lower-case) so casing can't be used to
 * bypass the limit.</p>
 */
@Component
public class ForgotPasswordRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final int maxPerWindow;
    private final Map<String, Deque<LocalDateTime>> hits = new ConcurrentHashMap<>();

    public ForgotPasswordRateLimiter(
            @Value("${auth.password-reset.max-requests-per-hour:5}") int maxPerWindow) {
        this.maxPerWindow = maxPerWindow;
    }

    /**
     * Registers an attempt for {@code email} and reports whether it is within the allowed rate.
     *
     * @return {@code true} if the request is permitted; {@code false} if the limit is exceeded.
     */
    public boolean tryAcquire(String email) {
        if (email == null || email.isBlank()) {
            return true; // let downstream validation reject blank input; don't limit on it
        }
        String key = email.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(WINDOW);

        Deque<LocalDateTime> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }
            if (window.size() >= maxPerWindow) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }
}
