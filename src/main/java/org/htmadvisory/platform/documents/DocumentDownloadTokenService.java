package org.htmadvisory.platform.documents;

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
 * Mints and validates the one-time tokens embedded in document-delivery
 * emails.
 *
 * <p>This is deliberately NOT {@code auth.JwtService} reused as-is, even
 * though both sign with the same {@code htm.jwt.secret} — the two serve
 * different purposes and have very different lifetimes and claim shapes.
 * A member-session token proves "this browser is logged in as this user"
 * for up to 7 days; a download token proves "this specific document was
 * requested by this specific user" for {@code
 * htm.documents.download-token-expiration-minutes} (15 by default) and is
 * meant to be embedded in an email a person might open minutes or days
 * later, outside any authenticated session — the token itself, not a
 * header, is the credential at that point. Reusing {@code JwtService}
 * directly would mean either weakening its session-token semantics or
 * bolting download-specific claims onto a class that has nothing to do
 * with documents.
 *
 * <p>Sharing the signing secret is intentional and safe — it doesn't imply
 * these tokens are interchangeable. {@link #validateAndExtractDocId}
 * checks a {@code purpose} claim specifically so a session token can never
 * be replayed here, and (equally important) a download token can never be
 * used in place of a session token, since {@code auth.JwtAuthInterceptor}
 * doesn't check for or accept this token's shape at all.
 */
@Service
public class DocumentDownloadTokenService {

    private static final String CLAIM_DOC_ID = "docId";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String PURPOSE_DOCUMENT_DOWNLOAD = "document-download";

    private final SecretKey key;
    private final long expirationMinutes;

    public DocumentDownloadTokenService(
            @Value("${htm.jwt.secret}") String secret,
            @Value("${htm.documents.download-token-expiration-minutes:15}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("htm.jwt.secret is not configured.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** Issues a token good for exactly one document, for a limited time. */
    public String generateToken(String userId, String docId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_DOC_ID, docId)
                .claim(CLAIM_PURPOSE, PURPOSE_DOCUMENT_DOWNLOAD)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Validates signature, expiry, and purpose, then returns the doc id it
     * was issued for.
     *
     * @throws JwtException if the token is malformed, expired, has an
     *                       invalid signature, or isn't a document-download
     *                       token at all (e.g. someone pasted a session
     *                       token here instead) — {@code DocumentController}
     *                       maps this to a 401.
     */
    public String validateAndExtractDocId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String purpose = claims.get(CLAIM_PURPOSE, String.class);
        if (!PURPOSE_DOCUMENT_DOWNLOAD.equals(purpose)) {
            throw new JwtException("Not a document-download token");
        }

        return claims.get(CLAIM_DOC_ID, String.class);
    }
}
