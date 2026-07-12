package org.htmadvisory.platform.audit.fetcher;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
public class PlaywrightPageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightPageFetcher.class);

    public PageContent fetch(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(List.of(
                                    "--no-sandbox",             // required in containers (no user namespace)
                                    "--disable-setuid-sandbox", // belt-and-suspenders for sandbox disable
                                    "--disable-dev-shm-usage",  // Cloud Run /dev/shm is tiny; use /tmp instead
                                    "--disable-gpu",
                                    "--no-first-run",
                                    "--no-zygote",
                                    "--single-process"          // avoids forking sub-processes in constrained envs
                            )));
            try {
                Page page = browser.newPage();
                page.navigate(url, new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));

                String html = page.content();
                String title = page.title();
                String finalUrl = page.url();
                String robotsTxt = fetchRobotsTxt(browser, extractBaseUrl(url));

                return new PageContent(html, title, finalUrl, robotsTxt);
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            log.warn("Playwright fetch failed for {}: {}", url, e.getMessage());
            return PageContent.empty(url);
        }
    }

    private String fetchRobotsTxt(Browser browser, String baseUrl) {
        try {
            Page robotsPage = browser.newPage();
            Response response = robotsPage.navigate(baseUrl + "/robots.txt",
                    new Page.NavigateOptions().setTimeout(5000));
            if (response != null && response.status() == 200) {
                return robotsPage.content();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private String extractBaseUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return url;
        }
    }
}
