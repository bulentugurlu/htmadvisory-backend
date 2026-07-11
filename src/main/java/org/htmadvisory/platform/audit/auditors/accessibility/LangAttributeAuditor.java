package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LangAttributeAuditor implements Auditor {

    @Override
    public String name() { return "Language Attribute"; }

    @Override
    public String auditType() { return "ACCESSIBILITY"; }

    @Override
    public DimensionResult audit(PageContent page) {
        if (page.isEmpty()) {
            return new DimensionResult(name(), auditType(), 0,
                    List.of(FindingResult.high("Page could not be fetched.")));
        }

        Document doc = Jsoup.parse(page.html());
        String lang = doc.select("html").attr("lang");

        if (lang != null && !lang.isBlank()) {
            return new DimensionResult(name(), auditType(), 100, List.of());
        }

        return new DimensionResult(name(), auditType(), 0,
                List.of(FindingResult.high(
                        "The <html> element is missing a lang attribute — required for screen readers to use the correct language.")));
    }
}
