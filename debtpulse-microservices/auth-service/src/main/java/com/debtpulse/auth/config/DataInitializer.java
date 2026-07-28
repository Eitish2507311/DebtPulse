package com.debtpulse.auth.config;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.auth.entity.User;
import com.debtpulse.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the nine demo users (one per role) on startup if they do not already exist,
 * each with the password {@code "password"} BCrypt-encoded by the live encoder — so
 * credentials are always valid regardless of environment. Mirrors the monolith's
 * seed accounts (admin@dp.com / password, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    private record Seed(String name, String email, Role role) {}

    @Override
    public void run(ApplicationArguments args) {
        // Ids are NOT hard-coded — each seed flows through the @BusinessId generator and receives a
        // USR-YYYY-NNNNNN id in declaration order (admin first). Login is by email, so the exact id
        // is irrelevant to callers.
        List<Seed> seeds = List.of(
                new Seed("System Admin",      "admin@dp.com", Role.ADMIN),
                new Seed("Collections Agent", "agent@dp.com", Role.COLLECTIONS_AGENT),
                new Seed("Field Officer",     "field@dp.com", Role.FIELD_OFFICER),
                new Seed("Legal Officer",     "legal@dp.com", Role.LEGAL_OFFICER),
                new Seed("Settlement Officer","so@dp.com",    Role.SETTLEMENT_OFFICER),
                new Seed("L1 Approver",       "l1@dp.com",    Role.L1_APPROVER),
                new Seed("L2 Approver",       "l2@dp.com",    Role.L2_APPROVER),
                new Seed("L3 Approver",       "l3@dp.com",    Role.L3_APPROVER),
                new Seed("Portfolio Manager", "pm@dp.com",    Role.PORTFOLIO_MANAGER)
        );

        int created = 0;
        for (Seed s : seeds) {
            if (!userRepository.existsByEmail(s.email())) {
                userRepository.save(User.builder()
                        .fullName(s.name())
                        .email(s.email())
                        .phone("9000000000")
                        .passwordHash(encoder.encode("password"))
                        .role(s.role())
                        .branchId("B01")
                        .status(UserStatus.ACTIVE)
                        .failedLoginAttempts(0)
                        .build());
                created++;
            }
        }
        log.info("DataInitializer: {} seed users created (login e.g. admin@dp.com / password)", created);
    }
}
