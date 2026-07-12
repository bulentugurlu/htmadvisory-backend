package org.htmadvisory.platform.audit.fetcher;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class PlaywrightPageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightPageFetcher.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public PageContent fetch(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
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
            } finally {
                page.close();
                browser.close();
            }
        } catch (Exception e) {
            log.warn("Playwright fetch failed for {}: {}", url, e.getMessage());
            return PageContent.empty(url);
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
}
