package org.htmadvisory.platform.auth.dto;

import org.htmadvisory.platform.auth.User;
import org.htmadvisory.platform.auth.UserRole;
import org.htmadvisory.platform.auth.UserStatus;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.profile.PersonProfile;

import java.time.Instant;

/**
 * The outward-facing view of a member account — joins {@link User} with its
 * {@code Person} (name) and {@code PersonProfile} (company/title), and never
 * includes {@code passwordHash}. Used for {@code GET /api/auth/me}, the
 * {@code user} field of {@code POST /api/auth/login}, and the admin user
 * list/approve endpoints.
 */
public class UserProfileResponse {

    private String id;
    private String name;
    private String email;
    private String company;
    private String title;
    private UserRole role;
    private UserStatus status;
    private Instant registeredAt;
    private Instant approvedAt;

    public UserProfileResponse(String id, String name, String email, String company, String title,
                                UserRole role, UserStatus status, Instant registeredAt, Instant approvedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.company = company;
        this.title = title;
        this.role = role;
        this.status = status;
        this.registeredAt = registeredAt;
        this.approvedAt = approvedAt;
    }

    /**
     * Builds the response from the three source records. {@code person} and
     * {@code profile} may be null defensively (e.g. data inconsistency), in
     * which case name/company/title fall back to null rather than throwing.
     */
    public static UserProfileResponse from(User user, Person person, PersonProfile profile) {
        return new UserProfileResponse(
                user.getId(),
                person != null ? person.getName() : null,
                user.getEmail(),
                profile != null ? profile.getCompany() : null,
                profile != null ? profile.getRole() : null,
                user.getRole(),
                user.getStatus(),
                user.getRegisteredAt(),
                user.getApprovedAt());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }
}
