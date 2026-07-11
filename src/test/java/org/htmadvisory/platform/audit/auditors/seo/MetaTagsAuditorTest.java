package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetaTagsAuditorTest {

    private final MetaTagsAuditor auditor = new MetaTagsAuditor();

    @Test
    void fullScore_whenAllMetaTagsPresent() {
        String html = """
                <html><head>
                  <title>Professional Consulting Services for Enterprise Clients</title>
                  <meta name="description" content="We provide expert consulting services to Fortune 500 companies, helping them navigate digital transformation and achieve sustainable growth results.">
                  <link rel="canonical" href="https://example.com/services">
                </head><body></body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void lowScore_whenNoMetaTags() {
        String html = "<html><head><title>Hi</title></head><body></body></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isLessThan(50);
        assertThat(result.findings()).isNotEmpty();
    }

    @Test
    void zeroScore_whenPageEmpty() {
        DimensionResult result = auditor.audit(PageContent.empty("https://example.com"));
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).severity()).isEqualTo("HIGH");
    }

    @Test
    void findingGenerated_whenTitleTooShort() {
        String html = "<html><head><title>Hi</title></head><body></body></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        boolean hasTitleFinding = result.findings().stream()
                .anyMatch(f -> f.finding().contains("Title tag"));
        assertThat(hasTitleFinding).isTrue();
    }

    @Test
    void highFinding_whenRobotsBlocksIndexing() {
        String html = """
                <html><head>
                  <title>Professional Consulting Services for Enterprise Clients Today</title>
                  <meta name="robots" content="noindex,nofollow">
                  <meta name="description" content="We provide expert consulting services to Fortune 500 companies, helping them navigate digital transformation and achieve sustainable growth results.">
                  <link rel="canonical" href="https://example.com">
                </head><body></body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        boolean hasBlockFinding = result.findings().stream()
                .anyMatch(f -> f.finding().contains("blocking") && f.severity().equals("HIGH"));
        assertThat(hasBlockFinding).isTrue();
    }
}
