package com.debtpulse.auth.service;

import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.repository.UserRepository;
import com.debtpulse.auth.service.impl.PasswordResetServiceImpl;
import com.debtpulse.auth.service.support.ForgotPasswordRateLimiter;
import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private ForgotPasswordRateLimiter rateLimiter;

    private PasswordResetServiceImpl service;

    private User active;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(userRepo, encoder, rateLimiter, 15L);
        active = User.builder()
                .userId("USR-002").fullName("Agent").email("agent@dp.com")
                .passwordHash("hashed").role(Role.COLLECTIONS_AGENT)
                .branchId("B01").status(UserStatus.ACTIVE).build();
    }

    @Test
    void forgotPassword_activeUser_returnsTokenInResponse() {
        // DEV-ONLY behaviour: the reset token is returned directly in the response body.
        when(rateLimiter.tryAcquire("agent@dp.com")).thenReturn(true);
        when(userRepo.findByEmail("agent@dp.com")).thenReturn(Optional.of(active));

        Map<String, String> res = service.forgotPassword("agent@dp.com");

        assertThat(res).containsKey("resetToken");
        assertThat(res.get("resetToken")).isNotBlank().isEqualTo(active.getResetToken());
        assertThat(res.get("message")).contains("reset token has been issued");
    }

    @Test
    void forgotPassword_unknownEmail_isGenericWithNoToken() {
        when(rateLimiter.tryAcquire("nobody@dp.com")).thenReturn(true);
        when(userRepo.findByEmail("nobody@dp.com")).thenReturn(Optional.empty());

        Map<String, String> res = service.forgotPassword("nobody@dp.com");

        assertThat(res).doesNotContainKey("resetToken");
        assertThat(res.get("message")).contains("If the email exists");
    }
}
