package org.htmadvisory.platform.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PasswordResetTokenService} — constructed directly
 * with a test secret, no Spring context needed. Mirrors {@code
 * documents.DocumentDownloadTokenServiceTest}'s structure; the
 * single-use-via-version-comparison behavior itself is tested in {@code
 * UserServiceTest}, since that comparison happens in {@code
 * UserService.resetPassword}, not here (see this class's own Javadoc).
 */
class PasswordResetTokenServiceTest {

    private static final String TEST_SECRET = "test-secret-at-least-32-bytes-long-xxxx";

    private final PasswordResetTokenService tokenService =
            new PasswordResetTokenService(TEST_SECRET, 30);

    @Test
    void generateToken_shouldProduceATokenThatValidatesBackToTheSameUserIdAndVersion() {
        String token = tokenService.generateToken("user-1", 3);

        PasswordResetTokenService.ResetTokenClaims claims = tokenService.validateAndExtractClaims(token);

        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.tokenVersion()).isEqualTo(3);
    }

    @Test
    void validateAndExtractClaims_shouldRejectATamperedToken() {
        String token = tokenService.generateToken("user-1", 0);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> tokenService.validateAndExtractClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractClaims_shouldRejectAnExpiredToken() {
        PasswordResetTokenService alreadyExpired = new PasswordResetTokenService(TEST_SECRET, -1);
        String token = alreadyExpired.generateToken("user-1", 0);

        assertThatThrownBy(() -> tokenService.validateAndExtractClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractClaims_shouldRejectATokenSignedWithADifferentSecret() {
        PasswordResetTokenService otherSecret =
                new PasswordResetTokenService("a-completely-different-secret-32-bytes-plus", 30);
        String token = otherSecret.generateToken("user-1", 0);

        assertThatThrownBy(() -> tokenService.validateAndExtractClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractClaims_shouldRejectATokenMissingThePasswordResetPurpose() {
        // Simulates someone pasting a document-download or member-session
        // token (or any other token signed with the same secret but a
        // different intent) into the reset link — this must not be
        // treated as valid, even though the signature checks out.
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String notAResetToken = Jwts.builder()
                .subject("user-1")
                .claim("purpose", "document-download")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> tokenService.validateAndExtractClaims(notAResetToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_shouldRejectBlankSecret() {
        assertThatThrownBy(() -> new PasswordResetTokenService("", 30))
                .isInstanceOf(IllegalStateException.class);
    }
}
