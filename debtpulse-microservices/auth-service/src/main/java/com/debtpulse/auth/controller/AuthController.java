package com.debtpulse.auth.controller;

import com.debtpulse.auth.dto.request.LoginRequest;
import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.response.AuthResponse;
import com.debtpulse.auth.exception.UnauthorizedActionException;
import com.debtpulse.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and admin-driven registration")
public class AuthController {

    /**
     * The refresh token is delivered as an httpOnly, Secure, SameSite=Strict cookie — never in the
     * response body — so an XSS payload can read (at most) the short-lived access token, never the
     * long-lived refresh token. Path is scoped to {@code /api/auth} so it is only ever sent to the
     * auth endpoints. (localhost is a secure context, so Secure cookies work in dev too.)
     */
    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final long refreshExpiryMs;

    public AuthController(AuthService authService,
                          @Value("${jwt.refresh-expiry-ms:604800000}") long refreshExpiryMs) {
        this.authService = authService;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    @PostMapping("/login")
    @Operation(summary = "Login — returns an access token (3h) in the body and sets the refresh token "
            + "as an httpOnly cookie; expiresIn is in seconds")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req.email(), req.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(res.refreshToken(), refreshExpiryMs / 1000).toString())
                .body(withoutRefreshToken(res));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the httpOnly refresh-token cookie for a new access token; "
            + "rotates the refresh cookie")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedActionException("Missing refresh token");
        }
        AuthResponse res = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(res.refreshToken(), refreshExpiryMs / 1000).toString())
                .body(withoutRefreshToken(res));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the session (refresh-token cookie) and clear it — idempotent")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(Map.of("message", "Logged out"));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new user (ADMIN only) — password is BCrypt encoded")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    // ---- refresh-token cookie helpers ----

    private ResponseCookie refreshCookie(String token, long maxAgeSeconds) {
        return baseCookie(token).maxAge(Duration.ofSeconds(maxAgeSeconds)).build();
    }

    private ResponseCookie clearRefreshCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }

    /** The refresh token lives only in the cookie — strip it from the JSON body (omitted via NON_NULL). */
    private AuthResponse withoutRefreshToken(AuthResponse r) {
        return new AuthResponse(r.message(), r.token(), null, r.expiresIn(),
                r.userId(), r.role(), r.name(), r.branchId());
    }
}
