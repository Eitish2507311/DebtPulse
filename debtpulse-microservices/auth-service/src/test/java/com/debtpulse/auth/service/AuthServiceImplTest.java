package com.debtpulse.auth.service;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.dto.request.RegisterRequest;
import com.debtpulse.auth.dto.response.AuthResponse;
import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.TokenService.IssuedTokens;
import com.debtpulse.auth.service.impl.AuthServiceImpl;
import com.debtpulse.auth.exception.BusinessRuleException;
import com.debtpulse.auth.exception.UnauthorizedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 900_000L;

    @Mock private UserRepository userRepo;
    @Mock private TokenService tokenService;
    @Mock private PasswordEncoder encoder;

    private AuthServiceImpl authService;

    private User active;

    @BeforeEach
    void setUp() {
        // Explicit construction: AuthServiceImpl now takes lockout config (int/long) that @InjectMocks
        // cannot supply. Wire the collaborators and pass the DP5-10 lockout thresholds directly.
        authService = new AuthServiceImpl(userRepo, tokenService, encoder, MAX_ATTEMPTS, LOCK_MS);
        active = User.builder()
                .userId("USR-002").fullName("Agent").email("agent@dp.com")
                .passwordHash("hashed").role(Role.COLLECTIONS_AGENT)
                .branchId("B01").status(UserStatus.ACTIVE).build();
    }

    @Test
    void login_success_returnsAccessAndRefreshTokens() {
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));
        when(encoder.matches("password", "hashed")).thenReturn(true);
        when(tokenService.issue(active)).thenReturn(new IssuedTokens(
                "jwt-token", "refresh-1", 10800, "USR-002", "COLLECTIONS_AGENT", "Agent", "B01"));

        AuthResponse res = authService.login("agent@dp.com", "password");

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.refreshToken()).isEqualTo("refresh-1");
        assertThat(res.expiresIn()).isEqualTo(10800);
        assertThat(res.role()).isEqualTo("COLLECTIONS_AGENT");
        assertThat(res.userId()).isEqualTo("USR-002");
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));
        when(encoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("agent@dp.com", "bad"))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_inactiveAccount_throws() {
        active.setStatus(UserStatus.SUSPENDED);
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));
        when(encoder.matches("password", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("agent@dp.com", "password"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    void login_wrongPassword_incrementsFailedAttempts() {
        active.setFailedLoginAttempts(0);
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));
        when(encoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("agent@dp.com", "bad"))
                .isInstanceOf(UnauthorizedActionException.class);

        assertThat(active.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(active.getLockedUntil()).isNull();
        verify(userRepo).save(active);
    }

    @Test
    void login_locksAccountOnReachingMaxAttempts() {
        active.setFailedLoginAttempts(MAX_ATTEMPTS - 1);   // next failure trips the lock
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));
        when(encoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("agent@dp.com", "bad"))
                .isInstanceOf(UnauthorizedActionException.class);

        assertThat(active.getLockedUntil()).isAfter(LocalDateTime.now());
        assertThat(active.getFailedLoginAttempts()).isZero();   // counter resets once locked
        verify(userRepo).save(active);
        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_whileLocked_isRejectedWithoutCheckingPassword() {
        active.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> authService.login("agent@dp.com", "password"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("locked");

        verify(encoder, never()).matches(any(), any());
        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_success_clearsResidualFailedAttempts() {
        active.setFailedLoginAttempts(3);
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));
        when(encoder.matches("password", "hashed")).thenReturn(true);
        when(tokenService.issue(active)).thenReturn(new IssuedTokens(
                "jwt-token", "refresh-1", 10800, "USR-002", "COLLECTIONS_AGENT", "Agent", "B01"));

        authService.login("agent@dp.com", "password");

        assertThat(active.getFailedLoginAttempts()).isZero();
        assertThat(active.getLockedUntil()).isNull();
        verify(userRepo).save(active);
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepo.findByEmail("nobody@dp.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login("nobody@dp.com", "x"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void refresh_delegatesToTokenService() {
        when(tokenService.refresh("refresh-1")).thenReturn(new IssuedTokens(
                "jwt-2", "refresh-2", 10800, "USR-002", "COLLECTIONS_AGENT", "Agent", "B01"));

        AuthResponse res = authService.refresh("refresh-1");

        assertThat(res.token()).isEqualTo("jwt-2");
        assertThat(res.refreshToken()).isEqualTo("refresh-2");
    }

    @Test
    void logout_delegatesToTokenService() {
        authService.logout("refresh-1");
        verify(tokenService).logout("refresh-1");
    }

    @Test
    void register_success_savesAndReturnsTokens() {
        RegisterRequest req = new RegisterRequest("New User", "new@dp.com", "Secret@123",
                Role.FIELD_OFFICER, "9876543210", "B02");
        when(userRepo.existsByEmail("new@dp.com")).thenReturn(false);
        when(encoder.encode("Secret@123")).thenReturn("enc");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId("USR-2026-000010");
            return u;
        });
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedTokens(
                "new-token", "refresh-3", 10800, "USR-2026-000010", "FIELD_OFFICER", "New User", "B02"));

        AuthResponse res = authService.register(req);

        assertThat(res.userId()).isEqualTo("USR-2026-000010");
        assertThat(res.token()).isEqualTo("new-token");
        assertThat(res.refreshToken()).isEqualTo("refresh-3");
        verify(userRepo).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest("Dup", "agent@dp.com", "Secret@123",
                Role.FIELD_OFFICER, "9876543210", "B01");
        when(userRepo.existsByEmail("agent@dp.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepo, never()).save(any());
    }
}
