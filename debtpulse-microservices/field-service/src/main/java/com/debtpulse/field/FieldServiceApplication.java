package com.debtpulse.field;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * field-service — Field Recovery Management module (DebtPulse 2.4).
 *
 * <p>Owns field visits and asset-verification reports. Field officers schedule and complete
 * on-site visits to delinquent borrowers, sight/verify pledged collateral and record outcomes.
 * When collateral is verified the account-service is told to flag the asset as VERIFIED.</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} so the shared {@code common-lib} components
 * (logging aspect, global exception handler, security header filter, Feign interceptor) are
 * component-scanned in. {@code @EnableScheduling} drives the daily visit-reminder job;
 * {@code @EnableJpaAuditing} populates the {@code @CreatedDate} timestamps on entities.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableJpaAuditing
public class FieldServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FieldServiceApplication.class, args);
    }
}
