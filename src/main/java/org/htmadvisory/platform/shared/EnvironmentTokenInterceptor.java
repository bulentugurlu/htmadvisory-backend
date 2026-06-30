package org.htmadvisory.platform.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Validates that every {@code /api/**} request carries the correct
 * per-environment shared secret in the {@code X-HTM-Env-Token} header.
 *
 * <p><strong>Design intent (read before changing):</strong> this interceptor
 * is the ONLY place the token is checked. Controllers never inspect the
 * header directly — that separation is what lets the token be swapped for
 * JWT later without touching any controller code. See ARCHITECTURE.md,
 * "Environment-Scoped API Security Tokens" for the full rationale.
 *
 * <p><strong>Token value:</strong> injected from the {@code HTM_ENV_TOKEN}
 * environment variable (via GCP Secret Manager at deploy time). Never
 * hardcoded, never committed to source, never set in {@code application-*.yml}.
 *
 * <p><strong>Bypass mode:</strong> if {@code HTM_ENV_TOKEN} is not set
 * (empty or absent), the check is disabled and a warning is logged. This
 * allows local development without a secret configured, but is intentionally
 * loud about it — production MUST have the variable set.
 */
@Component
public class EnvironmentTokenInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentTokenInterceptor.class);
    private static final String TOKEN_HEADER = "X-HTM-Env-Token";

    /** Injected from the {@code HTM_ENV_TOKEN} env var. Empty string = bypass. */
    @Value("${HTM_ENV_TOKEN:}")
    private String expectedToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (expectedToken == null || expectedToken.isBlank()) {
            log.warn("HTM_ENV_TOKEN is not configured — environment token check is DISABLED. " +
                     "This is acceptable for local development but MUST be set in production.");
            return true;
        }

        String provided = request.getHeader(TOKEN_HEADER);
        if (provided == null || !provided.equals(expectedToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid environment token\"}");
            return false;
        }

        return true;
    }
}
