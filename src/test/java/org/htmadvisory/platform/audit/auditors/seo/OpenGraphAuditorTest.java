package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenGraphAuditorTest {

    private final OpenGraphAuditor auditor = new OpenGraphAuditor();

    @Test
    void fullScore_whenAllOgTagsPresent() {
        String html = """
                <html><head>
                  <meta property="og:title" content="Test Title">
                  <meta property="og:description" content="Test description">
                  <meta property="og:image" content="https://example.com/image.png">
                  <meta property="og:url" content="https://example.com">
                </head></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void partialScore_whenSomeOgTagsMissing() {
        String html = """
                <html><head>
                  <meta property="og:title" content="Test Title">
                </head></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(25);
        assertThat(result.findings()).hasSize(3);
    }

    @Test
    void zeroScore_whenPageEmpty() {
        DimensionResult result = auditor.audit(PageContent.empty("https://example.com"));
        assertThat(result.score()).isEqualTo(0);
    }
}
