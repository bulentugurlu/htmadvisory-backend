package org.htmadvisory.platform.profile;

import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Manages demographic/firmographic detail for a Person. Unlike Engagement
 * (append-only, one record per event), a PersonProfile is updated IN PLACE
 * as more information becomes available — there is exactly one profile per
 * person, not a history of profile snapshots.
 *
 * Fields are updated only when a non-null value is provided, so a partial
 * update (e.g. Survey only asks for industry) does not wipe out fields
 * already known from an earlier interaction (e.g. company, captured at
 * registration).
 */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Creates a profile for this person if none exists yet, or updates the
     * existing one in place. Only non-null arguments overwrite existing
     * field values — passing null for a field leaves whatever was already
     * stored untouched, so callers can update just the fields they know
     * about without needing to first look up and re-supply the rest.
     */
    public PersonProfile createOrUpdateProfile(String personId, String company, String role, String industry, String companySize) {
        PersonProfile profile = profileRepository.findByPersonId(personId)
                .orElseGet(() -> new PersonProfile(personId, null, null, null, null, null));

        if (company != null) {
            profile.setCompany(company);
        }
        if (role != null) {
            profile.setRole(role);
        }
        if (industry != null) {
            profile.setIndustry(industry);
        }
        if (companySize != null) {
            profile.setCompanySize(companySize);
        }
        profile.setLastUpdatedAt(Instant.now());

        return profileRepository.save(profile);
    }

}
