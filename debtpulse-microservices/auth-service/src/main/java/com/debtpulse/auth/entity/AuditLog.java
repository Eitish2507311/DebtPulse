package com.debtpulse.auth.entity;

import com.debtpulse.common.id.BusinessId;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry (2.1 Identity & Access Management).
 *
 * <p>Centralised in auth-service. Every other microservice records business actions here
 * over Feign ({@code POST /api/internal/audit-logs}), so financial approvals, contact
 * records and legal actions all land in one tamper-evident log. {@code userId} is stored
 * as a plain string (the JWT subject) rather than a FK, keeping the log decoupled.</p>
 */
@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @BusinessId(prefix = "AUD")
    private String auditId;

    /** The userId (JWT subject) that performed the action; "SYSTEM" for scheduled jobs. */
    private String userId;

    private String action;

    private String entityType;

    private String recordId;

    private String sourceService;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
