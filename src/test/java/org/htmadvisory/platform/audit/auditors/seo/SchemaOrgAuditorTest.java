package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaOrgAuditorTest {

    private final SchemaOrgAuditor auditor = new SchemaOrgAuditor();

    @Test
    void fullScore_whenFullSchemaPresent() {
        String html = """
                <html><head>
                  <script type="application/ld+json">
                  {"@type":"Organization","name":"Acme","description":"Test co","url":"https://acme.com"}
                  </script>
                </head></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void partialScore_whenSchemaLacksType() {
        String html = """
                <html><head>
                  <script type="application/ld+json">
                  {"name":"Acme","description":"Test co","url":"https://acme.com"}
                  </script>
                </head></html>
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(70); // 40 + 0 + 30
    }

    @Test
    void zeroScore_whenNoScript() {
        String html = "<html><head></head></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.findings()).isNotEmpty();
    }
}
