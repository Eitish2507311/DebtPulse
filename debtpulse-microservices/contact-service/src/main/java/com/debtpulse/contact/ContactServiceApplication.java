package com.debtpulse.contact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * contact-service — Contact &amp; Follow-Up Management module (DebtPulse 2.3).
 *
 * <p>Owns contact attempts, promise-to-pay (PTP) commitments and borrower contact
 * records. Validates accounts against account-service and notifies agents through
 * notification-service over Feign. A daily scheduler marks lapsed PTPs as broken.</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} pulls in the shared {@code common-lib}
 * components (logging aspect, global exception handler, security header filter,
 * Feign interceptor).</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
public class ContactServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContactServiceApplication.class, args);
    }
}
