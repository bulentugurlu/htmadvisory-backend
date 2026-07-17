package org.htmadvisory.platform.consent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound payload for {@code POST /api/consent/email-opt-in}.
 *
 * <p>This is for the ANONYMOUS path only — a public-tab whitepaper
 * download by someone who isn't logged in. An approved member's consent is
 * captured once, at registration (see {@code UserService.register()}), and
 * never re-asked here or anywhere else.
 *
 * <p>No {@code name} field — the frontend doesn't collect one for this
 * call specifically (it's sent separately to {@code POST
 * /api/contacts/inquiries} in the same form submission). If this is truly
 * the first time we've seen this email, the resulting Person record will
 * have a null name until some other domain call fills it in.
 */
public class EmailOptInRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    private boolean consentCommunications;

    private boolean consentMarketing;

    /** e.g. "whitepaper-download" — which flow captured this consent. */
    private String source;

    public EmailOptInRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isConsentCommunications() {
        return consentCommunications;
    }

    public void setConsentCommunications(boolean consentCommunications) {
        this.consentCommunications = consentCommunications;
    }

    public boolean isConsentMarketing() {
        return consentMarketing;
    }

    public void setConsentMarketing(boolean consentMarketing) {
        this.consentMarketing = consentMarketing;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
