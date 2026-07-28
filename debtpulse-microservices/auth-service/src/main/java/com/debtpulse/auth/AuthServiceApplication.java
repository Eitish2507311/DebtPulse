package com.debtpulse.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * auth-service — Identity &amp; Access Management module (DebtPulse 2.1).
 *
 * <p>The JWT issuer for the whole platform: handles login, admin-driven registration,
 * user management, password reset and the immutable audit trail. Other services fetch
 * user data from here over Feign.</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} so the shared {@code common-lib} components
 * (logging aspect, global exception handler, security header filter, Feign interceptor)
 * are component-scanned into this service.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
