package org.htmadvisory.platform.contact;

import jakarta.validation.Valid;
import org.htmadvisory.platform.contact.dto.ContactInquiryRequest;
import org.htmadvisory.platform.contact.dto.ContactInquiryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the {@code contact} domain's capability: submitting an inquiry.
 *
 * <p>The path {@code POST /api/contacts/inquiries} is capability-centric —
 * it describes what the caller is doing ("submit an inquiry") rather than
 * naming the database table or the UI form that triggered it. See the
 * "API Design Convention" section in ARCHITECTURE.md for the full rationale.
 *
 * <p>Authentication is handled by the environment-token interceptor
 * ({@link org.htmadvisory.platform.shared.EnvironmentTokenInterceptor}) —
 * this controller does NOT check the token itself. That separation is
 * deliberate: when the token is eventually replaced by JWT, only the
 * interceptor changes; this controller is untouched.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactInquiryResponse submitInquiry(@RequestBody @Valid ContactInquiryRequest request) {
        ContactInquiry inquiry = contactService.submitInquiry(request);
        return new ContactInquiryResponse(inquiry.getId(), inquiry.getPersonId(), inquiry.getSubmittedAt());
    }
}
