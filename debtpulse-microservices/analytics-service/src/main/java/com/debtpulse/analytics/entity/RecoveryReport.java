package com.debtpulse.analytics.entity;

import com.debtpulse.common.id.BusinessId;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A point-in-time recovery report (2.7). {@code metrics} holds the aggregated metric set
 * serialized as JSON, so the report schema can evolve without DDL changes.
 */
@Entity
@Table(name = "recovery_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RecoveryReport {

    @Id
    @BusinessId(prefix = "RPT")
    private String reportId;

    /** Scope of the report, e.g. Branch / Agent / Bucket / Product / Period. */
    private String scope;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metrics;

    @CreatedDate
    private LocalDateTime generatedDate;
}
