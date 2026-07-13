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
        // Playwright.create() downloads browsers if not found — in Cloud Run we pre-install
        // them in the Docker image at /ms-playwright. Setting the env var here ensures
        // the Java library finds them without attempting a download (which hangs in Cloud Run).
        System.setProperty("playwright.driver.impl", "com.microsoft.playwright.impl.driver.jar.DriverJar");
        try (Playwright playwright = Playwright.create()) {
            // Find chromium executable — check pre-installed location first
            java.nio.file.Path chromiumPath = null;
            java.nio.file.Path msPlaywright = java.nio.file.Paths.get("/ms-playwright");
            if (java.nio.file.Files.exists(msPlaywright)) {
                try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(msPlaywright, 3)) {
                    chromiumPath = paths
                        .filter(p -> p.getFileName().toString().equals("chrome") || p.getFileName().toString().equals("chromium"))
                        .filter(java.nio.file.Files::isExecutable)
                        .findFirst().orElse(null);
                }
            }
            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-gpu",
                            "--no-first-run",
                            "--no-zygote",
                            "--single-process"
                    ));
            if (chromiumPath != null) {
                log.info("Using pre-installed Chromium at: {}", chromiumPath);
                opts.setExecutablePath(chromiumPath);
            } else {
                log.warn("Pre-installed Chromium not found at /ms-playwright — Playwright will attempt download");
            }
            Browser browser = playwright.chromium().launch(opts);
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
