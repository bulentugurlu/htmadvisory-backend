package org.htmadvisory.platform.traffic;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A single browsing session on the site — not a single page hit.
 * Multiple page views within the same session accumulate onto one Visit
 * record via {@code pagesViewed}, which grows as the session continues.
 *
 * Most visits start with {@code personId} null — a visitor lands before any
 * email is known. {@code personId} is stamped retroactively by
 * {@link TrafficService#backfillPersonId(String, String)} if/when that session
 * later provides an email anywhere on the platform (Contact, Survey, etc.).
 * This is what makes anonymous browsing behaviour visible in hindsight once
 * identity is established, without having to wait for identity before recording.
 *
 * Privacy note: {@code ipAddress} and {@code serviceProvider} are personal data
 * under GDPR. A privacy notice, retention policy, and legal review are open
 * items before this domain goes live in production (flagged in ARCHITECTURE.md).
 */
@Document(collection = "visits")
public class Visit {

    @Id
    private String id;

    /** Generated client-side or at first request; groups multiple page views into one visit. */
    private String sessionId;

    /** NULLABLE — most visits start with no known Person; populated retroactively. */
    private String personId;

    /** Captured server-side from the request — not client-supplied. */
    private String ipAddress;

    /** Derived from {@code ipAddress} via geolocation lookup. Nullable if lookup fails. */
    private String city;

    /**
     * ISP / service provider derived from {@code ipAddress}. Nullable if lookup fails.
     * Field name is {@code serviceProvider} — NOT {@code isp}.
     */
    private String serviceProvider;

    /** Derived from user agent: {@code desktop}, {@code mobile}, or {@code tablet}. */
    private String deviceType;

    /** Browser name derived from user agent (e.g. {@code Chrome}, {@code Safari}). */
    private String browser;

    private Instant startedAt;
    private Instant lastActivityAt;

    /** Pages viewed in this session, in order. Grows as the same session continues browsing. */
    private List<String> pagesViewed;

    public Visit() {
        this.pagesViewed = new ArrayList<>();
    }

    public Visit(String sessionId, String ipAddress, String city, String serviceProvider,
                 String deviceType, String browser, Instant startedAt, String firstPage) {
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.city = city;
        this.serviceProvider = serviceProvider;
        this.deviceType = deviceType;
        this.browser = browser;
        this.startedAt = startedAt;
        this.lastActivityAt = startedAt;
        this.pagesViewed = new ArrayList<>();
        if (firstPage != null) {
            this.pagesViewed.add(firstPage);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public List<String> getPagesViewed() {
        return pagesViewed;
    }

    public void setPagesViewed(List<String> pagesViewed) {
        this.pagesViewed = pagesViewed;
    }
}
