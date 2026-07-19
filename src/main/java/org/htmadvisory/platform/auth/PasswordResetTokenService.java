package org.htmadvisory.platform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Mints and validates the one-time tokens embedded in password-reset
 * emails. Mirrors {@code documents.DocumentDownloadTokenService} —
 * deliberately not {@code JwtService} reused as-is, for the same reason
 * documented there: a reset token proves "this specific reset was
 * requested for this specific user" for a short window, not "this browser
 * has a live session", and the two must never be interchangeable.
 * {@code JwtAuthInterceptor} doesn't recognize this token's shape at all.
 *
 * <p>Unlike a document-download token, a reset token also carries the
 * user's {@link User#getPasswordResetTokenVersion()} at mint time.
 * {@code UserService.resetPassword} increments that counter on every
 * successful reset, so a token becomes unusable the moment it's used once
 * — even though JWTs are otherwise stateless and can't normally be
 * revoked early. It also means requesting a second reset link silently
 * invalidates an earlier, unused one for the same user, which is the
 * correct behavior (only the most recent request should ever work).
 */
@Service
public class PasswordResetTokenService {

    private static final String CLAIM_PURPOSE = "purpose";
    private static final String CLAIM_VERSION = "tokenVersion";
    private static final String PURPOSE_PASSWORD_RESET = "password-reset";

    private final SecretKey key;
    private final long expirationMinutes;

    public PasswordResetTokenService(
            @Value("${htm.jwt.secret}") String secret,
            @Value("${htm.auth.password-reset-token-expiration-minutes:30}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("htm.jwt.secret is not configured.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** Issues a token for one user, valid until used once or expired — whichever first. */
    public String generateToken(String userId, int currentTokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_RESET)
                .claim(CLAIM_VERSION, currentTokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** The (userId, tokenVersion) pair a reset token was issued with. */
    public record ResetTokenClaims(String userId, int tokenVersion) {}

    /**
     * Validates signature, expiry, and purpose, then returns the userId
     * and version it was issued for. Does NOT check the version against
     * the user's current one — that comparison needs the User record, so
     * it's done by the caller ({@code UserService.resetPassword}), which
     * already has to look the user up anyway.
     *
     * @throws JwtException if malformed, expired, wrong signature, or not
     *                       a password-reset token at all (e.g. someone
     *                       pasted a document-download or session token
     *                       here instead).
     */
    public ResetTokenClaims validateAndExtractClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String purpose = claims.get(CLAIM_PURPOSE, String.class);
        if (!PURPOSE_PASSWORD_RESET.equals(purpose)) {
            throw new JwtException("Not a password-reset token");
        }

        Integer version = claims.get(CLAIM_VERSION, Integer.class);
        return new ResetTokenClaims(claims.getSubject(), version == null ? 0 : version);
    }
}
