package com.debtpulse.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A stored refresh-token record. The raw token is NEVER persisted — only its SHA-256 hash
 * ({@code tokenHash}). Tokens rotate on every refresh (one-time use); {@code replacedById} links
 * the rotation lineage so a replayed (already-rotated) token can be detected as reuse.
 * {@code sessionId} groups a login session's lineage; {@code lastActivityAt} drives the sliding
 * idle-timeout window.
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_rt_user", columnList = "userId"),
        @Index(name = "idx_rt_session", columnList = "sessionId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @Column(unique = true, length = 64)
    private String tokenHash;

    private String sessionId;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime lastActivityAt;

    @Builder.Default
    private boolean revoked = false;

    private String replacedById;
}
