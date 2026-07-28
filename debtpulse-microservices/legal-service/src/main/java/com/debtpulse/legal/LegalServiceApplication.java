package com.debtpulse.legal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * legal-service — Legal Proceedings Management module (DebtPulse 2.6).
 *
 * <p>Owns legal cases, court hearings and recovery orders for delinquent accounts.
 * Cross-service references (accounts, users) are stored as plain id strings and resolved
 * over Feign; a scheduled job alerts legal officers about upcoming hearings.</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} so the shared {@code common-lib} components
 * (logging aspect, global exception handler, security header filter, Feign interceptor)
 * are component-scanned into this service.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableJpaAuditing
public class LegalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalServiceApplication.class, args);
    }
}
