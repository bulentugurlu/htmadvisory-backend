package org.htmadvisory.platform.traffic;

import org.htmadvisory.platform.shared.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for TrafficService, run against a real, throwaway
 * MongoDB instance (see AbstractMongoIntegrationTest).
 *
 * GeoLocationService is mocked so tests never depend on real MaxMind
 * .mmdb files or network access — geo data stays deterministic.
 */
class TrafficServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private TrafficService trafficService;

    @Autowired
    private VisitRepository visitRepository;

    @MockBean
    private GeoLocationService geoLocationService;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        when(geoLocationService.lookup(any())).thenReturn(new GeoLocationResult("Minneapolis", "Comcast"));
    }

    @Test
    void shouldCreateNewVisitForFreshSessionWithPersonIdNull() {
        Visit result = trafficService.recordVisit("session-1", "203.0.113.1", "desktop", "Chrome", "/");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getSessionId()).isEqualTo("session-1");
        assertThat(result.getPersonId()).isNull();
        assertThat(result.getCity()).isEqualTo("Minneapolis");
        assertThat(result.getServiceProvider()).isEqualTo("Comcast");
        assertThat(result.getDeviceType()).isEqualTo("desktop");
        assertThat(result.getBrowser()).isEqualTo("Chrome");
        assertThat(result.getPagesViewed()).containsExactly("/");
        assertThat(result.getStartedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
        assertThat(result.getLastActivityAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldAppendPageToExistingActiveSessionNotCreateDuplicate() {
        trafficService.recordVisit("session-2", "203.0.113.1", "desktop", "Chrome", "/");
        trafficService.recordVisit("session-2", "203.0.113.1", "desktop", "Chrome", "/contact");

        List<Visit> visits = visitRepository.findBySessionId("session-2");

        // One Visit record, not two — the second call appended to the first.
        assertThat(visits).hasSize(1);
        assertThat(visits.get(0).getPagesViewed()).containsExactly("/", "/contact");
    }

    @Test
    void shouldBackfillPersonIdOntoExistingAnonymousVisit() {
        trafficService.recordVisit("session-3", "203.0.113.1", "mobile", "Safari", "/");
        assertThat(visitRepository.findBySessionId("session-3").get(0).getPersonId()).isNull();

        trafficService.backfillPersonId("session-3", "person-abc");

        List<Visit> visits = visitRepository.findBySessionId("session-3");
        assertThat(visits).hasSize(1);
        assertThat(visits.get(0).getPersonId()).isEqualTo("person-abc");
    }

    @Test
    void shouldFindVisitsByPersonIdAfterBackfill() {
        trafficService.recordVisit("session-4", "203.0.113.1", "desktop", "Firefox", "/");
        trafficService.backfillPersonId("session-4", "person-xyz");

        List<Visit> byPerson = trafficService.findByPersonId("person-xyz");

        assertThat(byPerson).hasSize(1);
        assertThat(byPerson.get(0).getSessionId()).isEqualTo("session-4");
    }

    @Test
    void shouldFindVisitsByDateRange() {
        Instant before = Instant.now().minusSeconds(1);
        trafficService.recordVisit("session-5", "203.0.113.1", "desktop", "Chrome", "/survey");
        Instant after = Instant.now().plusSeconds(1);

        List<Visit> inRange = trafficService.findByDateRange(before, after);

        assertThat(inRange).isNotEmpty();
        assertThat(inRange).anyMatch(v -> "session-5".equals(v.getSessionId()));
    }

    @Test
    void shouldFindVisitsBySessionId() {
        trafficService.recordVisit("session-6", "203.0.113.1", "tablet", "Safari", "/about");

        List<Visit> bySession = trafficService.findBySessionId("session-6");

        assertThat(bySession).hasSize(1);
        assertThat(bySession.get(0).getPagesViewed()).containsExactly("/about");
    }

    @Test
    void shouldStillSaveVisitWhenGeoLocationServiceReturnsNullFields() {
        when(geoLocationService.lookup(any())).thenReturn(new GeoLocationResult(null, null));

        Visit result = trafficService.recordVisit("session-7", "192.168.1.1", "desktop", "Chrome", "/");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getCity()).isNull();
        assertThat(result.getServiceProvider()).isNull();
        assertThat(result.getPagesViewed()).containsExactly("/");
    }

    @Test
    void shouldStillSaveVisitWhenGeoLocationServiceThrows() {
        when(geoLocationService.lookup(any())).thenThrow(new RuntimeException("MaxMind lookup failed"));

        Visit result = trafficService.recordVisit("session-8", "203.0.113.1", "tablet", "Safari", "/about");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getCity()).isNull();
        assertThat(result.getServiceProvider()).isNull();
        assertThat(result.getPagesViewed()).containsExactly("/about");
    }
}
