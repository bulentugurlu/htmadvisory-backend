package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetaTagsAuditor implements Auditor {

    @Override
    public String name() { return "Meta Tags"; }

    @Override
    public String auditType() { return "SEO"; }

    @Override
    public DimensionResult audit(PageContent page) {
        if (page.isEmpty()) {
            return new DimensionResult(name(), auditType(), 0,
                    List.of(FindingResult.high("Page could not be fetched — site may block automated access.")));
        }

        Document doc = Jsoup.parse(page.html());
        List<FindingResult> findings = new ArrayList<>();
        int score = 0;

        // Title tag: 30-60 chars
        Element titleEl = doc.selectFirst("title");
        if (titleEl != null && !titleEl.text().isBlank()) {
            int len = titleEl.text().length();
            if (len >= 30 && len <= 60) {
                score += 25;
            } else {
                findings.add(FindingResult.medium(
                        "Title tag is " + len + " characters (ideal: 30-60). Current: \"" + titleEl.text() + "\""));
                score += 10;
            }
        } else {
            findings.add(FindingResult.high("No title tag found."));
        }

        // Meta description: 120-160 chars
        Element descEl = doc.selectFirst("meta[name=description]");
        if (descEl != null && !descEl.attr("content").isBlank()) {
            int len = descEl.attr("content").length();
            if (len >= 120 && len <= 160) {
                score += 25;
            } else {
                findings.add(FindingResult.medium(
                        "Meta description is " + len + " characters (ideal: 120-160)."));
                score += 10;
            }
        } else {
            findings.add(FindingResult.high("No meta description found."));
        }

        // Meta robots - not blocking indexing
        Element robotsEl = doc.selectFirst("meta[name=robots]");
        if (robotsEl == null) {
            score += 25; // absence = allow
        } else {
            String content = robotsEl.attr("content").toLowerCase();
            if (content.contains("noindex") || content.contains("nofollow")) {
                findings.add(FindingResult.high("Meta robots tag is blocking search engine indexing: " + content));
            } else {
                score += 25;
            }
        }

        // Canonical link
        Element canonicalEl = doc.selectFirst("link[rel=canonical]");
        if (canonicalEl != null && !canonicalEl.attr("href").isBlank()) {
            score += 25;
        } else {
            findings.add(FindingResult.medium("No canonical link tag found."));
        }

        return new DimensionResult(name(), auditType(), Math.min(score, 100), findings);
    }
}
