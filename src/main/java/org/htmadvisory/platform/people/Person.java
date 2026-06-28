package org.htmadvisory.platform.people;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * The platform's identity anchor. Every domain that captures an email
 * address (contact, survey, news article-shares, and eventually
 * account/commerce) references a Person by id rather than owning its own
 * notion of who that person is.
 *
 * Deliberately lightweight — demographics live in the separate `profile`
 * domain, consent history in `consent`, and traffic/session data in
 * `traffic`, all referencing this id. See CLAUDE.md's "Cross-Domain Identity
 * Unification" and "Marketing & Engagement Data Model" sections for the
 * full reasoning on why this stays minimal rather than accumulating fields.
 */
@Document(collection = "people")
public class Person {

    @Id
    private String id;

    private String email;

    private String name;

    private Instant firstSeenAt;

    private Instant lastSeenAt;

    public Person() {
        // Required by Spring Data MongoDB for object mapping.
    }

    public Person(String email, String name, Instant firstSeenAt, Instant lastSeenAt) {
        this.email = email;
        this.name = name;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
