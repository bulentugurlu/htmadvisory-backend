package org.htmadvisory.platform.audit.fetcher;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class PlaywrightPageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightPageFetcher.class);

    // Playwright is not thread-safe — all calls must happen on the thread that created it.
    // A dedicated single-thread executor guarantees this while allowing the Browser to be
    // reused across audits (avoiding the per-request Chrome launch cost).
    private final ExecutorService playwrightThread = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "playwright-browser"));

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {
        playwrightThread.submit(() -> {
            try {
                playwright = Playwright.create();
                browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(true)
                                .setArgs(List.of(
                                        "--no-sandbox",
                                        "--disable-setuid-sandbox",
                                        "--disable-dev-shm-usage",
                                        "--disable-gpu",
                                        "--no-first-run",
                                        "--no-zygote",
                                        "--single-process"
                                )));
                log.info("Playwright browser launched and ready");
            } catch (Exception e) {
                log.error("Playwright browser failed to launch — audits will return empty results: {}", e.getMessage());
            }
        });
    }

    public PageContent fetch(String url) {
        if (browser == null) {
            log.warn("Playwright browser not available for {}", url);
            return PageContent.empty(url);
        }
        try {
            return playwrightThread.submit(() -> fetchOnPlaywrightThread(url))
                    .get(35, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Playwright fetch failed for {}: {}", url, e.getMessage());
            return PageContent.empty(url);
        }
    }

    private PageContent fetchOnPlaywrightThread(String url) {
        Page page = browser.newPage();
        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(30000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            String html = page.content();
            String title = page.title();
            String finalUrl = page.url();
            String robotsTxt = fetchRobotsTxtViaHttp(extractBaseUrl(finalUrl));

            return new PageContent(html, title, finalUrl, robotsTxt);
        } catch (Exception e) {
            log.warn("Page fetch error for {}: {}", url, e.getMessage());
            return PageContent.empty(url);
        } finally {
            page.close();
        }
    }

    private String fetchRobotsTxtViaHttp(String baseUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/robots.txt"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : "";
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

    @PreDestroy
    public void destroy() {
        try {
            playwrightThread.submit(() -> {
                if (browser != null) browser.close();
                if (playwright != null) playwright.close();
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Error closing Playwright browser: {}", e.getMessage());
        } finally {
            playwrightThread.shutdown();
        }
    }
}
