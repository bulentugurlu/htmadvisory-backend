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
public class GeoContentAuditor implements Auditor {

    @Override
    public String name() { return "GEO Content"; }

    @Override
    public String auditType() { return "SEO"; }

    @Override
    public DimensionResult audit(PageContent page) {
        if (page.isEmpty()) {
            return new DimensionResult(name(), auditType(), 0,
                    List.of(FindingResult.high("Page could not be fetched.")));
        }

        Document doc = Jsoup.parse(page.html());
        String htmlLower = doc.html().toLowerCase();
        String robotsLower = (page.robotsTxt() != null ? page.robotsTxt() : "").toLowerCase();

        List<FindingResult> findings = new ArrayList<>();
        int score = 0;

        // FAQPage schema
        if (htmlLower.contains("\"faqpage\"") || htmlLower.contains("faqpage")) {
            score += 40;
        } else {
            findings.add(FindingResult.medium("No FAQPage schema found — FAQ markup helps AI search engines surface answers."));
        }

        // robots.txt AI bot allowances
        if (robotsLower.contains("gptbot") && !isDisallowed(robotsLower, "gptbot")) {
            score += 20;
        } else {
            findings.add(FindingResult.low("robots.txt does not explicitly allow GPTBot (OpenAI crawler)."));
        }

        if (robotsLower.contains("perplexitybot") && !isDisallowed(robotsLower, "perplexitybot")) {
            score += 20;
        } else {
            findings.add(FindingResult.low("robots.txt does not explicitly allow PerplexityBot."));
        }

        if (robotsLower.contains("claude-web") && !isDisallowed(robotsLower, "claude-web")) {
            score += 20;
        } else {
            findings.add(FindingResult.low("robots.txt does not explicitly allow Claude-Web (Anthropic crawler)."));
        }

        return new DimensionResult(name(), auditType(), Math.min(score, 100), findings);
    }

    private boolean isDisallowed(String robotsTxt, String botName) {
        int botIndex = robotsTxt.indexOf("user-agent: " + botName);
        if (botIndex == -1) return false;
        String afterBot = robotsTxt.substring(botIndex);
        int nextAgent = afterBot.indexOf("user-agent:", 12);
        String section = nextAgent == -1 ? afterBot : afterBot.substring(0, nextAgent);
        return section.contains("disallow: /");
    }
}
