package org.htmadvisory.platform.profile;

import java.time.Instant;

/**
 * Test data factory for PersonProfile, following the aXxx()/withYyy()
 * builder convention. Sensible defaults so a test only specifies what it
 * actually cares about.
 */
public class PersonProfileTestDataBuilder {

    private String personId = "default-person-id";
    private String company = "Acme Corp";
    private String role = "Engineer";
    private String industry = "Technology";
    private String companySize = "51-200";
    private Instant lastUpdatedAt = Instant.now();

    public static PersonProfileTestDataBuilder aPersonProfile() {
        return new PersonProfileTestDataBuilder();
    }

    public PersonProfileTestDataBuilder withPersonId(String personId) {
        this.personId = personId;
        return this;
    }

    public PersonProfileTestDataBuilder withCompany(String company) {
        this.company = company;
        return this;
    }

    public PersonProfileTestDataBuilder withRole(String role) {
        this.role = role;
        return this;
    }

    public PersonProfileTestDataBuilder withIndustry(String industry) {
        this.industry = industry;
        return this;
    }

    public PersonProfileTestDataBuilder withCompanySize(String companySize) {
        this.companySize = companySize;
        return this;
    }

    public PersonProfile build() {
        return new PersonProfile(personId, company, role, industry, companySize, lastUpdatedAt);
    }

}
