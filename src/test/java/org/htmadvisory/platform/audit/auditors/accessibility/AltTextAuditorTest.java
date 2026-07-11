package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AltTextAuditorTest {

    private final AltTextAuditor auditor = new AltTextAuditor();

    @Test
    void fullScore_whenAllImagesHaveAlt() {
        String html = """
                <html><body>
                  <img src="a.png" alt="Logo">
                  <img src="b.png" alt="Hero image">
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void partialScore_whenSomeImagesMissingAlt() {
        String html = """
                <html><body>
                  <img src="a.png" alt="Logo">
                  <img src="b.png">
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(50);
        assertThat(result.findings()).isNotEmpty();
    }

    @Test
    void fullScore_whenNoImages() {
        String html = "<html><body><p>Text only</p></body></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void zeroScore_whenPageEmpty() {
        DimensionResult result = auditor.audit(PageContent.empty("https://example.com"));
        assertThat(result.score()).isEqualTo(0);
    }
}
