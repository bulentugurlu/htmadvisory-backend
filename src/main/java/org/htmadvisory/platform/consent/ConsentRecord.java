package org.htmadvisory.platform.consent;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A single, immutable consent event for a Person — an opt-in or opt-out
 * for a specific consent type (e.g. "marketing_email", "analytics_tracking").
 *
 * APPEND-ONLY, like Engagement, NOT update-in-place like PersonProfile:
 * every change in consent status creates a NEW record rather than modifying
 * an existing one. This is intentional and important — consent history must
 * be auditable. If a person opts in, then opts out, then opts back in, all
 * three events must remain in the collection permanently so the platform
 * can prove what consent was in effect at any point in time, not just what
 * it is right now.
 *
 * The CURRENT consent status for a given (personId, consentType) pair is
 * derived by querying for the most recent record, not by maintaining a
 * separate mutable "current state" field anywhere.
 */
@Document(collection = "consent_records")
public class ConsentRecord {

    @Id
    private String id;

    private String personId;

    /** e.g. "marketing_email", "analytics_tracking", "sms" */
    private String consentType;

    private boolean granted;

    /** When this specific consent event was recorded. */
    private Instant recordedAt;

    /** Where the consent was captured, e.g. "contact_form", "survey", "account_signup" */
    private String source;

    public ConsentRecord() {
        // Required by Spring Data MongoDB for object mapping.
    }

    public ConsentRecord(String personId, String consentType, boolean granted, Instant recordedAt, String source) {
        this.personId = personId;
        this.consentType = consentType;
        this.granted = granted;
        this.recordedAt = recordedAt;
        this.source = source;
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

    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
