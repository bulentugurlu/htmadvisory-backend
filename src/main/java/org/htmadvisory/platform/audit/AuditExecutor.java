package org.htmadvisory.platform.audit;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.enricher.ClaudeAuditEnricher;
import org.htmadvisory.platform.audit.enricher.EnrichmentResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.htmadvisory.platform.audit.fetcher.PlaywrightPageFetcher;
import org.htmadvisory.platform.audit.model.Audit;
import org.htmadvisory.platform.audit.model.AuditDimension;
import org.htmadvisory.platform.audit.model.AuditFinding;
import org.htmadvisory.platform.audit.model.AuditStatus;
import org.htmadvisory.platform.audit.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuditExecutor {

    private static final Logger log = LoggerFactory.getLogger(AuditExecutor.class);

    private final AuditRepository auditRepository;
    private final PlaywrightPageFetcher pageFetcher;
    private final ClaudeAuditEnricher enricher;
    private final List<Auditor> auditors;

    public AuditExecutor(AuditRepository auditRepository,
                         PlaywrightPageFetcher pageFetcher,
                         ClaudeAuditEnricher enricher,
                         List<Auditor> auditors) {
        this.auditRepository = auditRepository;
        this.pageFetcher = pageFetcher;
        this.enricher = enricher;
        this.auditors = auditors;
    }

    @Async("auditTaskExecutor")
    @Transactional
    public void execute(UUID auditId) {
        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalStateException("Audit not found: " + auditId));

        try {
            audit.setStatus(AuditStatus.RUNNING);
            auditRepository.save(audit);

            // Fetch page
            PageContent page = pageFetcher.fetch(audit.getUrl());

            // Determine which audit types were requested
            Set<String> requestedTypes = audit.getAuditTypes() != null
                    ? Arrays.stream(audit.getAuditTypes()).collect(Collectors.toSet())
                    : Set.of("SEO", "ACCESSIBILITY");

            // Run relevant auditors
            List<DimensionResult> dimensionResults = auditors.stream()
                    .filter(a -> requestedTypes.contains(a.auditType()))
                    .map(a -> a.audit(page))
                    .toList();

            // Enrich with Claude
            EnrichmentResult enrichment = enricher.enrich(
                    audit.getUrl(), audit.getCompanyName(), dimensionResults);

            // Compute overall score (average of dimension scores)
            int overallScore = dimensionResults.isEmpty() ? 0
                    : (int) dimensionResults.stream()
                            .mapToInt(DimensionResult::score)
                            .average()
                            .orElse(0);

            // Persist dimensions and findings
            for (DimensionResult dr : dimensionResults) {
                AuditDimension dimension = new AuditDimension(
                        audit, dr.auditType(), dr.dimensionName(),
                        dr.score(), dr.grade());

                for (FindingResult fr : dr.findings()) {
                    String recommendation = findRecommendation(fr.finding(), enrichment);
                    AuditFinding finding = new AuditFinding(
                            dimension, fr.severity(), fr.finding(), recommendation);
                    dimension.getFindings().add(finding);
                }

                audit.getDimensions().add(dimension);
            }

            audit.setOverallScore(overallScore);
            audit.setOverallGrade(computeGrade(overallScore));
            audit.setClaudeSummary(buildSummary(enrichment));
            audit.setStatus(AuditStatus.COMPLETED);
            audit.setCompletedAt(Instant.now());
            auditRepository.save(audit);

            log.info("Audit {} completed. URL: {}, Score: {}", auditId, audit.getUrl(), overallScore);

        } catch (Exception e) {
            log.error("Audit {} failed: {}", auditId, e.getMessage(), e);
            audit.setStatus(AuditStatus.FAILED);
            audit.setErrorMessage(e.getMessage());
            audit.setCompletedAt(Instant.now());
            auditRepository.save(audit);
        }
    }

    private String findRecommendation(String finding, EnrichmentResult enrichment) {
        if (enrichment == null || enrichment.recommendations() == null) return null;
        return enrichment.recommendations().stream()
                .filter(r -> finding.equals(r.get("finding")))
                .map(r -> r.get("recommendation"))
                .findFirst()
                .orElse(null);
    }

    private String buildSummary(EnrichmentResult enrichment) {
        if (enrichment == null) return null;
        StringBuilder sb = new StringBuilder();
        if (enrichment.executiveSummary() != null && !enrichment.executiveSummary().isBlank()) {
            sb.append(enrichment.executiveSummary());
        }
        if (enrichment.closingStatement() != null && !enrichment.closingStatement().isBlank()) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append(enrichment.closingStatement());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String computeGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
