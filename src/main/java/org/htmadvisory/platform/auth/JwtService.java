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
 * Issues and validates the JWTs used for member-portal session auth.
 *
 * <p>This is a second, independent security layer from {@link
 * org.htmadvisory.platform.shared.EnvironmentTokenInterceptor} — the env
 * token proves "this request came from our frontend"; the JWT proves "this
 * request is acting as this specific logged-in member". Both checks apply
 * on the routes that need them (see {@link JwtAuthInterceptor} and {@code
 * WebMvcConfig}). This is a deliberate choice, not the "swap the env token
 * for JWT" migration mentioned in {@code EnvironmentTokenInterceptor}'s
 * Javadoc — that comment describes replacing the *environment* gate
 * end-to-end someday; until then the two layers serve different purposes
 * and both stay in place.
 *
 * <p><strong>Secret:</strong> injected from the {@code JWT_SECRET}
 * environment variable (via GCP Secret Manager at deploy time). Must be at
 * least 32 bytes for HS256. {@code application.yml} provides a checked-in,
 * non-secret default (same convention as {@code spring.datasource.password})
 * so tests and local dev work without any setup — see that file's comment
 * for why an empty/fail-fast default was tried and rejected.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_PERSON_ID = "personId";

    private final SecretKey key;
    private final long expirationHours;

    public JwtService(@Value("${htm.jwt.secret}") String secret,
                       @Value("${htm.jwt.expiration-hours:168}") long expirationHours) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "htm.jwt.secret (JWT_SECRET) is not configured and no default resolved from " +
                    "application.yml — this should not happen outside of a broken test/property setup.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    /** Issues a signed JWT for a freshly authenticated, APPROVED user. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_PERSON_ID, user.getPersonId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    /**
     * Parses and validates a token, returning its claims.
     *
     * @throws JwtException if the token is malformed, expired, or has an
     *                       invalid signature — callers (currently only
     *                       {@code JwtAuthInterceptor}) translate this into
     *                       a 401 response.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }
}
