package org.htmadvisory.platform.audit.auditors.seo;

import org.htmadvisory.platform.audit.auditors.Auditor;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.htmadvisory.platform.audit.fetcher.PageContent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RobotsTxtAuditor implements Auditor {

    @Override
    public String name() { return "Robots.txt"; }

    @Override
    public String auditType() { return "SEO"; }

    @Override
    public DimensionResult audit(PageContent page) {
        String robots = page.robotsTxt() != null ? page.robotsTxt() : "";
        String robotsLower = robots.toLowerCase();

        List<FindingResult> findings = new ArrayList<>();
        int score = 0;

        if (!robots.isBlank()) {
            score += 30;
        } else {
            findings.add(FindingResult.high("robots.txt is missing or inaccessible."));
            return new DimensionResult(name(), auditType(), 0, findings);
        }

        if (robotsLower.contains("sitemap:")) {
            score += 40;
        } else {
            findings.add(FindingResult.medium("robots.txt does not declare a Sitemap — add 'Sitemap: https://yourdomain.com/sitemap.xml'."));
        }

        // Check for blanket disallow
        boolean blanketDisallow = robotsLower.contains("user-agent: *")
                && robotsLower.contains("disallow: /")
                && !robotsLower.contains("disallow: /\n")
                && !isOnlyRootDisallow(robotsLower);
        if (!blanketDisallow) {
            score += 30;
        } else {
            findings.add(FindingResult.high("robots.txt contains 'Disallow: /' for all user agents — search engines cannot crawl the site."));
        }

        return new DimensionResult(name(), auditType(), Math.min(score, 100), findings);
    }

    private boolean isOnlyRootDisallow(String robotsLower) {
        // If disallow: / appears only as disallow: /specific-path (not just /)
        return !robotsLower.matches("(?s).*disallow:\\s*/\\s*\\n.*");
    }
}
