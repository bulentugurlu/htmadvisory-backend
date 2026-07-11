package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoContentAuditorTest {

    private final GeoContentAuditor auditor = new GeoContentAuditor();

    @Test
    void maxScore_whenFaqAndAllBotsAllowed() {
        String html = """
                <html><head>
                  <script type="application/ld+json">{"@type":"FAQPage"}</script>
                </head></html>
                """;
        String robots = """
                User-agent: GPTBot
                Allow: /
                User-agent: PerplexityBot
                Allow: /
                User-agent: Claude-Web
                Allow: /
                """;
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", robots));
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void lowScore_whenNoFaqAndNoBotsAllowed() {
        String html = "<html><head></head></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.findings()).isNotEmpty();
    }
}
