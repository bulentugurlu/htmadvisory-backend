package org.htmadvisory.platform.auth;

import org.htmadvisory.platform.auth.dto.ForgotPasswordRequest;
import org.htmadvisory.platform.auth.dto.ForgotPasswordResponse;
import org.htmadvisory.platform.auth.dto.LoginRequest;
import org.htmadvisory.platform.auth.dto.RegisterRequest;
import org.htmadvisory.platform.auth.dto.ResetPasswordRequest;
import org.htmadvisory.platform.auth.dto.UserProfileResponse;
import org.htmadvisory.platform.consent.ConsentService;
import io.jsonwebtoken.JwtException;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonRepository;
import org.htmadvisory.platform.people.PersonService;
import org.htmadvisory.platform.profile.PersonProfile;
import org.htmadvisory.platform.profile.ProfileRepository;
import org.htmadvisory.platform.profile.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the member-portal account lifecycle: register (PENDING) → admin
 * approve (APPROVED) → login (issues a JWT). Follows the same four-step
 * cross-domain pattern documented on {@code ContactService}: resolve/create
 * the Person, persist the domain record, record an engagement, and (here)
 * also write to {@code profile} since registration is the richest signup
 * moment we have.
 *
 * <p>Consent is captured exactly once, here, rather than being re-asked on
 * every subsequent action a member takes (e.g. every whitepaper download —
 * that used to be the flow, and it was repetitive and pointless for a
 * known, already-approved member). It's recorded via the existing {@code
 * consent} domain rather than as fields on User, matching this codebase's
 * pattern of consent living in its own append-only, auditable domain keyed
 * by personId — see {@code ConsentRecord}'s Javadoc for why.
 *
 * <p>Errors are surfaced as {@link ResponseStatusException} directly from
 * this service — there's no {@code @ControllerAdvice} in this codebase yet,
 * so this keeps error mapping in one place per failure without introducing
 * a new architectural layer for a single domain.
 */
@Service
public class UserService {

    /** Required to register — implied by submitting the form at all, but
     *  still recorded as an explicit, auditable consent event. */
    private static final String CONSENT_TYPE_COMMUNICATIONS = "communications";
    /** Optional — the one real choice on the registration form. */
    private static final String CONSENT_TYPE_MARKETING = "marketing";
    private static final String CONSENT_SOURCE = "account_registration";

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final ProfileRepository profileRepository;
    private final PersonService personService;
    private final ProfileService profileService;
    private final JwtService jwtService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final ConsentService consentService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository,
                        PersonRepository personRepository,
                        ProfileRepository profileRepository,
                        PersonService personService,
                        ProfileService profileService,
                        JwtService jwtService,
                        PasswordResetTokenService passwordResetTokenService,
                        ConsentService consentService) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.profileRepository = profileRepository;
        this.personService = personService;
        this.profileService = profileService;
        this.jwtService = jwtService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.consentService = consentService;
    }

    /**
     * Creates a PENDING account. Rejects with 409 if the email is already
     * registered — checked against {@code users}, not {@code people}, since
     * a Person can legitimately exist (e.g. from a prior contact inquiry)
     * without having a portal account yet.
     */
    public User register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        });

        // Step 1 — identity
        Person person = personService.findOrCreateByEmail(request.getEmail(), request.getName());

        // Step 2 — firmographic detail captured at registration
        profileService.createOrUpdateProfile(person.getId(), request.getCompany(), request.getTitle(), null, null);

        // Step 3 — the account itself, PENDING until an admin approves it
        Instant now = Instant.now();
        User user = new User(
                person.getId(), request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                UserRole.MEMBER, UserStatus.PENDING, now, null);
        userRepository.save(user);

        // Step 4 — cross-domain engagement
        personService.recordEngagement(person.getId(), "auth", "registered", Map.of("userId", user.getId()));

        // Step 5 — consent, captured once here rather than re-asked later.
        // Communications consent is always true at this point — the
        // frontend makes that checkbox mandatory to submit the form.
        consentService.recordConsent(person.getId(), CONSENT_TYPE_COMMUNICATIONS, true, CONSENT_SOURCE);
        consentService.recordConsent(person.getId(), CONSENT_TYPE_MARKETING, request.isConsentMarketing(), CONSENT_SOURCE);

        return user;
    }

    /**
     * Verifies credentials and status, then issues a JWT. Deliberately
     * returns the same "invalid email or password" message whether the
     * email doesn't exist or the password is wrong, so login can't be used
     * to enumerate registered emails. Returns the {@link User} alongside
     * the token so the controller can build the response's {@code user}
     * field without a second lookup.
     */
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is still pending approval");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not active");
        }

        personService.recordEngagement(user.getPersonId(), "auth", "logged_in", Map.of("userId", user.getId()));

        return new LoginResult(jwtService.generateToken(user), user);
    }

    /** Token + the User it was issued for. See {@link #login(LoginRequest)}. */
    public record LoginResult(String token, User user) {}

    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "If that email is registered, a password reset link has been sent.";

    /**
     * Always returns the same generic message regardless of whether the
     * email is registered — see {@link ForgotPasswordResponse}'s Javadoc
     * for the full reasoning. Only populates {@code resetToken}/{@code
     * name} when a real account was found; the controller/frontend must
     * not branch on anything else in this response.
     *
     * <p>Silently does nothing (still returns the generic message) for a
     * PENDING or REJECTED account — there's no password to reset access
     * to yet, and confirming account status here would reopen the same
     * enumeration concern this method exists to avoid.
     */
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        Optional<User> maybeUser = userRepository.findByEmail(request.getEmail());
        if (maybeUser.isEmpty() || maybeUser.get().getStatus() != UserStatus.APPROVED) {
            return new ForgotPasswordResponse(GENERIC_FORGOT_PASSWORD_MESSAGE, null, null);
        }

        User user = maybeUser.get();
        String token = passwordResetTokenService.generateToken(user.getId(), user.getPasswordResetTokenVersion());
        Person person = personRepository.findById(user.getPersonId()).orElse(null);
        String name = person != null ? person.getName() : null;

        personService.recordEngagement(user.getPersonId(), "auth", "password_reset_requested", Map.of("userId", user.getId()));

        return new ForgotPasswordResponse(GENERIC_FORGOT_PASSWORD_MESSAGE, token, name);
    }

    /**
     * Validates the reset token (signature, expiry, purpose, AND that its
     * embedded version still matches the user's current {@code
     * passwordResetTokenVersion} — see {@link PasswordResetTokenService}'s
     * Javadoc for why that comparison lives here rather than in the token
     * service itself), then updates the password and increments the
     * version, which immediately invalidates this token and any other
     * outstanding one for the same user.
     */
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenService.ResetTokenClaims claims;
        try {
            claims = passwordResetTokenService.validateAndExtractClaims(request.getToken());
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This reset link is invalid or has expired.");
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This reset link is invalid or has expired."));

        if (claims.tokenVersion() != user.getPasswordResetTokenVersion()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "This reset link has already been used or a newer one was requested. Please request a new link.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetTokenVersion(user.getPasswordResetTokenVersion() + 1);
        userRepository.save(user);

        personService.recordEngagement(user.getPersonId(), "auth", "password_reset_completed", Map.of("userId", user.getId()));
    }

    /** Builds the joined {@code User + Person + PersonProfile} view for a given user id. */
    public UserProfileResponse getProfile(String userId) {
        User user = findById(userId);
        Person person = personRepository.findById(user.getPersonId()).orElse(null);
        PersonProfile profile = profileRepository.findByPersonId(user.getPersonId()).orElse(null);
        return UserProfileResponse.from(user, person, profile);
    }

    public User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Moves a PENDING (or REJECTED) account to APPROVED, allowing it to log
     * in. Idempotent-ish: approving an already-APPROVED account just
     * refreshes {@code approvedAt} rather than erroring, since a double
     * click in the admin UI shouldn't be a hard failure.
     */
    public UserProfileResponse approve(String userId) {
        User user = findById(userId);
        user.setStatus(UserStatus.APPROVED);
        user.setApprovedAt(Instant.now());
        userRepository.save(user);

        personService.recordEngagement(user.getPersonId(), "auth", "approved", Map.of("userId", user.getId()));

        return getProfile(user.getId());
    }

    /** Lists accounts for the admin console, optionally filtered by status. */
    public List<UserProfileResponse> listUsers(Optional<UserStatus> status) {
        List<User> users = status.map(userRepository::findByStatus).orElseGet(userRepository::findAll);
        return users.stream().map(u -> getProfile(u.getId())).toList();
    }
}
