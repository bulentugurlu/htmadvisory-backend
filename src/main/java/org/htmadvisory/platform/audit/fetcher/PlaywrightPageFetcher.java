package org.htmadvisory.platform.audit.fetcher;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class PlaywrightPageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightPageFetcher.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public PageContent fetch(String url) {
        try {
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; HTMAdvisoryBot/1.0)")
                    .timeout(15000)
                    .followRedirects(true)
                    .maxBodySize(0);
            Document doc = connection.get();

            String html = doc.outerHtml();
            String title = doc.title();
            String finalUrl = connection.response().url().toString();
            String robotsTxt = fetchRobotsTxtViaHttp(extractBaseUrl(finalUrl));

            return new PageContent(html, title, finalUrl, robotsTxt);
        } catch (Exception e) {
            log.warn("Page fetch failed for {}: {}", url, e.getMessage());
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
