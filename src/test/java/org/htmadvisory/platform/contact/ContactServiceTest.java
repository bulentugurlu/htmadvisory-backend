package org.htmadvisory.platform.contact;

import org.htmadvisory.platform.contact.dto.ContactInquiryRequest;
import org.htmadvisory.platform.people.Engagement;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonService;
import org.htmadvisory.platform.traffic.TrafficService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContactService} — no Spring context, no database.
 * All collaborators (repository, PersonService, TrafficService) are Mockito mocks.
 *
 * These tests verify the service's coordination logic: correct delegation to each
 * collaborator, correct argument values, and correct conditional behaviour (e.g.
 * backfill is only called when a sessionId is present).
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private PersonService personService;

    @Mock
    private TrafficService trafficService;

    @InjectMocks
    private ContactService contactService;

    private Person mockPerson;
    private ContactInquiryRequest validRequest;

    @BeforeEach
    void setUp() {
        mockPerson = new Person("jane@example.com", "Jane Doe", Instant.now(), Instant.now());
        mockPerson.setId("person-1");

        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(contactRepository.save(any())).thenAnswer(inv -> {
            ContactInquiry c = inv.getArgument(0);
            c.setId("inquiry-1");
            return c;
        });

        validRequest = new ContactInquiryRequest();
        validRequest.setName("Jane Doe");
        validRequest.setEmail("jane@example.com");
        validRequest.setCompany("Acme Corp");
        validRequest.setMessage("I'd like to discuss a potential engagement.");
        validRequest.setSessionId("session-abc");
    }

    @Test
    void shouldCallFindOrCreateByEmailWithCorrectEmailAndName() {
        contactService.submitInquiry(validRequest);

        verify(personService).findOrCreateByEmail("jane@example.com", "Jane Doe");
    }

    @Test
    void shouldPersistInquiryLinkedToResolvedPerson() {
        contactService.submitInquiry(validRequest);

        verify(contactRepository).save(argThat(inquiry ->
                "person-1".equals(inquiry.getPersonId()) &&
                "jane@example.com".equals(inquiry.getEmail()) &&
                "I'd like to discuss a potential engagement.".equals(inquiry.getMessage()) &&
                "Acme Corp".equals(inquiry.getCompany())));
    }

    @Test
    void shouldRecordEngagementWithCorrectDomainAndType() {
        contactService.submitInquiry(validRequest);

        verify(personService).recordEngagement(
                eq("person-1"), eq("contact"), eq("inquiry_submitted"), any(Map.class));
    }

    @Test
    void shouldBackfillPersonIdOnSessionWhenSessionIdIsPresent() {
        contactService.submitInquiry(validRequest);  // validRequest has sessionId = "session-abc"

        verify(trafficService).backfillPersonId("session-abc", "person-1");
    }

    @Test
    void shouldNotBackfillWhenSessionIdIsNull() {
        validRequest.setSessionId(null);

        contactService.submitInquiry(validRequest);

        verify(trafficService, never()).backfillPersonId(any(), any());
    }

    @Test
    void shouldNotBackfillWhenSessionIdIsBlank() {
        validRequest.setSessionId("   ");

        contactService.submitInquiry(validRequest);

        verify(trafficService, never()).backfillPersonId(any(), any());
    }

    @Test
    void shouldReturnPersistedInquiryWithId() {
        ContactInquiry result = contactService.submitInquiry(validRequest);

        assertThat(result.getId()).isEqualTo("inquiry-1");
        assertThat(result.getPersonId()).isEqualTo("person-1");
    }
}
