package org.htmadvisory.platform.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.htmadvisory.platform.shared.EnvironmentTokenInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Validates the {@code Authorization: Bearer <token>} header on routes that
 * need to know WHO is calling, not just that the call came from our
 * frontend (that's {@link EnvironmentTokenInterceptor}'s job, and both
 * interceptors run — see {@code WebMvcConfig} in the {@code shared}
 * package, which registers both). This lives in {@code auth} rather than
 * {@code shared} because, unlike the environment token, it's genuinely
 * auth-domain logic (JWT parsing, role semantics) — {@code shared} stays
 * for domain-agnostic cross-cutting concerns only, per CLAUDE.md.
 *
 * <p>On success, sets two request attributes that downstream controllers
 * read instead of touching the token themselves — same separation-of-
 * concerns rule as the env-token interceptor:
 * <ul>
 *   <li>{@code userId} — the authenticated User's id ({@code AuthController.me})</li>
 *   <li>{@code userRole} — {@code "MEMBER"} or {@code "ADMIN"}</li>
 * </ul>
 *
 * <p><strong>Admin enforcement lives here too</strong>, path-prefix based:
 * any {@code /api/admin/**} request also requires {@code userRole ==
 * ADMIN}, returning 403 otherwise. Keeping both checks in one interceptor
 * (rather than a second role-checking layer) matches this codebase's
 * "exactly one place checks it" convention.
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return unauthorized(response, "Missing or malformed Authorization header");
        }

        String token = header.substring(BEARER_PREFIX.length());
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (JwtException e) {
            log.debug("Rejected invalid/expired JWT: {}", e.getMessage());
            return unauthorized(response, "Invalid or expired token");
        }

        String userId = jwtService.extractUserId(claims);
        String role = jwtService.extractRole(claims);
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);

        if (request.getRequestURI().startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Admin access required\"}");
            return false;
        }

        return true;
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
