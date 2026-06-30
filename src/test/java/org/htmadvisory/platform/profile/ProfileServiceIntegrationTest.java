package org.htmadvisory.platform.profile;

import org.htmadvisory.platform.shared.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ProfileService, run against a real, throwaway
 * MongoDB instance (see AbstractMongoIntegrationTest).
 */
class ProfileServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void shouldCreateNewProfileWhenNoneExistsForPerson() {
        PersonProfile result = profileService.createOrUpdateProfile(
                "person-1", "Acme Corp", "Engineer", "Technology", "51-200");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getPersonId()).isEqualTo("person-1");
        assertThat(result.getCompany()).isEqualTo("Acme Corp");
        assertThat(result.getRole()).isEqualTo("Engineer");
        assertThat(result.getIndustry()).isEqualTo("Technology");
        assertThat(result.getCompanySize()).isEqualTo("51-200");
        assertThat(result.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateExistingProfileInPlace_NotCreateASecondOne() {
        profileService.createOrUpdateProfile("person-2", "Acme Corp", "Engineer", "Technology", "51-200");

        profileService.createOrUpdateProfile("person-2", "Acme Corp", "Senior Engineer", "Technology", "51-200");

        assertThat(profileRepository.findAll())
                .filteredOn(p -> p.getPersonId().equals("person-2"))
                .hasSize(1)
                .first()
                .satisfies(p -> assertThat(p.getRole()).isEqualTo("Senior Engineer"));
    }

    @Test
    void shouldLeaveExistingFieldsUntouched_WhenLaterUpdateOnlyProvidesSomeFields() {
        profileService.createOrUpdateProfile("person-3", "Acme Corp", "Engineer", "Technology", "51-200");

        // Survey only asks for industry — company/role/companySize should
        // remain exactly as they were from the first call.
        PersonProfile updated = profileService.createOrUpdateProfile("person-3", null, null, "Finance", null);

        assertThat(updated.getCompany()).isEqualTo("Acme Corp");
        assertThat(updated.getRole()).isEqualTo("Engineer");
        assertThat(updated.getIndustry()).isEqualTo("Finance");
        assertThat(updated.getCompanySize()).isEqualTo("51-200");
    }

    @Test
    void shouldAllowDifferentPeopleToHaveIndependentProfiles() {
        profileService.createOrUpdateProfile("person-4", "Company A", "Engineer", "Technology", "1-50");
        profileService.createOrUpdateProfile("person-5", "Company B", "Manager", "Finance", "201+");

        PersonProfile profile4 = profileRepository.findByPersonId("person-4").orElseThrow();
        PersonProfile profile5 = profileRepository.findByPersonId("person-5").orElseThrow();

        assertThat(profile4.getCompany()).isEqualTo("Company A");
        assertThat(profile5.getCompany()).isEqualTo("Company B");
    }

}
