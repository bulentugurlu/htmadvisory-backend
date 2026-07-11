package org.htmadvisory.platform.audit.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_findings")
public class AuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimension_id", nullable = false)
    private AuditDimension dimension;

    @Column(nullable = false, length = 10)
    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String finding;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public AuditFinding() {}

    public AuditFinding(AuditDimension dimension, String severity, String finding, String recommendation) {
        this.dimension = dimension;
        this.severity = severity;
        this.finding = finding;
        this.recommendation = recommendation;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AuditDimension getDimension() { return dimension; }
    public String getSeverity() { return severity; }
    public String getFinding() { return finding; }
    public String getRecommendation() { return recommendation; }
    public Instant getCreatedAt() { return createdAt; }
}
