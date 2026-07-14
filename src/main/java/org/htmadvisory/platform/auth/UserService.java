package org.htmadvisory.platform.auth;

import org.htmadvisory.platform.auth.dto.LoginRequest;
import org.htmadvisory.platform.auth.dto.RegisterRequest;
import org.htmadvisory.platform.auth.dto.UserProfileResponse;
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
 * <p>Errors are surfaced as {@link ResponseStatusException} directly from
 * this service — there's no {@code @ControllerAdvice} in this codebase yet,
 * so this keeps error mapping in one place per failure without introducing
 * a new architectural layer for a single domain.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final ProfileRepository profileRepository;
    private final PersonService personService;
    private final ProfileService profileService;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository,
                        PersonRepository personRepository,
                        ProfileRepository profileRepository,
                        PersonService personService,
                        ProfileService profileService,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.profileRepository = profileRepository;
        this.personService = personService;
        this.profileService = profileService;
        this.jwtService = jwtService;
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
