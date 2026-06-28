package org.htmadvisory.platform.people;

import org.htmadvisory.platform.shared.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for PersonService, run against a real, throwaway
 * MongoDB instance (see AbstractMongoIntegrationTest). These are the tests
 * that should have existed BEFORE the people domain was built — written
 * now to close that gap, per CLAUDE.md's flagged sequencing deviation.
 */
class PersonServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private PersonService personService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Test
    void shouldCreateNewPersonWhenEmailNotSeenBefore() {
        Person result = personService.findOrCreateByEmail("new.visitor@example.com", "New Visitor");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("new.visitor@example.com");
        assertThat(result.getName()).isEqualTo("New Visitor");
        assertThat(result.getFirstSeenAt()).isNotNull();
        assertThat(result.getLastSeenAt()).isNotNull();
    }

    @Test
    void shouldReturnSameExistingPersonWhenEmailAlreadySeen_NotADuplicate() {
        Person firstCall = personService.findOrCreateByEmail("returning.visitor@example.com", "Returning Visitor");

        Person secondCall = personService.findOrCreateByEmail("returning.visitor@example.com", "Returning Visitor");

        assertThat(secondCall.getId()).isEqualTo(firstCall.getId());
        assertThat(personRepository.findAll())
                .filteredOn(p -> p.getEmail().equals("returning.visitor@example.com"))
                .hasSize(1);
    }

    @Test
    void shouldUpdateLastSeenAtWithoutChangingFirstSeenAt_OnReturningVisitor() {
        Person firstCall = personService.findOrCreateByEmail("repeat.visitor@example.com", "Repeat Visitor");

        Person secondCall = personService.findOrCreateByEmail("repeat.visitor@example.com", "Repeat Visitor");

        assertThat(secondCall.getFirstSeenAt()).isCloseTo(firstCall.getFirstSeenAt(), within(1, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(secondCall.getLastSeenAt()).isAfterOrEqualTo(firstCall.getLastSeenAt());
    }

    @Test
    void shouldRecordEngagementLinkedToCorrectPerson() {
        Person person = personService.findOrCreateByEmail("engaged.visitor@example.com", "Engaged Visitor");

        Engagement engagement = personService.recordEngagement(
                person.getId(), "contact", "inquiry_submitted", Map.of("source", "homepage"));

        assertThat(engagement.getId()).isNotNull();
        assertThat(engagement.getPersonId()).isEqualTo(person.getId());
        assertThat(engagement.getDomain()).isEqualTo("contact");
        assertThat(engagement.getType()).isEqualTo("inquiry_submitted");
        assertThat(engagement.getOccurredAt()).isNotNull();
        assertThat(engagement.getMetadata()).containsEntry("source", "homepage");

        assertThat(engagementRepository.findByPersonId(person.getId())).hasSize(1);
    }

    @Test
    void shouldAccumulateMultipleEngagementsAcrossDifferentDomains_ForSamePerson() {
        Person person = personService.findOrCreateByEmail("multi.domain.visitor@example.com", "Multi Domain Visitor");

        personService.recordEngagement(person.getId(), "contact", "inquiry_submitted", Map.of());
        personService.recordEngagement(person.getId(), "survey", "survey_completed", Map.of("score", 72));
        personService.recordEngagement(person.getId(), "news", "article_shared", Map.of("articleId", "abc-123"));

        assertThat(engagementRepository.findByPersonId(person.getId()))
                .hasSize(3)
                .extracting(Engagement::getDomain)
                .containsExactlyInAnyOrder("contact", "survey", "news");
    }

}
