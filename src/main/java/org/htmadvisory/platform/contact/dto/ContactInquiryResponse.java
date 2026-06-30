package org.htmadvisory.platform.contact.dto;

import java.time.Instant;

/**
 * Response payload for {@code POST /api/contacts/inquiries}.
 * Returns the persisted inquiry's id, the resolved Person id, and the
 * timestamp — enough for the frontend to confirm the submission was recorded.
 */
public class ContactInquiryResponse {

    private String id;
    private String personId;
    private Instant submittedAt;

    public ContactInquiryResponse(String id, String personId, Instant submittedAt) {
        this.id = id;
        this.personId = personId;
        this.submittedAt = submittedAt;
    }

    public String getId() {
        return id;
    }

    public String getPersonId() {
        return personId;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
