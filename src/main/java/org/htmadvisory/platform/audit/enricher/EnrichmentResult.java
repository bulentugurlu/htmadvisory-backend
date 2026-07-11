package org.htmadvisory.platform.audit.enricher;

import java.util.List;
import java.util.Map;

public record EnrichmentResult(
        String executiveSummary,
        List<Map<String, String>> recommendations,
        String closingStatement
) {
    public static EnrichmentResult empty() {
        return new EnrichmentResult("", List.of(), "");
    }
}
