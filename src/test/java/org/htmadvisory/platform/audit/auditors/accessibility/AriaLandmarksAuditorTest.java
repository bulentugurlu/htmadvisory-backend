package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AriaLandmarksAuditorTest {

    private final AriaLandmarksAuditor auditor = new AriaLandmarksAuditor();

    @Test
    void fullScore_whenAllLandmarksPresent() {
        String html = """
                <html><body>
                  <header>Site header</header>
                  <nav>Navigation</nav>
                  <main>Main content</main>
                  <footer>Footer</footer>
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void partialScore_whenSomeLandmarksMissing() {
        String html = """
                <html><body>
                  <header>Header</header>
                  <main>Main</main>
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(50);
        assertThat(result.findings()).hasSize(2);
    }

    @Test
    void fullScore_whenRoleAttributesUsed() {
        String html = """
                <html><body>
                  <div role="banner">Header</div>
                  <div role="navigation">Nav</div>
                  <div role="main">Main</div>
                  <div role="contentinfo">Footer</div>
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
    }
}
