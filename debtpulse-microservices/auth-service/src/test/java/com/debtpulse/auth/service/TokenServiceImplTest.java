package com.debtpulse.auth.service;

import com.debtpulse.auth.config.JwtTokenProvider;
import com.debtpulse.auth.entity.RefreshToken;
import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.exception.UnauthorizedActionException;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.TokenService.IssuedTokens;
import com.debtpulse.auth.service.impl.TokenServiceImpl;
import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock private RefreshTokenStore store;
    @Mock private JwtTokenProvider jwt;
    @Mock private UserRepository userRepo;

    private TokenServiceImpl service;
    private User user;

    private static final long ACCESS = 10_800_000L;   // 3h
    private static final long REFRESH = 604_800_000L;  // 7d
    private static final long IDLE = 1_800_000L;       // 30m

    @BeforeEach
    void setUp() {
        service = new TokenServiceImpl(store, jwt, userRepo, ACCESS, REFRESH, IDLE);
        user = User.builder().userId("USR-1").fullName("A").email("a@dp.com")
                .role(Role.COLLECTIONS_AGENT).branchId("B01").status(UserStatus.ACTIVE).build();
    }

    @Test
    void issue_persistsHashedTokenAndReturnsPair() {
        when(jwt.generateToken(user)).thenReturn("access-jwt");
        when(store.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        IssuedTokens t = service.issue(user);

        assertThat(t.accessToken()).isEqualTo("access-jwt");
        assertThat(t.refreshToken()).isNotBlank();
        assertThat(t.expiresInSeconds()).isEqualTo(ACCESS / 1000);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(store).save(captor.capture());
        RefreshToken saved = captor.getValue();
        // The raw token is never stored — only a 64-char SHA-256 hex hash.
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(t.refreshToken());
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void refresh_rotates_revokingOldAndIssuingNew() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = RefreshToken.builder()
                .id("RT-1").userId("USR-1").sessionId("S-1").revoked(false)
                .issuedAt(now.minusMinutes(5)).lastActivityAt(now.minusMinutes(5))
                .expiresAt(now.plusDays(7)).tokenHash("hash").build();
        when(store.findByHash(any())).thenReturn(Optional.of(current));
        when(userRepo.findById("USR-1")).thenReturn(Optional.of(user));
        when(jwt.generateToken(user)).thenReturn("access-2");
        when(store.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        IssuedTokens t = service.refresh("any-raw");

        assertThat(t.accessToken()).isEqualTo("access-2");
        assertThat(current.isRevoked()).isTrue();               // old rotated out
        assertThat(current.getReplacedById()).isNotNull();      // lineage linked
        verify(store, atLeast(2)).save(any(RefreshToken.class)); // new + updated old
    }

    @Test
    void refresh_reusedRevokedToken_revokesAllAndThrows() {
        RefreshToken revoked = RefreshToken.builder()
                .id("RT-0").userId("USR-1").sessionId("S-1").revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(1)).tokenHash("hash").build();
        when(store.findByHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("raw"))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(store).revokeAllForUser("USR-1");
    }

    @Test
    void refresh_expiredToken_throws() {
        RefreshToken expired = RefreshToken.builder()
                .id("RT-2").userId("USR-1").sessionId("S-1").revoked(false)
                .lastActivityAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().minusSeconds(1))
                .tokenHash("hash").build();
        when(store.findByHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("raw"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void refresh_idleTimeout_revokesSessionAndThrows() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken idle = RefreshToken.builder()
                .id("RT-3").userId("USR-1").sessionId("S-1").revoked(false)
                .lastActivityAt(now.minusMinutes(31))     // > 30m idle
                .expiresAt(now.plusDays(7)).tokenHash("hash").build();
        when(store.findByHash(any())).thenReturn(Optional.of(idle));

        assertThatThrownBy(() -> service.refresh("raw"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("inactivity");
        verify(store).revokeSession("S-1");
    }

    @Test
    void refresh_unknownToken_throws() {
        when(store.findByHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("raw"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void logout_revokesSession() {
        RefreshToken rt = RefreshToken.builder().id("RT-9").userId("USR-1").sessionId("S-9")
                .revoked(false).tokenHash("hash").build();
        when(store.findByHash(any())).thenReturn(Optional.of(rt));

        service.logout("raw");

        verify(store).revokeSession("S-9");
    }

    @Test
    void logout_unknownToken_isIdempotent() {
        when(store.findByHash(any())).thenReturn(Optional.empty());
        service.logout("raw"); // no throw
        verify(store, never()).revokeSession(any());
    }
}
