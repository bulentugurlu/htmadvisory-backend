package org.htmadvisory.platform.contact;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A persisted record of a contact form submission.
 *
 * Every {@code ContactInquiry} is linked to a {@code Person} via
 * {@code personId} — the cross-domain identity anchor that makes it possible
 * to query "all inquiries from this person" alongside their survey results,
 * article shares, and visit history in one place. See the {@code people}
 * domain's {@code PersonService.findOrCreateByEmail()} for how the link is
 * established: the same email submitted twice (across any domain, in any
 * order) will resolve to the same {@code Person}.
 *
 * {@code sessionId} is nullable: it is populated when the contact form
 * submission includes a session identifier from the frontend, allowing
 * {@link ContactService} to retroactively link the anonymous {@code Visit}
 * records for that session to this person via {@code TrafficService.backfillPersonId()}.
 */
@Document(collection = "contacts")
public class ContactInquiry {

    @Id
    private String id;

    /** References the {@code Person} who submitted this inquiry. */
    private String personId;

    private String name;

    private String email;

    /** Optional — not all visitors identify their company. */
    private String company;

    private String message;

    /**
     * Optional — populated when the submission includes the frontend session id,
     * enabling retroactive linking of anonymous {@code Visit} records.
     */
    private String sessionId;

    private Instant submittedAt;

    public ContactInquiry() {
        // Required by Spring Data MongoDB for object mapping.
    }

    public ContactInquiry(String personId, String name, String email, String company,
                          String message, String sessionId, Instant submittedAt) {
        this.personId = personId;
        this.name = name;
        this.email = email;
        this.company = company;
        this.message = message;
        this.sessionId = sessionId;
        this.submittedAt = submittedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
