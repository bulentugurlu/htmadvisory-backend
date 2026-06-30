package org.htmadvisory.platform.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound payload for {@code POST /api/contacts/inquiries}.
 *
 * {@code company} and {@code sessionId} are optional. All other fields are
 * required and validated before the request reaches {@link
 * org.htmadvisory.platform.contact.ContactService}.
 *
 * {@code sessionId} is the frontend session identifier — when present it
 * allows the service to retroactively link any anonymous {@code Visit} records
 * for that session to the {@code Person} whose email is captured here.
 */
public class ContactInquiryRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    /** Optional — not all visitors identify their company. */
    private String company;

    @NotBlank(message = "Message is required")
    private String message;

    /**
     * Optional — the frontend session id, used to link anonymous Visit records
     * to this Person once their email is known. May be null or absent.
     */
    private String sessionId;

    public ContactInquiryRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
