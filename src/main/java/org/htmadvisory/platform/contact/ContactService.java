package org.htmadvisory.platform.contact;

import org.htmadvisory.platform.contact.dto.ContactInquiryRequest;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonService;
import org.htmadvisory.platform.traffic.TrafficService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Handles the "submit an inquiry" capability ({@code POST /api/contacts/inquiries}).
 *
 * <p>Every inquiry submission follows the four-step cross-domain pattern:
 * <ol>
 *   <li>Resolve or create identity via {@code PersonService.findOrCreateByEmail()} —
 *       the same email submitted here, in a survey, or in an article share
 *       resolves to exactly one {@code Person}, making a joined engagement
 *       history possible without manual cross-referencing.</li>
 *   <li>Persist the {@link ContactInquiry} to the {@code contacts} collection.</li>
 *   <li>Record a cross-domain engagement event so the inquiry appears in the
 *       person's full activity history alongside anything else they've done on
 *       the platform.</li>
 *   <li>Back-fill the session → person link if the request includes a
 *       {@code sessionId}, so anonymous {@code Visit} records from before the
 *       email was known are retroactively attributed to this person.</li>
 * </ol>
 *
 * <p>This is the canonical example of how every future email-capturing domain
 * ({@code survey}, {@code news}, etc.) should be implemented — follow this
 * same four-step sequence.
 */
@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final PersonService personService;
    private final TrafficService trafficService;

    public ContactService(ContactRepository contactRepository,
                          PersonService personService,
                          TrafficService trafficService) {
        this.contactRepository = contactRepository;
        this.personService = personService;
        this.trafficService = trafficService;
    }

    /**
     * Persists a contact inquiry, resolving or creating a {@code Person} for
     * the submitted email, recording a cross-domain engagement event, and
     * retroactively linking any anonymous session visits if a session id was
     * included in the request.
     */
    public ContactInquiry submitInquiry(ContactInquiryRequest request) {
        // Step 1 — identity
        Person person = personService.findOrCreateByEmail(request.getEmail(), request.getName());

        // Step 2 — persist the inquiry (before recordEngagement so the id is available for metadata)
        ContactInquiry inquiry = new ContactInquiry(
                person.getId(), request.getName(), request.getEmail(),
                request.getCompany(), request.getMessage(),
                request.getSessionId(), Instant.now());
        contactRepository.save(inquiry);

        // Step 3 — cross-domain engagement
        personService.recordEngagement(
                person.getId(), "contact", "inquiry_submitted",
                Map.of("inquiryId", inquiry.getId()));

        // Step 4 — back-fill anonymous session visits (if session id is known)
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            trafficService.backfillPersonId(request.getSessionId(), person.getId());
        }

        return inquiry;
    }

    public List<ContactInquiry> findByPersonId(String personId) {
        return contactRepository.findByPersonId(personId);
    }

    public List<ContactInquiry> findByEmail(String email) {
        return contactRepository.findByEmail(email);
    }
}
