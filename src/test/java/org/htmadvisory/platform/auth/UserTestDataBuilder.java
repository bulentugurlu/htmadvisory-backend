package org.htmadvisory.platform.auth;

import java.time.Instant;

/**
 * Test data factory for {@link User}, following the {@code aXxx().withYyy().build()}
 * builder convention used across all domains.
 */
public class UserTestDataBuilder {

    private String personId = "default-person-id";
    private String email = "jane@example.com";
    // BCrypt hash of "password123", precomputed so tests don't pay the hashing cost repeatedly.
    private String passwordHash = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5L5J0Q0V6C6z6f1v2i3u4t5r6y7u8";
    private UserRole role = UserRole.MEMBER;
    private UserStatus status = UserStatus.PENDING;
    private Instant registeredAt = Instant.now();
    private Instant approvedAt = null;

    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    public UserTestDataBuilder withPersonId(String personId) {
        this.personId = personId;
        return this;
    }

    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestDataBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserTestDataBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserTestDataBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UserTestDataBuilder withApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
        return this;
    }

    public User build() {
        return new User(personId, email, passwordHash, role, status, registeredAt, approvedAt);
    }
}
