package org.htmadvisory.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;

/**
 * Entry point for the HTM Advisory platform backend.
 *
 * Domain code lives in sibling packages under org.htmadvisory.platform
 * (e.g. org.htmadvisory.platform.contact, .people, .survey) — this class
 * is intentionally minimal and contains no business logic. See CLAUDE.md
 * in the project root for the full domain-based, capability-centric
 * architecture this codebase follows.
 *
 * LiquibaseAutoConfiguration is excluded because MongoDB Liquibase is managed
 * manually via MongoLiquibaseRunner — Spring Boot's Liquibase auto-configuration
 * requires a JDBC DataSource, which was unavailable until the audit domain added
 * PostgreSQL. Now that a DataSource exists, the autoconfiguration would activate
 * and fail trying to find a JDBC changelog. The manual runner remains in place.
 *
 * Repository scanning is split in DataStoreConfig (shared/) so Spring Data
 * doesn't apply both JPA and Mongo scanners to every repository interface.
 * Kept in a separate @Configuration class so @WebMvcTest slice tests are not
 * affected — WebMvcTypeExcludeFilter excludes non-web @Configuration beans.
 */
@SpringBootApplication(exclude = {LiquibaseAutoConfiguration.class})
public class PlatformBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformBackendApplication.class, args);
    }

}
