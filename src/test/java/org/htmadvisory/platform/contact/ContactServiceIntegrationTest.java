package org.htmadvisory.platform.contact;

import org.htmadvisory.platform.contact.dto.ContactInquiryRequest;
import org.htmadvisory.platform.people.EngagementRepository;
import org.htmadvisory.platform.people.PersonRepository;
import org.htmadvisory.platform.shared.AbstractMongoIntegrationTest;
import org.htmadvisory.platform.traffic.GeoLocationResult;
import org.htmadvisory.platform.traffic.GeoLocationService;
import org.htmadvisory.platform.traffic.VisitRepository;
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
 * Integration tests for {@link ContactService}, run against a real, throwaway
 * MongoDB instance (see {@link AbstractMongoIntegrationTest}).
 *
 * These tests exercise the full cross-domain flow end-to-end: inquiry
 * persistence, Person resolution/creation, Engagement recording, and
 * traffic session back-fill — all against a real MongoDB schema (created
 * by the same Liquibase changesets used in dev/prod).
 *
 * {@link GeoLocationService} is mocked so tests never depend on MaxMind
 * .mmdb files or network access.
 */
class ContactServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private VisitRepository visitRepository;

    @MockBean
    private GeoLocationService geoLocationService;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();
        personRepository.deleteAll();
        engagementRepository.deleteAll();
        visitRepository.deleteAll();
        when(geoLocationService.lookup(any())).thenReturn(new GeoLocationResult(null, null));
    }

    @Test
    void shouldPersistInquiryWithIdAndSubmittedAt() {
        ContactInquiry result = contactService.submitInquiry(requestFor("jane@example.com"));

        assertThat(result.getId()).isNotNull();
        assertThat(result.getPersonId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        assertThat(result.getMessage()).isEqualTo("Default test message.");
        assertThat(result.getSubmittedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldCreateNewPersonForFirstTimeEmail() {
        assertThat(personRepository.findByEmail("new@example.com")).isEmpty();

        contactService.submitInquiry(requestFor("new@example.com"));

        assertThat(personRepository.findByEmail("new@example.com")).isPresent();
    }

    @Test
    void shouldReuseExistingPersonWhenEmailIsKnown() {
        contactService.submitInquiry(requestFor("repeat@example.com"));
        String firstPersonId = contactRepository.findByEmail("repeat@example.com").get(0).getPersonId();

        contactService.submitInquiry(requestFor("repeat@example.com"));
        String secondPersonId = contactRepository.findByEmail("repeat@example.com").get(1).getPersonId();

        assertThat(firstPersonId).isEqualTo(secondPersonId);
        assertThat(personRepository.findAll()).hasSize(1);  // one Person for two inquiries
    }

    @Test
    void shouldRecordEngagementOnPersonAfterInquiry() {
        ContactInquiry inquiry = contactService.submitInquiry(requestFor("engaged@example.com"));

        assertThat(engagementRepository.findByPersonId(inquiry.getPersonId()))
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getDomain()).isEqualTo("contact");
                    assertThat(e.getType()).isEqualTo("inquiry_submitted");
                });
    }

    @Test
    void shouldBackfillPersonIdOnVisitWhenSessionIdProvided() {
        // Record an anonymous visit first
        visitRepository.save(buildAnonymousVisit("session-xyz"));

        ContactInquiryRequest request = requestFor("visitor@example.com");
        request.setSessionId("session-xyz");
        ContactInquiry inquiry = contactService.submitInquiry(request);

        // The previously anonymous visit should now carry the person id
        assertThat(visitRepository.findBySessionId("session-xyz"))
                .hasSize(1)
                .first()
                .satisfies(v -> assertThat(v.getPersonId()).isEqualTo(inquiry.getPersonId()));
    }

    @Test
    void shouldSaveInquiryWithNullSessionIdWithoutError() {
        ContactInquiryRequest request = requestFor("nosession@example.com");
        request.setSessionId(null);

        ContactInquiry result = contactService.submitInquiry(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getSessionId()).isNull();
    }

    @Test
    void shouldFindInquiriesByPersonId() {
        ContactInquiry inquiry = contactService.submitInquiry(requestFor("findme@example.com"));

        List<ContactInquiry> found = contactService.findByPersonId(inquiry.getPersonId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getEmail()).isEqualTo("findme@example.com");
    }

    @Test
    void shouldFindInquiriesByEmail() {
        contactService.submitInquiry(requestFor("search@example.com"));
        contactService.submitInquiry(requestFor("search@example.com"));

        List<ContactInquiry> found = contactService.findByEmail("search@example.com");

        assertThat(found).hasSize(2);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ContactInquiryRequest requestFor(String email) {
        ContactInquiryRequest req = new ContactInquiryRequest();
        req.setName("Test User");
        req.setEmail(email);
        req.setCompany("Test Corp");
        req.setMessage("Default test message.");
        req.setSessionId(null);
        return req;
    }

    private org.htmadvisory.platform.traffic.Visit buildAnonymousVisit(String sessionId) {
        org.htmadvisory.platform.traffic.Visit v = new org.htmadvisory.platform.traffic.Visit();
        v.setSessionId(sessionId);
        v.setIpAddress("203.0.113.1");
        v.setDeviceType("desktop");
        v.setBrowser("Chrome");
        v.setStartedAt(Instant.now());
        v.setLastActivityAt(Instant.now());
        v.setPagesViewed(java.util.List.of("/"));
        return v;
    }
}
