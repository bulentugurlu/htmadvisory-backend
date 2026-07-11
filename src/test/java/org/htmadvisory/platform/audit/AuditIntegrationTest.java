package org.htmadvisory.platform.audit;

import org.htmadvisory.platform.audit.enricher.ClaudeAuditEnricher;
import org.htmadvisory.platform.audit.enricher.EnrichmentResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.htmadvisory.platform.audit.fetcher.PlaywrightPageFetcher;
import org.htmadvisory.platform.audit.model.Audit;
import org.htmadvisory.platform.audit.model.AuditStatus;
import org.htmadvisory.platform.audit.repository.AuditRepository;
import org.htmadvisory.platform.shared.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AuditIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @MockBean
    private PlaywrightPageFetcher pageFetcher;

    @MockBean
    private ClaudeAuditEnricher enricher;

    @Test
    void fullAuditFlow_completes_withMockedFetcherAndEnricher() {
        String html = """
                <html lang="en"><head>
                  <title>Acme Corporation Professional Services Consulting</title>
                  <meta name="description" content="Leading professional services firm helping Fortune 500 companies achieve digital transformation goals and sustainable competitive advantage.">
                  <link rel="canonical" href="https://acmecorp.com">
                  <meta property="og:title" content="Acme Corp">
                  <meta property="og:description" content="Services">
                  <meta property="og:image" content="https://acmecorp.com/img.png">
                  <meta property="og:url" content="https://acmecorp.com">
                </head>
                <body>
                  <header>Header</header>
                  <nav>Nav</nav>
                  <main><h1>Welcome</h1><h2>Services</h2></main>
                  <footer>Footer</footer>
                </body></html>
                """;
        when(pageFetcher.fetch(anyString()))
                .thenReturn(new PageContent(html, "Acme Corp", "https://acmecorp.com",
                        "User-agent: *\nAllow: /\nSitemap: https://acmecorp.com/sitemap.xml"));
        when(enricher.enrich(anyString(), anyString(), anyList()))
                .thenReturn(new EnrichmentResult("Good website.", List.of(), "Fix these issues."));

        AuditRequest request = new AuditRequest();
        request.setUrl("https://acmecorp.com");
        request.setCompanyName("Acme Corp");
        request.setAuditTypes(List.of("SEO", "ACCESSIBILITY"));

        AuditStartResponse response = auditService.startAudit(request);
        assertThat(response.auditId()).isNotNull();

        UUID auditId = response.auditId();

        // Poll until completed (async execution)
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<Audit> audit = auditRepository.findById(auditId);
                    assertThat(audit).isPresent();
                    assertThat(audit.get().getStatus()).isIn(AuditStatus.COMPLETED, AuditStatus.FAILED);
                });

        Audit completed = auditRepository.findByIdWithDimensions(auditId).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(AuditStatus.COMPLETED);
        assertThat(completed.getOverallScore()).isNotNull().isGreaterThan(0);
        assertThat(completed.getDimensions()).isNotEmpty();
    }

    @Test
    void auditPersisted_withPendingStatus_immediately() {
        when(pageFetcher.fetch(anyString())).thenReturn(PageContent.empty("https://slowsite.com"));
        when(enricher.enrich(anyString(), any(), anyList())).thenReturn(EnrichmentResult.empty());

        AuditRequest request = new AuditRequest();
        request.setUrl("https://slowsite.com");
        request.setAuditTypes(List.of("SEO"));

        AuditStartResponse response = auditService.startAudit(request);

        // Immediately after calling startAudit, the entity should exist (PENDING or RUNNING)
        Optional<Audit> audit = auditRepository.findById(response.auditId());
        assertThat(audit).isPresent();
        assertThat(audit.get().getStatus()).isIn(AuditStatus.PENDING, AuditStatus.RUNNING, AuditStatus.COMPLETED);
    }
}
