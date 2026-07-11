package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LangAttributeAuditorTest {

    private final LangAttributeAuditor auditor = new LangAttributeAuditor();

    @Test
    void fullScore_whenLangPresent() {
        String html = "<html lang=\"en\"><head></head><body></body></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void zeroScore_whenLangMissing() {
        String html = "<html><head></head><body></body></html>";
        DimensionResult result = auditor.audit(new PageContent(html, "Test", "https://example.com", ""));
        assertThat(result.score()).isEqualTo(0);
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).severity()).isEqualTo("HIGH");
    }

    @Test
    void zeroScore_whenPageEmpty() {
        DimensionResult result = auditor.audit(PageContent.empty("https://example.com"));
        assertThat(result.score()).isEqualTo(0);
    }
}
