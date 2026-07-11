package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SchemaOrgAuditor implements Auditor {

    private static final Set<String> DESIRED_TYPES = Set.of(
            "Organization", "ProfessionalService", "LocalBusiness");

    @Override
    public String name() { return "Schema.org"; }

    @Override
    public String auditType() { return "SEO"; }

    @Override
    public DimensionResult audit(PageContent page) {
        if (page.isEmpty()) {
            return new DimensionResult(name(), auditType(), 0,
                    List.of(FindingResult.high("Page could not be fetched.")));
        }

        Document doc = Jsoup.parse(page.html());
        List<FindingResult> findings = new ArrayList<>();
        int score = 0;

        Elements scripts = doc.select("script[type=application/ld+json]");
        if (scripts.isEmpty()) {
            findings.add(FindingResult.high("No Schema.org JSON-LD markup found."));
            return new DimensionResult(name(), auditType(), 0, findings);
        }

        score += 40;

        String combinedJson = scripts.stream()
                .map(Element::data)
                .reduce("", String::concat)
                .toLowerCase();

        boolean hasDesiredType = DESIRED_TYPES.stream()
                .anyMatch(t -> combinedJson.contains(t.toLowerCase()));
        if (hasDesiredType) {
            score += 30;
        } else {
            findings.add(FindingResult.medium(
                    "Schema.org markup present but missing Organization, ProfessionalService, or LocalBusiness type."));
        }

        boolean hasRequiredFields = combinedJson.contains("\"name\"")
                && combinedJson.contains("\"description\"")
                && combinedJson.contains("\"url\"");
        if (hasRequiredFields) {
            score += 30;
        } else {
            findings.add(FindingResult.medium(
                    "Schema.org markup missing one or more of: name, description, url fields."));
        }

        return new DimensionResult(name(), auditType(), Math.min(score, 100), findings);
    }
}
