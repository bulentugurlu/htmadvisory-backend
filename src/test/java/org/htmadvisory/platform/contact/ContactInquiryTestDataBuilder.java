package org.htmadvisory.platform.contact;

import java.time.Instant;

/**
 * Test data factory for {@link ContactInquiry}, following the
 * {@code aXxx().withYyy().build()} builder convention used across all domains.
 *
 * Sensible defaults are provided for every field so tests only specify what
 * they actually care about.
 */
public class ContactInquiryTestDataBuilder {

    private String personId = "default-person-id";
    private String name = "Jane Doe";
    private String email = "jane@example.com";
    private String company = "Acme Corp";
    private String message = "I'd like to discuss a potential engagement.";
    private String sessionId = null;
    private Instant submittedAt = Instant.now();

    public static ContactInquiryTestDataBuilder aContactInquiry() {
        return new ContactInquiryTestDataBuilder();
    }

    public ContactInquiryTestDataBuilder withPersonId(String personId) {
        this.personId = personId;
        return this;
    }

    public ContactInquiryTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ContactInquiryTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public ContactInquiryTestDataBuilder withCompany(String company) {
        this.company = company;
        return this;
    }

    public ContactInquiryTestDataBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    public ContactInquiryTestDataBuilder withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public ContactInquiry build() {
        return new ContactInquiry(personId, name, email, company, message, sessionId, submittedAt);
    }
}
