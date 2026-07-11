package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AriaLandmarksAuditor implements Auditor {

    @Override
    public String name() { return "ARIA Landmarks"; }

    @Override
    public String auditType() { return "ACCESSIBILITY"; }

    @Override
    public DimensionResult audit(PageContent page) {
        if (page.isEmpty()) {
            return new DimensionResult(name(), auditType(), 0,
                    List.of(FindingResult.high("Page could not be fetched.")));
        }

        Document doc = Jsoup.parse(page.html());
        List<FindingResult> findings = new ArrayList<>();
        int score = 0;

        if (doc.selectFirst("main") != null || doc.selectFirst("[role=main]") != null) {
            score += 25;
        } else {
            findings.add(FindingResult.medium("No <main> element or role=\"main\" found."));
        }

        if (doc.selectFirst("nav") != null || doc.selectFirst("[role=navigation]") != null) {
            score += 25;
        } else {
            findings.add(FindingResult.medium("No <nav> element or role=\"navigation\" found."));
        }

        if (doc.selectFirst("header") != null || doc.selectFirst("[role=banner]") != null) {
            score += 25;
        } else {
            findings.add(FindingResult.medium("No <header> element or role=\"banner\" found."));
        }

        if (doc.selectFirst("footer") != null || doc.selectFirst("[role=contentinfo]") != null) {
            score += 25;
        } else {
            findings.add(FindingResult.medium("No <footer> element or role=\"contentinfo\" found."));
        }

        return new DimensionResult(name(), auditType(), score, findings);
    }
}
