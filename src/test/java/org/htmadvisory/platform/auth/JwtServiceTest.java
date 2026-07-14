package org.htmadvisory.platform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService} — constructed directly with a test
 * secret, no Spring context needed.
 */
class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-at-least-32-bytes-long-xxxx";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 168);

    @Test
    void generateToken_shouldProduceAParsableToken() {
        User user = UserTestDataBuilder.aUser().withRole(UserRole.MEMBER).build();
        user.setId("user-1");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.parseToken(token)).isNotNull();
    }

    @Test
    void parseToken_shouldContainCorrectClaims() {
        User user = UserTestDataBuilder.aUser().withPersonId("person-42").withRole(UserRole.ADMIN).build();
        user.setId("user-99");

        Claims claims = jwtService.parseToken(jwtService.generateToken(user));

        assertThat(jwtService.extractUserId(claims)).isEqualTo("user-99");
        assertThat(jwtService.extractRole(claims)).isEqualTo("ADMIN");
        assertThat(claims.get("personId", String.class)).isEqualTo("person-42");
        assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
    }

    @Test
    void parseToken_shouldRejectATamperedToken() {
        User user = UserTestDataBuilder.aUser().build();
        user.setId("user-1");
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.parseToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseToken_shouldRejectAnExpiredToken() {
        JwtService alreadyExpiredIssuer = new JwtService(TEST_SECRET, -1); // expires 1 hour before issuance
        User user = UserTestDataBuilder.aUser().build();
        user.setId("user-1");
        String expiredToken = alreadyExpiredIssuer.generateToken(user);

        assertThatThrownBy(() -> jwtService.parseToken(expiredToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseToken_shouldRejectATokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-32-bytes-plus", 168);
        User user = UserTestDataBuilder.aUser().build();
        user.setId("user-1");
        String token = otherService.generateToken(user);

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_shouldRejectBlankSecret() {
        assertThatThrownBy(() -> new JwtService("", 168)).isInstanceOf(IllegalStateException.class);
    }
}
