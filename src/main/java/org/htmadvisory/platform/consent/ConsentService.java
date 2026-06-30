package org.htmadvisory.platform.consent;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Manages consent history for a Person. Unlike ProfileService, this NEVER
 * updates a record in place — every call to recordConsent() creates a brand
 * new ConsentRecord, even if it's recording the same consentType as before.
 * This preserves a full, permanent audit trail of consent changes over time.
 */
@Service
public class ConsentService {

    private final ConsentRepository consentRepository;

    public ConsentService(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    /**
     * Records a new consent event. Always inserts a new record — never
     * updates an existing one, even if the same personId/consentType pair
     * already has prior records. This is what makes the consent history
     * auditable: a person opting in, then out, then back in produces three
     * permanent records, not one record overwritten twice.
     */
    public ConsentRecord recordConsent(String personId, String consentType, boolean granted, String source) {
        ConsentRecord record = new ConsentRecord(personId, consentType, granted, Instant.now(), source);
        return consentRepository.save(record);
    }

    /**
     * The current consent status for a given person and consent type,
     * derived from the most recent record. Returns empty if no consent
     * event has ever been recorded for this pair — callers should treat
     * "no record" as "not granted" (the safe default), not as an error.
     */
    public Optional<ConsentRecord> getCurrentConsent(String personId, String consentType) {
        return consentRepository.findFirstByPersonIdAndConsentTypeOrderByRecordedAtDesc(personId, consentType);
    }

    /**
     * Full consent history for a person, across all consent types — useful
     * for compliance/audit views and for the privacy/data-export domain
     * planned later in the build order.
     */
    public List<ConsentRecord> getConsentHistory(String personId) {
        return consentRepository.findByPersonId(personId);
    }

}
