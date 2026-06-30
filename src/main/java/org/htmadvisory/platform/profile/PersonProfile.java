package org.htmadvisory.platform.profile;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Demographic and firmographic detail about a Person — company, role,
 * industry, company size. Referenced by personId, never embedded in
 * Person, so Person stays a stable, lightweight identity record regardless
 * of how much demographic detail accumulates over time.
 *
 * One profile per person, filled in progressively as more information
 * becomes available (e.g. when Survey is built and asks for role/industry).
 * See CLAUDE.md's "Marketing & Engagement Data Model" section for the full
 * reasoning behind keeping this separate from Person.
 */
@Document(collection = "profiles")
public class PersonProfile {

    @Id
    private String id;

    private String personId;

    private String company;

    private String role;

    private String industry;

    /** e.g. "1-50", "51-200", "201+" */
    private String companySize;

    private Instant lastUpdatedAt;

    public PersonProfile() {
        // Required by Spring Data MongoDB for object mapping.
    }

    public PersonProfile(String personId, String company, String role, String industry, String companySize, Instant lastUpdatedAt) {
        this.personId = personId;
        this.company = company;
        this.role = role;
        this.industry = industry;
        this.companySize = companySize;
        this.lastUpdatedAt = lastUpdatedAt;
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
