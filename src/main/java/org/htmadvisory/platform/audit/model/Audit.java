package org.htmadvisory.platform.audit.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audits")
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "requested_by_email")
    private String requestedByEmail;

    @Column(name = "person_id")
    private String personId;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "overall_grade", length = 2)
    private String overallGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status = AuditStatus.PENDING;

    @Column(name = "audit_types", columnDefinition = "TEXT[]")
    private String[] auditTypes;

    @Column(name = "claude_summary", columnDefinition = "TEXT")
    private String claudeSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditDimension> dimensions = new ArrayList<>();

    public Audit() {}

    public Audit(String url, String companyName, String requestedByEmail, String personId, String[] auditTypes) {
        this.url = url;
        this.companyName = companyName;
        this.requestedByEmail = requestedByEmail;
        this.personId = personId;
        this.auditTypes = auditTypes;
        this.status = AuditStatus.PENDING;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public String getUrl() { return url; }
    public String getCompanyName() { return companyName; }
    public String getRequestedByEmail() { return requestedByEmail; }
    public String getPersonId() { return personId; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public String getOverallGrade() { return overallGrade; }
    public void setOverallGrade(String overallGrade) { this.overallGrade = overallGrade; }
    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }
    public String[] getAuditTypes() { return auditTypes; }
    public String getClaudeSummary() { return claudeSummary; }
    public void setClaudeSummary(String claudeSummary) { this.claudeSummary = claudeSummary; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<AuditDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<AuditDimension> dimensions) { this.dimensions = dimensions; }
}
