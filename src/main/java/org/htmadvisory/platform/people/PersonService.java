package org.htmadvisory.platform.people;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * The single entry point every other domain uses to interact with identity.
 * Contact, Survey, News (and eventually Account/Commerce) call
 * findOrCreateByEmail() and recordEngagement() rather than each implementing
 * their own notion of "who is this" — see CLAUDE.md's "Cross-Domain Identity
 * Unification" section for the full reasoning behind this pattern.
 */
@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final EngagementRepository engagementRepository;

    public PersonService(PersonRepository personRepository, EngagementRepository engagementRepository) {
        this.personRepository = personRepository;
        this.engagementRepository = engagementRepository;
    }

    /**
     * Returns the existing Person for this email if one already exists
     * (from any prior interaction, in any domain), or creates a new one.
     * This is what lets someone who took the Survey months ago be
     * recognized as the same person when they later submit a Contact
     * inquiry — there is exactly one Person record per email, regardless
     * of how many different domains have interacted with them.
     */
    public Person findOrCreateByEmail(String email, String name) {
        Instant now = Instant.now();

        return personRepository.findByEmail(email)
                .map(existing -> touchLastSeen(existing, now))
                .orElseGet(() -> createNewPerson(email, name, now));
    }

    private Person touchLastSeen(Person existing, Instant now) {
        existing.setLastSeenAt(now);
        return personRepository.save(existing);
    }

    private Person createNewPerson(String email, String name, Instant now) {
        Person newPerson = new Person(email, name, now, now);
        return personRepository.save(newPerson);
    }

    /**
     * Records that a Person did something, in a given domain. Called by
     * every domain-specific service (ContactService, SurveyService, etc.)
     * immediately after its own domain-specific write, so the full
     * cross-domain activity history accumulates in one place.
     */
    public Engagement recordEngagement(String personId, String domain, String type, Map<String, Object> metadata) {
        Engagement engagement = new Engagement(personId, domain, type, Instant.now(), metadata);
        return engagementRepository.save(engagement);
    }

}
