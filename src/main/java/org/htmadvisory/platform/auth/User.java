package org.htmadvisory.platform.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A member portal account — the credentials + permission/approval state
 * layered on top of a {@code Person}.
 *
 * <p>Follows the same pattern as {@code ContactInquiry}, {@code ConsentRecord},
 * and {@code PersonProfile}: it references {@code personId} rather than
 * embedding identity fields, so the same cross-domain Person (and their full
 * engagement history — inquiries, survey responses, page visits) is
 * recognized whether they first showed up as an anonymous visitor, a contact
 * form submission, or a portal registration. {@code email} is denormalized
 * here (mirroring {@code Person.email}) purely so {@link UserRepository} can
 * look up credentials by email in one query without a join.
 *
 * <p>One {@code User} per {@code Person} — enforced by a unique index on
 * both {@code email} and {@code personId} (see Liquibase changesets
 * 014–016). A person can exist without ever registering (e.g. they only
 * submitted a contact form); not every Person has a User.
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    /** References the {@code Person} this account belongs to. */
    private String personId;

    /** Denormalized from {@code Person.email} for fast credential lookup. */
    private String email;

    /** BCrypt hash — never the plaintext password. */
    private String passwordHash;

    private UserRole role;

    private UserStatus status;

    private Instant registeredAt;

    /** Null until an admin approves the account. */
    private Instant approvedAt;

    /** Incremented on every successful password reset. A reset token
     *  embeds the version it was minted against ({@link PasswordResetTokenService}),
     *  so once used (or once a newer reset is requested), any earlier
     *  token's embedded version no longer matches and it's rejected —
     *  even though it hasn't expired yet. Starts at 0 for every account. */
    private int passwordResetTokenVersion = 0;

    public User() {
        // Required by Spring Data MongoDB for object mapping.
    }

    public User(String personId, String email, String passwordHash, UserRole role,
                UserStatus status, Instant registeredAt, Instant approvedAt) {
        this.personId = personId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.registeredAt = registeredAt;
        this.approvedAt = approvedAt;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public int getPasswordResetTokenVersion() {
        return passwordResetTokenVersion;
    }

    public void setPasswordResetTokenVersion(int passwordResetTokenVersion) {
        this.passwordResetTokenVersion = passwordResetTokenVersion;
    }
}
