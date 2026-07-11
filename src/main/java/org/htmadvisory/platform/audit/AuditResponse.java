package org.htmadvisory.platform.audit;

import org.htmadvisory.platform.audit.model.Audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        String url,
        String companyName,
        String requestedByEmail,
        Integer overallScore,
        String overallGrade,
        String status,
        String[] auditTypes,
        String claudeSummary,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        List<DimensionResponse> dimensions
) {
    public record DimensionResponse(
            UUID id,
            String auditType,
            String dimensionName,
            Integer score,
            String grade,
            List<FindingResponse> findings
    ) {}

    public record FindingResponse(
            UUID id,
            String severity,
            String finding,
            String recommendation
    ) {}

    public static AuditResponse from(Audit audit) {
        List<DimensionResponse> dims = audit.getDimensions().stream()
                .map(d -> new DimensionResponse(
                        d.getId(), d.getAuditType(), d.getDimensionName(),
                        d.getScore(), d.getGrade(),
                        d.getFindings().stream()
                                .map(f -> new FindingResponse(
                                        f.getId(), f.getSeverity(),
                                        f.getFinding(), f.getRecommendation()))
                                .toList()))
                .toList();

        return new AuditResponse(
                audit.getId(), audit.getUrl(), audit.getCompanyName(),
                audit.getRequestedByEmail(), audit.getOverallScore(),
                audit.getOverallGrade(), audit.getStatus().name(),
                audit.getAuditTypes(), audit.getClaudeSummary(),
                audit.getErrorMessage(), audit.getCreatedAt(),
                audit.getCompletedAt(), dims);
    }

    public static AuditResponse summary(Audit audit) {
        return new AuditResponse(
                audit.getId(), audit.getUrl(), audit.getCompanyName(),
                audit.getRequestedByEmail(), audit.getOverallScore(),
                audit.getOverallGrade(), audit.getStatus().name(),
                audit.getAuditTypes(), null, audit.getErrorMessage(),
                audit.getCreatedAt(), audit.getCompletedAt(), List.of());
    }
}
