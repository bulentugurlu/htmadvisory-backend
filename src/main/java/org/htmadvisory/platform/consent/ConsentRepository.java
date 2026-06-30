package org.htmadvisory.platform.consent;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository extends MongoRepository<ConsentRecord, String> {

    List<ConsentRecord> findByPersonId(String personId);

    List<ConsentRecord> findByPersonIdAndConsentType(String personId, String consentType);

    /**
     * Most recent consent record for a given person and consent type —
     * this is how "current consent status" is derived, by querying for the
     * latest record rather than maintaining a separate mutable field.
     */
    Optional<ConsentRecord> findFirstByPersonIdAndConsentTypeOrderByRecordedAtDesc(String personId, String consentType);

}
