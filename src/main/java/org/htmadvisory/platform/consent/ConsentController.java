package org.htmadvisory.platform.consent;

import jakarta.validation.Valid;
import org.htmadvisory.platform.consent.dto.EmailOptInRequest;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the {@code consent} domain's write path for the ANONYMOUS flow
 * only — a public-tab whitepaper download by someone who isn't logged in.
 * This is the endpoint {@code Whitepapers.jsx}'s lead-gate modal has been
 * calling all along; it simply never existed on the backend until now
 * (confirmed 404ing silently, non-blocking, in production).
 *
 * <p>An approved member's consent is captured exactly once, at
 * registration — see {@code UserService.register()} — and never re-asked
 * here. That's a deliberate, separate path: {@code Whitepapers.jsx} skips
 * this call entirely for a logged-in member.
 *
 * <p>Authentication is the environment-token interceptor only, same as
 * {@code ContactController} — this is a public-facing write endpoint, not
 * a member-only one.
 */
@RestController
@RequestMapping("/api/consent")
public class ConsentController {

    private final ConsentService consentService;
    private final PersonService personService;

    public ConsentController(ConsentService consentService, PersonService personService) {
        this.consentService = consentService;
        this.personService = personService;
    }

    @PostMapping("/email-opt-in")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordEmailOptIn(@RequestBody @Valid EmailOptInRequest request) {
        Person person = personService.findOrCreateByEmail(request.getEmail(), null);

        // Both recorded explicitly, true or false — matches
        // UserService.register()'s behavior. An explicit "declined" record
        // is more useful for compliance than no record at all: it proves
        // the person was actually asked, not just that we never captured
        // an answer.
        consentService.recordConsent(
                person.getId(), "communications", request.isConsentCommunications(), request.getSource());
        consentService.recordConsent(
                person.getId(), "marketing", request.isConsentMarketing(), request.getSource());
    }
}
