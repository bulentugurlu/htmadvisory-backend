package org.htmadvisory.platform.shared;

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
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final EnvironmentTokenInterceptor environmentTokenInterceptor;

    public WebMvcConfig(EnvironmentTokenInterceptor environmentTokenInterceptor) {
        this.environmentTokenInterceptor = environmentTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(environmentTokenInterceptor)
                .addPathPatterns("/api/**");
    }
}
