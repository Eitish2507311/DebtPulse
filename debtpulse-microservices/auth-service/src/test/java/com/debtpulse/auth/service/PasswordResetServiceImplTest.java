package com.debtpulse.auth.service;

import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.feign.NotificationClient;
import com.debtpulse.auth.feign.dto.NotificationRequest;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.impl.PasswordResetServiceImpl;
import com.debtpulse.auth.service.support.ForgotPasswordRateLimiter;
import com.debtpulse.common.enums.NotifCategory;
import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private ForgotPasswordRateLimiter rateLimiter;
    @Mock private NotificationClient notificationClient;

    private PasswordResetServiceImpl service;

    private User active;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(userRepo, encoder, rateLimiter, notificationClient, 15L);
        active = User.builder()
                .userId("USR-002").fullName("Agent").email("agent@dp.com")
                .passwordHash("hashed").role(Role.COLLECTIONS_AGENT)
                .branchId("B01").status(UserStatus.ACTIVE).build();
    }

    @Test
    void forgotPassword_neverReturnsTheTokenInTheResponse_andSendsItOutOfBand() {
        when(rateLimiter.tryAcquire("agent@dp.com")).thenReturn(true);
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));

        Map<String, String> res = service.forgotPassword("agent@dp.com");

        // The token must NEVER appear in the HTTP response.
        assertThat(res).doesNotContainKey("resetToken");
        assertThat(res.values()).noneMatch(v -> v != null && v.equals(active.getResetToken()));
        assertThat(res.get("message")).isEqualTo("If the email exists, a password reset link has been sent.");

        // It is delivered out-of-band to the right user under the SECURITY category, carrying the token.
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).notify(captor.capture());
        NotificationRequest sent = captor.getValue();
        assertThat(sent.userId()).isEqualTo("USR-002");
        assertThat(sent.category()).isEqualTo(NotifCategory.SECURITY.name());
        assertThat(sent.message()).contains(active.getResetToken());
        assertThat(active.getResetToken()).isNotBlank();
    }

    @Test
    void forgotPassword_unknownEmail_isGenericAndSendsNothing() {
        when(rateLimiter.tryAcquire("nobody@dp.com")).thenReturn(true);
        when(userRepo.findByEmail("nobody@dp.com")).thenReturn(Optional.empty());

        Map<String, String> res = service.forgotPassword("nobody@dp.com");

        assertThat(res).doesNotContainKey("resetToken");
        assertThat(res.get("message")).isEqualTo("If the email exists, a password reset link has been sent.");
        verify(notificationClient, never()).notify(any());
    }
}
