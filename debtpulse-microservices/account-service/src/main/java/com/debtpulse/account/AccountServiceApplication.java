package com.debtpulse.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * account-service — Delinquent Loan Portfolio Management module (DebtPulse 2.2).
 *
 * <p>Owns delinquent accounts, their collateral assets and the allocation rules that
 * distribute accounts to collections agents. Runs nightly schedulers that age DPD buckets
 * and escalate stagnant accounts, and exposes internal Feign endpoints so other services
 * can look up account data and mark collateral verified.</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} so the shared {@code common-lib} components
 * (logging aspect, global exception handler, security header filter, Feign interceptor)
 * are component-scanned into this service.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
