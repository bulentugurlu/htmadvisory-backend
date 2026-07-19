package org.htmadvisory.platform.auth;

import org.htmadvisory.platform.auth.dto.ForgotPasswordRequest;
import org.htmadvisory.platform.auth.dto.ForgotPasswordResponse;
import org.htmadvisory.platform.auth.dto.LoginRequest;
import org.htmadvisory.platform.auth.dto.RegisterRequest;
import org.htmadvisory.platform.auth.dto.ResetPasswordRequest;
import org.htmadvisory.platform.consent.ConsentService;
import io.jsonwebtoken.JwtException;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonRepository;
import org.htmadvisory.platform.people.PersonService;
import org.htmadvisory.platform.profile.ProfileRepository;
import org.htmadvisory.platform.profile.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService} — no Spring context, no database. All
 * collaborators are Mockito mocks, following the same pattern as {@code
 * ContactServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PersonService personService;

    @Mock
    private ProfileService profileService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private ConsentService consentService;

    @InjectMocks
    private UserService userService;

    private Person mockPerson;
    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        mockPerson = new Person("jane@example.com", "Jane Doe", Instant.now(), Instant.now());
        mockPerson.setId("person-1");

        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setName("Jane Doe");
        validRegisterRequest.setEmail("jane@example.com");
        validRegisterRequest.setCompany("Acme Corp");
        validRegisterRequest.setTitle("CEO");
        validRegisterRequest.setPassword("password123");
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    void register_shouldRejectDuplicateEmail() {
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(UserTestDataBuilder.aUser().build()));

        assertThatThrownBy(() -> userService.register(validRegisterRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(personService, never()).findOrCreateByEmail(any(), any());
    }

    @Test
    void register_shouldResolvePersonWithCorrectEmailAndName() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });

        userService.register(validRegisterRequest);

        verify(personService).findOrCreateByEmail("jane@example.com", "Jane Doe");
    }

    @Test
    void register_shouldCreateProfileWithCompanyAndTitle() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });

        userService.register(validRegisterRequest);

        verify(profileService).createOrUpdateProfile("person-1", "Acme Corp", "CEO", null, null);
    }

    @Test
    void register_shouldSaveUserAsPendingWithHashedPassword() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });

        User result = userService.register(validRegisterRequest);

        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(result.getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(result.getPersonId()).isEqualTo("person-1");
        assertThat(result.getPasswordHash()).isNotEqualTo("password123");
        assertThat(new BCryptPasswordEncoder().matches("password123", result.getPasswordHash())).isTrue();
    }

    @Test
    void register_shouldRecordEngagementAfterCreatingUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });

        userService.register(validRegisterRequest);

        verify(personService).recordEngagement(
                eq("person-1"), eq("auth"), eq("registered"), any(Map.class));
    }

    @Test
    void register_shouldRecordBothConsentTypes() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        validRegisterRequest.setConsentMarketing(true);

        userService.register(validRegisterRequest);

        // Communications consent is always true — implied by submitting the
        // form at all, per the mandatory checkbox on the frontend.
        verify(consentService).recordConsent("person-1", "communications", true, "account_registration");
        // Marketing reflects whatever the checkbox was actually set to.
        verify(consentService).recordConsent("person-1", "marketing", true, "account_registration");
    }

    @Test
    void register_shouldRecordMarketingConsentAsFalseWhenNotOptedIn() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personService.findOrCreateByEmail(anyString(), anyString())).thenReturn(mockPerson);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        // validRegisterRequest.consentMarketing defaults to false — not set here.

        userService.register(validRegisterRequest);

        verify(consentService).recordConsent("person-1", "marketing", false, "account_registration");
    }

    // ── login ────────────────────────────────────────────────────────────

    @Test
    void login_shouldThrowUnauthorizedWhenEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(loginRequest("missing@example.com", "anything")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_shouldThrowUnauthorizedWhenPasswordWrong() {
        String realHash = new BCryptPasswordEncoder().encode("correct-password");
        User user = UserTestDataBuilder.aUser().withStatus(UserStatus.APPROVED).withPasswordHash(realHash).build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(loginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_shouldThrowForbiddenWhenPending() {
        String realHash = new BCryptPasswordEncoder().encode("password123");
        User user = UserTestDataBuilder.aUser().withStatus(UserStatus.PENDING).withPasswordHash(realHash).build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(loginRequest("jane@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pending approval");
    }

    @Test
    void login_shouldThrowForbiddenWhenRejected() {
        String realHash = new BCryptPasswordEncoder().encode("password123");
        User user = UserTestDataBuilder.aUser().withStatus(UserStatus.REJECTED).withPasswordHash(realHash).build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(loginRequest("jane@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void login_shouldReturnTokenAndUserWhenApproved() {
        String realHash = new BCryptPasswordEncoder().encode("password123");
        User user = UserTestDataBuilder.aUser().withStatus(UserStatus.APPROVED).withPasswordHash(realHash).build();
        user.setId("user-1");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed-jwt-token");

        UserService.LoginResult result = userService.login(loginRequest("jane@example.com", "password123"));

        assertThat(result.token()).isEqualTo("signed-jwt-token");
        assertThat(result.user().getId()).isEqualTo("user-1");
    }

    @Test
    void login_shouldRecordEngagementOnSuccess() {
        String realHash = new BCryptPasswordEncoder().encode("password123");
        User user = UserTestDataBuilder.aUser().withPersonId("person-1").withStatus(UserStatus.APPROVED)
                .withPasswordHash(realHash).build();
        user.setId("user-1");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        userService.login(loginRequest("jane@example.com", "password123"));

        verify(personService).recordEngagement(eq("person-1"), eq("auth"), eq("logged_in"), any(Map.class));
    }

    // ── approve ──────────────────────────────────────────────────────────

    @Test
    void approve_shouldSetStatusApprovedAndTimestamp() {
        User user = UserTestDataBuilder.aUser().withStatus(UserStatus.PENDING).build();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        when(personRepository.findById(anyString())).thenReturn(Optional.of(mockPerson));
        when(profileRepository.findByPersonId(anyString())).thenReturn(Optional.empty());

        userService.approve("user-1");

        verify(userRepository).save(argThat(u ->
                u.getStatus() == UserStatus.APPROVED && u.getApprovedAt() != null));
    }

    @Test
    void approve_shouldRecordEngagement() {
        User user = UserTestDataBuilder.aUser().withPersonId("person-1").build();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        when(personRepository.findById(anyString())).thenReturn(Optional.of(mockPerson));
        when(profileRepository.findByPersonId(anyString())).thenReturn(Optional.empty());

        userService.approve("user-1");

        verify(personService).recordEngagement(eq("person-1"), eq("auth"), eq("approved"), any(Map.class));
    }

    // ── forgot password ──────────────────────────────────────────────────

    @Test
    void forgotPassword_shouldReturnGenericMessageAndNoTokenWhenEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = userService.forgotPassword(forgotPasswordRequest("missing@example.com"));

        assertThat(response.getMessage()).isEqualTo("If that email is registered, a password reset link has been sent.");
        assertThat(response.getResetToken()).isNull();
        assertThat(response.getName()).isNull();
    }

    @Test
    void forgotPassword_shouldReturnSameGenericMessageWhenAccountPending() {
        User user = UserTestDataBuilder.aUser().withStatus(UserStatus.PENDING).build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = userService.forgotPassword(forgotPasswordRequest("jane@example.com"));

        assertThat(response.getMessage()).isEqualTo("If that email is registered, a password reset link has been sent.");
        assertThat(response.getResetToken()).isNull();
    }

    @Test
    void forgotPassword_shouldReturnTokenAndNameWhenApproved() {
        User user = UserTestDataBuilder.aUser().withPersonId("person-1").withStatus(UserStatus.APPROVED).build();
        user.setId("user-1");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(personRepository.findById("person-1")).thenReturn(Optional.of(mockPerson));
        when(passwordResetTokenService.generateToken("user-1", 0)).thenReturn("reset-token-abc");

        ForgotPasswordResponse response = userService.forgotPassword(forgotPasswordRequest("jane@example.com"));

        assertThat(response.getResetToken()).isEqualTo("reset-token-abc");
        assertThat(response.getName()).isEqualTo("Jane Doe");
    }

    @Test
    void forgotPassword_shouldRecordEngagementWhenApproved() {
        User user = UserTestDataBuilder.aUser().withPersonId("person-1").withStatus(UserStatus.APPROVED).build();
        user.setId("user-1");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(personRepository.findById("person-1")).thenReturn(Optional.of(mockPerson));

        userService.forgotPassword(forgotPasswordRequest("jane@example.com"));

        verify(personService).recordEngagement(eq("person-1"), eq("auth"), eq("password_reset_requested"), any(Map.class));
    }

    // ── reset password ───────────────────────────────────────────────────

    @Test
    void resetPassword_shouldThrowUnauthorizedWhenTokenInvalid() {
        when(passwordResetTokenService.validateAndExtractClaims("bad-token"))
                .thenThrow(new JwtException("bad signature"));

        assertThatThrownBy(() -> userService.resetPassword(resetPasswordRequest("bad-token", "newPassword123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void resetPassword_shouldThrowUnauthorizedWhenUserMissing() {
        when(passwordResetTokenService.validateAndExtractClaims("token"))
                .thenReturn(new PasswordResetTokenService.ResetTokenClaims("missing-user", 0));
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(resetPasswordRequest("token", "newPassword123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void resetPassword_shouldThrowUnauthorizedWhenTokenVersionStale() {
        User user = UserTestDataBuilder.aUser().build();
        user.setId("user-1");
        user.setPasswordResetTokenVersion(2); // already used, or a newer link was requested since
        when(passwordResetTokenService.validateAndExtractClaims("token"))
                .thenReturn(new PasswordResetTokenService.ResetTokenClaims("user-1", 1)); // stale version
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.resetPassword(resetPasswordRequest("token", "newPassword123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndIncrementVersionWhenValid() {
        User user = UserTestDataBuilder.aUser().withPersonId("person-1").build();
        user.setId("user-1");
        user.setPasswordResetTokenVersion(0);
        when(passwordResetTokenService.validateAndExtractClaims("token"))
                .thenReturn(new PasswordResetTokenService.ResetTokenClaims("user-1", 0));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.resetPassword(resetPasswordRequest("token", "brandNewPassword123"));

        verify(userRepository).save(argThat(u ->
                u.getPasswordResetTokenVersion() == 1
                        && new BCryptPasswordEncoder().matches("brandNewPassword123", u.getPasswordHash())));
    }

    @Test
    void resetPassword_shouldRecordEngagementOnSuccess() {
        User user = UserTestDataBuilder.aUser().withPersonId("person-1").build();
        user.setId("user-1");
        when(passwordResetTokenService.validateAndExtractClaims("token"))
                .thenReturn(new PasswordResetTokenService.ResetTokenClaims("user-1", 0));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.resetPassword(resetPasswordRequest("token", "brandNewPassword123"));

        verify(personService).recordEngagement(eq("person-1"), eq("auth"), eq("password_reset_completed"), any(Map.class));
    }

    @Test
    void findById_shouldThrowNotFoundWhenMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private ForgotPasswordRequest forgotPasswordRequest(String email) {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(email);
        return req;
    }

    private ResetPasswordRequest resetPasswordRequest(String token, String newPassword) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken(token);
        req.setNewPassword(newPassword);
        return req;
    }
}
