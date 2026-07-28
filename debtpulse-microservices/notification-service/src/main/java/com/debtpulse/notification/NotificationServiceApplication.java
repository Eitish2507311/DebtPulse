package com.debtpulse.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * notification-service — Notifications &amp; Alerts module (DebtPulse 2.8).
 *
 * <p>Stores and serves per-user in-app notifications. Every other microservice pushes
 * alerts here over Feign ({@code POST /api/internal/notifications}); the current user
 * reads and manages their own notifications via {@code /api/notifications}.</p>
 *
 * <p>{@code scanBasePackages = "com.debtpulse"} so the shared {@code common-lib} components
 * (logging aspect, global exception handler, security header filter) are component-scanned
 * into this service.</p>
 *
 * <p>This service has NO outbound Feign clients, so {@code @EnableFeignClients} is omitted
 * (nothing to declare). Created timestamps are set explicitly in the service layer, so JPA
 * auditing is not enabled.</p>
 */
@SpringBootApplication(scanBasePackages = "com.debtpulse")
@EnableDiscoveryClient
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
