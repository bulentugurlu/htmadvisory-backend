package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeadingStructureAuditorTest {

    private final HeadingStructureAuditor auditor = new HeadingStructureAuditor();

    @Test
    void fullScore_whenProperHeadingHierarchy() {
        String html = """
                <html><body>
                  <h1>Main Title</h1>
                  <h2>Section</h2>
                  <h3>Subsection</h3>
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void reducedScore_whenMultipleH1s() {
        String html = """
                <html><body>
                  <h1>Title One</h1>
                  <h1>Title Two</h1>
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isLessThan(100);
        assertThat(result.findings()).anyMatch(f -> f.finding().contains("Multiple H1"));
    }

    @Test
    void findingForSkippedHeadings() {
        String html = """
                <html><body>
                  <h1>Title</h1>
                  <h3>Skips H2</h3>
                </body></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.findings()).anyMatch(f -> f.finding().contains("skip"));
    }

    @Test
    void zeroScore_whenNoHeadings() {
        String html = "<html><body><p>No headings here</p></body></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(0);
    }
}
