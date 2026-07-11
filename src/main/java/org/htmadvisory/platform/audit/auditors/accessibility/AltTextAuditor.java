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
public class AltTextAuditor implements Auditor {

    @Override
    public String name() { return "Alt Text"; }

    @Override
    public String auditType() { return "ACCESSIBILITY"; }

    @Override
    public DimensionResult audit(PageContent page) {
        if (page.isEmpty()) {
            return new DimensionResult(name(), auditType(), 0,
                    List.of(FindingResult.high("Page could not be fetched.")));
        }

        Document doc = Jsoup.parse(page.html());
        Elements images = doc.select("img");
        List<FindingResult> findings = new ArrayList<>();

        if (images.isEmpty()) {
            return new DimensionResult(name(), auditType(), 100, findings);
        }

        long withAlt = images.stream()
                .filter(img -> !img.attr("alt").isBlank())
                .count();
        int total = images.size();
        int score = (int) ((withAlt * 100) / total);

        if (score < 100) {
            long missing = total - withAlt;
            findings.add(score < 50 ? FindingResult.high(missing + " of " + total + " images are missing alt text.")
                    : FindingResult.medium(missing + " of " + total + " images are missing alt text."));
        }

        return new DimensionResult(name(), auditType(), score, findings);
    }
}
