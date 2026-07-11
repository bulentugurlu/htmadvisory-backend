package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsTxtAuditorTest {

    private final RobotsTxtAuditor auditor = new RobotsTxtAuditor();

    @Test
    void fullScore_whenRobotsTxtHasSitemapAndAllows() {
        String robots = "User-agent: *\nAllow: /\nSitemap: https://example.com/sitemap.xml\n";
        DimensionResult result = auditor.audit(new PageContent("<html></html>", "Test", "https://example.com", robots));
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void partialScore_whenRobotsTxtMissingSitemap() {
        String robots = "User-agent: *\nAllow: /\n";
        DimensionResult result = auditor.audit(new PageContent("<html></html>", "Test", "https://example.com", robots));
        assertThat(result.score()).isEqualTo(60);
        assertThat(result.findings()).anyMatch(f -> f.finding().contains("Sitemap"));
    }

    @Test
    void zeroScore_whenRobotsTxtMissing() {
        DimensionResult result = auditor.audit(new PageContent("<html></html>", "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.findings()).anyMatch(f -> f.severity().equals("HIGH"));
    }
}
