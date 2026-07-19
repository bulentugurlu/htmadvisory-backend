package org.htmadvisory.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.htmadvisory.platform.auth.dto.ForgotPasswordRequest;
import org.htmadvisory.platform.auth.dto.ForgotPasswordResponse;
import org.htmadvisory.platform.auth.dto.LoginRequest;
import org.htmadvisory.platform.auth.dto.LoginResponse;
import org.htmadvisory.platform.auth.dto.RegisterRequest;
import org.htmadvisory.platform.auth.dto.RegisterResponse;
import org.htmadvisory.platform.auth.dto.ResetPasswordRequest;
import org.htmadvisory.platform.auth.dto.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The member-portal auth capability: register, login, and "who am I".
 *
 * <p>{@code /register} and {@code /login} sit behind only the environment
 * token (same as every other {@code /api/**} route) — they have to be
 * reachable by a logged-out visitor. {@code /me} additionally requires a
 * valid JWT; see {@code JwtAuthInterceptor} and its registration in {@code
 * WebMvcConfig}, which is the ONLY place that checks the token, mirroring
 * how {@code EnvironmentTokenInterceptor} keeps that concern out of every
 * controller.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        User user = userService.register(request);
        return new RegisterResponse(
                user.getId(), user.getEmail(), user.getStatus().name(),
                "Your registration is pending approval. We'll email you once your account is active.");
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        UserService.LoginResult result = userService.login(request);
        return new LoginResponse(result.token(), userService.getProfile(result.user().getId()));
    }

    /**
     * Always 200 with a generic message, whether or not the email is
     * registered — see {@code UserService.forgotPassword} and {@code
     * ForgotPasswordResponse}'s Javadoc for the full reasoning. The
     * frontend uses {@code resetToken} (present only for a real, approved
     * account) purely to decide whether to fire the reset email via
     * EmailJS — it must never surface that presence/absence to the person
     * using the form.
     */
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return userService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
    }

    /**
     * {@code userId} is read from the request attribute set by {@code
     * JwtAuthInterceptor} after it validates the {@code Authorization:
     * Bearer <token>} header — this controller never touches the token
     * itself.
     */
    @GetMapping("/me")
    public UserProfileResponse me(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return userService.getProfile(userId);
    }
}
