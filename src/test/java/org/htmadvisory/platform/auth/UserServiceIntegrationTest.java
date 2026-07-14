package org.htmadvisory.platform.auth;

import org.htmadvisory.platform.auth.dto.LoginRequest;
import org.htmadvisory.platform.auth.dto.RegisterRequest;
import org.htmadvisory.platform.auth.dto.UserProfileResponse;
import org.htmadvisory.platform.people.EngagementRepository;
import org.htmadvisory.platform.people.PersonRepository;
import org.htmadvisory.platform.profile.ProfileRepository;
import org.htmadvisory.platform.shared.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link UserService}, run against a real, throwaway
 * MongoDB instance (see {@link AbstractMongoIntegrationTest}). Exercises the
 * full register → approve → login flow end-to-end, including the
 * cross-domain Person/Profile writes.
 */
class UserServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        personRepository.deleteAll();
        profileRepository.deleteAll();
        engagementRepository.deleteAll();
    }

    @Test
    void register_shouldCreateLinkedPersonProfileAndUser() {
        User user = userService.register(registerRequest("ceo@example.com"));

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(personRepository.findByEmail("ceo@example.com")).isPresent();
        assertThat(profileRepository.findByPersonId(user.getPersonId()))
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getCompany()).isEqualTo("Acme Corp");
                    assertThat(p.getRole()).isEqualTo("CEO");
                });
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        userService.register(registerRequest("dup@example.com"));

        assertThatThrownBy(() -> userService.register(registerRequest("dup@example.com")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void register_shouldRecordEngagement() {
        User user = userService.register(registerRequest("engaged@example.com"));

        assertThat(engagementRepository.findByPersonId(user.getPersonId()))
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getDomain()).isEqualTo("auth");
                    assertThat(e.getType()).isEqualTo("registered");
                });
    }

    @Test
    void login_shouldBeRejectedWhilePending() {
        userService.register(registerRequest("pending@example.com"));

        assertThatThrownBy(() -> userService.login(loginRequest("pending@example.com")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pending approval");
    }

    @Test
    void login_shouldSucceedAfterApproval() {
        User user = userService.register(registerRequest("approve-me@example.com"));
        userService.approve(user.getId());

        UserService.LoginResult result = userService.login(loginRequest("approve-me@example.com"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().getStatus()).isEqualTo(UserStatus.APPROVED);
    }

    @Test
    void login_shouldRejectWrongPassword() {
        userService.register(registerRequest("wrongpw@example.com"));
        userService.approve(userRepository.findByEmail("wrongpw@example.com").orElseThrow().getId());

        LoginRequest badLogin = loginRequest("wrongpw@example.com");
        badLogin.setPassword("not-the-right-password");

        assertThatThrownBy(() -> userService.login(badLogin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void approve_shouldMovePendingUserToApproved() {
        User user = userService.register(registerRequest("toapprove@example.com"));

        UserProfileResponse approved = userService.approve(user.getId());

        assertThat(approved.getStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(approved.getApprovedAt()).isNotNull();
    }

    @Test
    void listUsers_shouldFilterByPendingStatusByDefault() {
        User pending = userService.register(registerRequest("stillpending@example.com"));
        User toApprove = userService.register(registerRequest("willbeapproved@example.com"));
        userService.approve(toApprove.getId());

        List<UserProfileResponse> pendingUsers = userService.listUsers(Optional.of(UserStatus.PENDING));

        assertThat(pendingUsers).extracting(UserProfileResponse::getId).containsExactly(pending.getId());
    }

    @Test
    void getProfile_shouldJoinNameCompanyAndTitle() {
        User user = userService.register(registerRequest("joined@example.com"));

        UserProfileResponse profile = userService.getProfile(user.getId());

        assertThat(profile.getName()).isEqualTo("Test CEO");
        assertThat(profile.getEmail()).isEqualTo("joined@example.com");
        assertThat(profile.getCompany()).isEqualTo("Acme Corp");
        assertThat(profile.getTitle()).isEqualTo("CEO");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private RegisterRequest registerRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test CEO");
        req.setEmail(email);
        req.setCompany("Acme Corp");
        req.setTitle("CEO");
        req.setPassword("password123");
        return req;
    }

    private LoginRequest loginRequest(String email) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("password123");
        return req;
    }
}
