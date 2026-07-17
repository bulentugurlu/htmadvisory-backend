package org.htmadvisory.platform.shared;

import org.htmadvisory.platform.auth.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers shared cross-cutting MVC concerns.
 *
 * The {@link EnvironmentTokenInterceptor} is applied to all {@code /api/**}
 * paths. Management/actuator endpoints ({@code /actuator/**}) are intentionally
 * excluded — they use Spring Boot's own security mechanisms and do not carry
 * the frontend-facing API token.
 *
 * <p>{@link JwtAuthInterceptor} is layered on top of that (both run, in
 * registration order) for the routes that additionally need to know which
 * logged-in member is calling: {@code /api/auth/me}, everything under
 * {@code /api/admin/**} (which the interceptor also restricts to ADMIN
 * role — see its Javadoc), and {@code /api/documents/private/{docId}/request-download}
 * (any approved member, not ADMIN-only). {@code /api/auth/register},
 * {@code /api/auth/login}, and {@code /api/documents/private/download} are
 * intentionally NOT in this list — they must be reachable without a
 * session token (the last one is opened from an email and carries its own
 * single-purpose token instead; see {@code documents.DocumentController}).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final EnvironmentTokenInterceptor environmentTokenInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(EnvironmentTokenInterceptor environmentTokenInterceptor,
                         JwtAuthInterceptor jwtAuthInterceptor) {
        this.environmentTokenInterceptor = environmentTokenInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(environmentTokenInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/auth/me", "/api/admin/**", "/api/documents/private/*/request-download");
    }
}
