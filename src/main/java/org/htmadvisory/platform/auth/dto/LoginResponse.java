package org.htmadvisory.platform.auth.dto;

/**
 * Response payload for {@code POST /api/auth/login}. {@code token} is a
 * signed JWT the frontend stores (see {@code AuthContext.jsx} — localStorage)
 * and sends back as {@code Authorization: Bearer <token>} on subsequent
 * requests to routes guarded by {@code JwtAuthInterceptor}.
 */
public class LoginResponse {

    private String token;
    private UserProfileResponse user;

    public LoginResponse(String token, UserProfileResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserProfileResponse getUser() {
        return user;
    }
}
