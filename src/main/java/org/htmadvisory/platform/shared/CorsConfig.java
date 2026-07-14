package org.htmadvisory.platform.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the production frontend and local dev.
        // Localhost uses a pattern (not exact ports) because Vite
        // auto-increments its port (5173, 5174, 5175, ...) whenever the
        // default is already taken by another running dev server — an
        // exact-port allowlist breaks every time that happens.
        config.setAllowedOriginPatterns(List.of(
            "https://htmadvisory.org",
            "https://www.htmadvisory.org",
            "http://localhost:*"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
