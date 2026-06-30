package org.htmadvisory.platform.consent;

import org.htmadvisory.platform.shared.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ConsentService, run against a real, throwaway
 * MongoDB instance (see AbstractMongoIntegrationTest).
 */
class ConsentServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ConsentService consentService;

    @Autowired
    private ConsentRepository consentRepository;

    @Test
    void shouldRecordANewConsentEvent() {
        ConsentRecord result = consentService.recordConsent(
                "person-1", "marketing_email", true, "contact_form");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getPersonId()).isEqualTo("person-1");
        assertThat(result.getConsentType()).isEqualTo("marketing_email");
        assertThat(result.isGranted()).isTrue();
        assertThat(result.getSource()).isEqualTo("contact_form");
        assertThat(result.getRecordedAt()).isNotNull();
    }

    @Test
    void shouldCreateANewRecordEachTime_NeverUpdateInPlace() {
        consentService.recordConsent("person-2", "marketing_email", true, "contact_form");
        consentService.recordConsent("person-2", "marketing_email", false, "account_settings");
        consentService.recordConsent("person-2", "marketing_email", true, "account_settings");

        List<ConsentRecord> allRecords = consentRepository.findByPersonIdAndConsentType("person-2", "marketing_email");

        // Three separate opt-in/opt-out events should produce three
        // separate permanent records, not one record overwritten twice.
        assertThat(allRecords).hasSize(3);
    }

    @Test
    void shouldReturnMostRecentRecordAsCurrentConsent() {
        consentService.recordConsent("person-3", "marketing_email", true, "contact_form");
        consentService.recordConsent("person-3", "marketing_email", false, "account_settings");

        Optional<ConsentRecord> current = consentService.getCurrentConsent("person-3", "marketing_email");

        assertThat(current).isPresent();
        assertThat(current.get().isGranted()).isFalse();
        assertThat(current.get().getSource()).isEqualTo("account_settings");
    }

    @Test
    void shouldReturnEmptyWhenNoConsentEverRecordedForPair() {
        Optional<ConsentRecord> current = consentService.getCurrentConsent("person-4", "sms");

        assertThat(current).isEmpty();
    }

    @Test
    void shouldTrackDifferentConsentTypesIndependentlyForSamePerson() {
        consentService.recordConsent("person-5", "marketing_email", true, "contact_form");
        consentService.recordConsent("person-5", "analytics_tracking", false, "contact_form");

        Optional<ConsentRecord> emailConsent = consentService.getCurrentConsent("person-5", "marketing_email");
        Optional<ConsentRecord> analyticsConsent = consentService.getCurrentConsent("person-5", "analytics_tracking");

        assertThat(emailConsent).isPresent();
        assertThat(emailConsent.get().isGranted()).isTrue();
        assertThat(analyticsConsent).isPresent();
        assertThat(analyticsConsent.get().isGranted()).isFalse();
    }

    @Test
    void shouldReturnFullConsentHistoryAcrossAllTypesForAPerson() {
        consentService.recordConsent("person-6", "marketing_email", true, "contact_form");
        consentService.recordConsent("person-6", "analytics_tracking", true, "contact_form");
        consentService.recordConsent("person-6", "sms", false, "account_settings");

        List<ConsentRecord> history = consentService.getConsentHistory("person-6");

        assertThat(history)
                .hasSize(3)
                .extracting(ConsentRecord::getConsentType)
                .containsExactlyInAnyOrder("marketing_email", "analytics_tracking", "sms");
    }

}
