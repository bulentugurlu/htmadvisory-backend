package org.htmadvisory.platform.traffic;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Manages anonymous-until-matched website session tracking.
 *
 * <p>A {@link Visit} is created the moment someone lands on the site with
 * {@code personId} null — there is no email yet, so there is nothing to link
 * to. If, later in that same session (or a return visit using the same
 * {@code sessionId}), the visitor submits the Contact form, takes the Survey,
 * or shares an article, the calling domain's service must invoke
 * {@link #backfillPersonId(String, String)} to stamp the now-known
 * {@code personId} onto the Visit records for that session.
 *
 * <p><strong>Calling convention every email-capturing domain must follow:</strong>
 * <pre>{@code
 * Person person = personService.findOrCreateByEmail(email, name);
 * trafficService.backfillPersonId(sessionId, person.getId());
 * }</pre>
 * This keeps {@code PersonService} completely clean — no {@code TrafficService}
 * dependency injected into the foundational identity service, which would
 * create an architecturally backward {@code people → traffic} dependency.
 *
 * <p><strong>Session continuity:</strong> a second {@link #recordVisit} call
 * for the same {@code sessionId} appends to the existing Visit if it was active
 * within the last {@value #SESSION_TIMEOUT_MINUTES} minutes. Beyond that, a new
 * Visit record is created — matching the industry-standard session timeout used
 * by most analytics tools (Google Analytics, Mixpanel, etc.) and giving a clean
 * boundary for "one visit" without requiring client-side session re-generation.
 */
@Service
public class TrafficService {

    private static final int SESSION_TIMEOUT_MINUTES = 30;
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(SESSION_TIMEOUT_MINUTES);

    private final VisitRepository visitRepository;
    private final GeoLocationService geoLocationService;

    public TrafficService(VisitRepository visitRepository, GeoLocationService geoLocationService) {
        this.visitRepository = visitRepository;
        this.geoLocationService = geoLocationService;
    }

    /**
     * Records a page view for the given session. Creates a new {@link Visit} if
     * no active Visit exists for this {@code sessionId}, or appends {@code page}
     * to the most recent Visit if it was active within the last
     * {@value #SESSION_TIMEOUT_MINUTES} minutes.
     *
     * <p>Geolocation lookup is best-effort: if the lookup fails or returns null,
     * the Visit is still saved with null {@code city}/{@code serviceProvider} —
     * the write is never blocked by a geolocation failure.
     */
    public Visit recordVisit(String sessionId, String ipAddress, String deviceType,
                             String browser, String page) {
        Instant now = Instant.now();

        Optional<Visit> activeVisit = findMostRecentActiveVisit(sessionId, now);
        if (activeVisit.isPresent()) {
            Visit visit = activeVisit.get();
            visit.getPagesViewed().add(page);
            visit.setLastActivityAt(now);
            return visitRepository.save(visit);
        }

        GeoLocationResult geo = lookupSafely(ipAddress);
        Visit newVisit = new Visit(
                sessionId, ipAddress,
                geo != null ? geo.getCity() : null,
                geo != null ? geo.getServiceProvider() : null,
                deviceType, browser, now, page);
        return visitRepository.save(newVisit);
    }

    /**
     * Retroactively stamps {@code personId} onto all {@link Visit} records for
     * the given {@code sessionId} where {@code personId} is currently null.
     *
     * <p>Called by domain services (ContactService, SurveyService, etc.)
     * immediately after {@code personService.findOrCreateByEmail()} returns,
     * when a {@code sessionId} is available from the HTTP request context.
     *
     * @param sessionId the session whose anonymous Visit records should be linked
     * @param personId  the now-known Person to link them to
     */
    public void backfillPersonId(String sessionId, String personId) {
        List<Visit> visits = visitRepository.findBySessionId(sessionId);
        List<Visit> anonymous = visits.stream()
                .filter(v -> v.getPersonId() == null)
                .toList();
        anonymous.forEach(v -> v.setPersonId(personId));
        visitRepository.saveAll(anonymous);
    }

    public List<Visit> findBySessionId(String sessionId) {
        return visitRepository.findBySessionId(sessionId);
    }

    public List<Visit> findByPersonId(String personId) {
        return visitRepository.findByPersonId(personId);
    }

    public List<Visit> findByDateRange(Instant start, Instant end) {
        return visitRepository.findByStartedAtBetween(start, end);
    }

    private Optional<Visit> findMostRecentActiveVisit(String sessionId, Instant now) {
        return visitRepository.findBySessionId(sessionId).stream()
                .filter(v -> v.getLastActivityAt() != null &&
                             Duration.between(v.getLastActivityAt(), now).compareTo(SESSION_TIMEOUT) <= 0)
                .max((a, b) -> a.getLastActivityAt().compareTo(b.getLastActivityAt()));
    }

    private GeoLocationResult lookupSafely(String ipAddress) {
        try {
            return geoLocationService.lookup(ipAddress);
        } catch (Exception e) {
            return null;
        }
    }
}
