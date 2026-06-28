package org.htmadvisory.platform.people;

import java.time.Instant;

/**
 * Test data factory for Person, following the aXxx()/withYyy() builder
 * convention locked in CLAUDE.md's Automated Testing Strategy. Sensible
 * defaults on every field so a test only specifies what it actually cares
 * about. Reused across unit, integration, AND (later) Cucumber step
 * definitions — never duplicated per test layer.
 */
public class PersonTestDataBuilder {

    private String email = "jane@example.com";
    private String name = "Jane Doe";
    private Instant firstSeenAt = Instant.now();
    private Instant lastSeenAt = Instant.now();

    public static PersonTestDataBuilder aPerson() {
        return new PersonTestDataBuilder();
    }

    public PersonTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public PersonTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public PersonTestDataBuilder withFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
        return this;
    }

    public PersonTestDataBuilder withLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
        return this;
    }

    public Person build() {
        return new Person(email, name, firstSeenAt, lastSeenAt);
    }

}
