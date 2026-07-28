package com.debtpulse.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * settlement-service — Settlement &amp; Restructuring Management (DebtPulse 2.5).
 *
 * <p>Owns settlement proposals (with a maker-checker approval workflow) and restructuring
 * proposals. Resolves approvers and records audits/notifications via Feign against
 * auth-service and notification-service (with Resilience4j circuit breakers + fallbacks).</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} pulls in the shared {@code common-lib}
 * components (logging aspect, global exception handler, security header filter,
 * Feign interceptor). {@code @EnableScheduling} drives the settlement-expiry job.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
public class SettlementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }
}
