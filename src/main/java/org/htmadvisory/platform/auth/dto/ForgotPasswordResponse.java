package org.htmadvisory.platform.auth.dto;

/**
 * Response for {@code POST /api/auth/forgot-password}. {@code message} is
 * always the same generic text regardless of whether the email was found —
 * this endpoint deliberately never reveals whether a given email is
 * registered, same principle as {@code UserService.login}'s identical
 * "invalid email or password" for both a wrong password and an unknown
 * email.
 *
 * <p>{@code resetToken} and {@code name} are present ONLY when the email
 * matched a real account — the frontend checks for {@code resetToken} to
 * decide whether to actually fire the reset email via EmailJS, but always
 * shows the person the same success message either way, so the UI never
 * leaks the answer. This does mean the token briefly exists in the
 * browser (there's no backend email capability in this codebase — EmailJS
 * sends are client-triggered everywhere, see {@code CLAUDE.md}), which is
 * a real, deliberate tradeoff of this architecture, not an oversight —
 * HTTPS protects it in transit, and the token itself is short-lived and
 * single-use ({@link org.htmadvisory.platform.auth.PasswordResetTokenService}).
 */
public class ForgotPasswordResponse {

    private final String message;
    private final String resetToken;
    private final String name;

    public ForgotPasswordResponse(String message, String resetToken, String name) {
        this.message = message;
        this.resetToken = resetToken;
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public String getResetToken() {
        return resetToken;
    }

    public String getName() {
        return name;
    }
}
