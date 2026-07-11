package org.htmadvisory.platform.audit.auditors.accessibility;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HeadingStructureAuditor implements Auditor {

    @Override
    public String name() { return "Heading Structure"; }

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

        // At least one heading
        Elements allHeadings = doc.select("h1, h2, h3, h4, h5, h6");
        if (allHeadings.isEmpty()) {
            findings.add(FindingResult.high("No heading tags found on the page."));
            return new DimensionResult(name(), auditType(), 0, findings);
        }
        score += 30;

        // Exactly one h1
        Elements h1s = doc.select("h1");
        if (h1s.size() == 1) {
            score += 40;
        } else if (h1s.isEmpty()) {
            findings.add(FindingResult.high("No H1 tag found — every page should have exactly one H1."));
        } else {
            findings.add(FindingResult.medium("Multiple H1 tags found (" + h1s.size() + ") — use exactly one H1 per page."));
            score += 15;
        }

        // Logical heading order — detect level skips
        int[] levelOrder = allHeadings.stream()
                .mapToInt(h -> Integer.parseInt(h.tagName().substring(1)))
                .toArray();
        boolean hasSkip = false;
        for (int i = 1; i < levelOrder.length; i++) {
            if (levelOrder[i] > levelOrder[i - 1] + 1) {
                hasSkip = true;
                break;
            }
        }
        if (!hasSkip) {
            score += 30;
        } else {
            findings.add(FindingResult.medium("Heading levels skip (e.g., H1 → H3 without H2) — maintain logical hierarchy."));
        }

        return new DimensionResult(name(), auditType(), Math.min(score, 100), findings);
    }
}
