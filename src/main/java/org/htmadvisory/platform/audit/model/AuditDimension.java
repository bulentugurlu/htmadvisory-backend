package org.htmadvisory.platform.audit.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_dimensions")
public class AuditDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Column(name = "audit_type", nullable = false, length = 50)
    private String auditType;

    @Column(name = "dimension_name", nullable = false)
    private String dimensionName;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, length = 2)
    private String grade;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditFinding> findings = new ArrayList<>();

    public AuditDimension() {}

    public AuditDimension(Audit audit, String auditType, String dimensionName, Integer score, String grade) {
        this.audit = audit;
        this.auditType = auditType;
        this.dimensionName = dimensionName;
        this.score = score;
        this.grade = grade;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Audit getAudit() { return audit; }
    public String getAuditType() { return auditType; }
    public String getDimensionName() { return dimensionName; }
    public Integer getScore() { return score; }
    public String getGrade() { return grade; }
    public Instant getCreatedAt() { return createdAt; }
    public List<AuditFinding> getFindings() { return findings; }
    public void setFindings(List<AuditFinding> findings) { this.findings = findings; }
}
