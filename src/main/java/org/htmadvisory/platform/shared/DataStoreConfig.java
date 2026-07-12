package org.htmadvisory.platform.shared;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Explicit repository scanning configuration.
 *
 * Spring Data cannot reliably distinguish JPA from MongoDB repositories
 * when both are on the classpath. Without explicit scoping, both scanners
 * attempt to handle every repository interface, which causes the async
 * AuditExecutor to stall (audits stay PENDING) and may surface cryptic
 * "not a managed type" or "no qualifying bean" errors at startup.
 *
 * Split:
 * - @EnableMongoRepositories: entire platform tree EXCEPT audit.repository
 *   (people, profile, consent, traffic, contact — all MongoDB)
 * - @EnableJpaRepositories: audit.repository only (PostgreSQL / JPA)
 *
 * Placed in a standalone @Configuration class rather than on
 * PlatformBackendApplication so that @WebMvcTest slice tests are unaffected:
 * WebMvcTypeExcludeFilter excludes non-web @Configuration beans from the
 * slice context, preventing "no mongoTemplate bean" errors in those tests.
 */
@Configuration
@EnableMongoRepositories(
        basePackages = "org.htmadvisory.platform",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "org\\.htmadvisory\\.platform\\.audit\\.repository\\..*"
        )
)
@EnableJpaRepositories(basePackages = "org.htmadvisory.platform.audit.repository")
public class DataStoreConfig {
}
