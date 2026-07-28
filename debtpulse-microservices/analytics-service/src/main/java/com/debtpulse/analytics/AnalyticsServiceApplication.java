package com.debtpulse.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * analytics-service — Recovery Analytics &amp; Reporting (2.7).
 *
 * <p>Owns no transactional domain data of its own beyond generated {@code RecoveryReport}s;
 * it aggregates metrics from every other service over Feign (with Resilience4j circuit
 * breakers + fallbacks) to build recovery dashboards.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
