package org.htmadvisory.platform.audit.fetcher;

public record PageContent(String html, String title, String finalUrl, String robotsTxt) {

    public boolean isEmpty() {
        return html == null || html.isBlank();
    }

    public static PageContent empty(String url) {
        return new PageContent("", "", url, "");
    }
}
