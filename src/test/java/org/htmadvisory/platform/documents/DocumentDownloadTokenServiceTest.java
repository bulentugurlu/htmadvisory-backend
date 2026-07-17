package org.htmadvisory.platform.documents;

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
 * Unit tests for {@link DocumentDownloadTokenService} — constructed
 * directly with a test secret, no Spring context needed. Mirrors {@code
 * auth.JwtServiceTest}'s structure.
 */
class DocumentDownloadTokenServiceTest {

    private static final String TEST_SECRET = "test-secret-at-least-32-bytes-long-xxxx";

    private final DocumentDownloadTokenService tokenService =
            new DocumentDownloadTokenService(TEST_SECRET, 15);

    @Test
    void generateToken_shouldProduceATokenThatValidatesBackToTheSameDocId() {
        String token = tokenService.generateToken("user-1", "arch-spec");

        assertThat(tokenService.validateAndExtractDocId(token)).isEqualTo("arch-spec");
    }

    @Test
    void validateAndExtractDocId_shouldRejectATamperedToken() {
        String token = tokenService.generateToken("user-1", "arch-spec");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> tokenService.validateAndExtractDocId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractDocId_shouldRejectAnExpiredToken() {
        DocumentDownloadTokenService alreadyExpired = new DocumentDownloadTokenService(TEST_SECRET, -1);
        String token = alreadyExpired.generateToken("user-1", "arch-spec");

        assertThatThrownBy(() -> tokenService.validateAndExtractDocId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractDocId_shouldRejectATokenSignedWithADifferentSecret() {
        DocumentDownloadTokenService otherSecret =
                new DocumentDownloadTokenService("a-completely-different-secret-32-bytes-plus", 15);
        String token = otherSecret.generateToken("user-1", "arch-spec");

        assertThatThrownBy(() -> tokenService.validateAndExtractDocId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAndExtractDocId_shouldRejectATokenMissingTheDocumentDownloadPurpose() {
        // Simulates someone pasting a member-session-style token (or any
        // other token signed with the same secret but a different intent)
        // into the download link — this must not be treated as valid.
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String notADownloadToken = Jwts.builder()
                .subject("user-1")
                .claim("purpose", "member-session")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> tokenService.validateAndExtractDocId(notADownloadToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_shouldRejectBlankSecret() {
        assertThatThrownBy(() -> new DocumentDownloadTokenService("", 15))
                .isInstanceOf(IllegalStateException.class);
    }
}
