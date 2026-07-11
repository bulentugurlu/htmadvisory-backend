package org.htmadvisory.platform.audit.auditors.seo;

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
public class OpenGraphAuditor implements Auditor {

    @Override
    public String name() { return "Open Graph"; }

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

        String[] tags = {"og:title", "og:description", "og:image", "og:url"};
        for (String tag : tags) {
            var el = doc.selectFirst("meta[property=" + tag + "]");
            if (el != null && !el.attr("content").isBlank()) {
                score += 25;
            } else {
                findings.add(FindingResult.medium("Missing Open Graph tag: " + tag));
            }
        }

        return new DimensionResult(name(), auditType(), score, findings);
    }
}
