package com.debtpulse.auth.entity;

import com.debtpulse.common.enums.Role;
import com.debtpulse.common.enums.UserStatus;
import com.debtpulse.common.id.BusinessId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A collections-staff user (2.1 Identity & Access Management).
 * Owned exclusively by auth-service; other services reference a user only by its id.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @BusinessId(prefix = "USR")
    private String userId;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String phone;

    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String branchId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @JsonIgnore
    private String resetToken;

    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    @Builder.Default
    @JsonIgnore
    private Integer failedLoginAttempts = 0;

    @JsonIgnore
    private LocalDateTime lockedUntil;
}
