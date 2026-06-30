package org.htmadvisory.platform.traffic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Test data factory for {@link Visit}, following the {@code aXxx().withYyy().build()}
 * builder convention used across all domains.
 *
 * Sensible defaults are provided for every field so tests only need to specify
 * what is relevant to the scenario under test.
 */
public class VisitTestDataBuilder {

    private String sessionId = "test-session-id";
    private String personId = null;
    private String ipAddress = "203.0.113.1";
    private String city = "Minneapolis";
    private String serviceProvider = "Comcast";
    private String deviceType = "desktop";
    private String browser = "Chrome";
    private Instant startedAt = Instant.now();
    private Instant lastActivityAt = Instant.now();
    private List<String> pagesViewed = new ArrayList<>(List.of("/"));

    public static VisitTestDataBuilder aVisit() {
        return new VisitTestDataBuilder();
    }

    public VisitTestDataBuilder withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public VisitTestDataBuilder withPersonId(String personId) {
        this.personId = personId;
        return this;
    }

    public VisitTestDataBuilder withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public VisitTestDataBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public VisitTestDataBuilder withServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
        return this;
    }

    public VisitTestDataBuilder withDeviceType(String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public VisitTestDataBuilder withBrowser(String browser) {
        this.browser = browser;
        return this;
    }

    public VisitTestDataBuilder withStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public VisitTestDataBuilder withPagesViewed(List<String> pagesViewed) {
        this.pagesViewed = new ArrayList<>(pagesViewed);
        return this;
    }

    public Visit build() {
        Visit visit = new Visit();
        visit.setSessionId(sessionId);
        visit.setPersonId(personId);
        visit.setIpAddress(ipAddress);
        visit.setCity(city);
        visit.setServiceProvider(serviceProvider);
        visit.setDeviceType(deviceType);
        visit.setBrowser(browser);
        visit.setStartedAt(startedAt);
        visit.setLastActivityAt(lastActivityAt);
        visit.setPagesViewed(new ArrayList<>(pagesViewed));
        return visit;
    }
}
