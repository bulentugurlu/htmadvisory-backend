package org.htmadvisory.platform.consent;

import java.time.Instant;

/**
 * Test data factory for ConsentRecord, following the aXxx()/withYyy()
 * builder convention.
 */
public class ConsentRecordTestDataBuilder {

    private String personId = "default-person-id";
    private String consentType = "marketing_email";
    private boolean granted = true;
    private Instant recordedAt = Instant.now();
    private String source = "contact_form";

    public static ConsentRecordTestDataBuilder aConsentRecord() {
        return new ConsentRecordTestDataBuilder();
    }

    public ConsentRecordTestDataBuilder withPersonId(String personId) {
        this.personId = personId;
        return this;
    }

    public ConsentRecordTestDataBuilder withConsentType(String consentType) {
        this.consentType = consentType;
        return this;
    }

    public ConsentRecordTestDataBuilder withGranted(boolean granted) {
        this.granted = granted;
        return this;
    }

    public ConsentRecordTestDataBuilder withSource(String source) {
        this.source = source;
        return this;
    }

    public ConsentRecord build() {
        return new ConsentRecord(personId, consentType, granted, recordedAt, source);
    }

}
