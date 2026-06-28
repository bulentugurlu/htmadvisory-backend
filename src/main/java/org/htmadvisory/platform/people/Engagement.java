package org.htmadvisory.platform.people;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Records a single interaction a Person had with the platform, regardless
 * of which domain it happened in. This is what makes "what has this person
 * done across the whole platform" a single query against one collection,
 * rather than a manual cross-reference across contact/survey/news/etc.
 *
 * Written via PersonService.recordEngagement() — every domain that captures
 * an email calls this after its own domain-specific write, rather than each
 * domain inventing its own activity log. See CLAUDE.md's "Cross-Domain
 * Identity Unification" section for the full reasoning.
 */
@Document(collection = "engagements")
public class Engagement {

    @Id
    private String id;

    private String personId;

    /** e.g. "contact", "survey", "news" */
    private String domain;

    /** e.g. "inquiry_submitted", "survey_completed", "article_shared" */
    private String type;

    private Instant occurredAt;

    /** Domain-specific detail — e.g. which article was shared, survey score. */
    private Map<String, Object> metadata;

    public Engagement() {
        // Required by Spring Data MongoDB for object mapping.
    }

    public Engagement(String personId, String domain, String type, Instant occurredAt, Map<String, Object> metadata) {
        this.personId = personId;
        this.domain = domain;
        this.type = type;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
