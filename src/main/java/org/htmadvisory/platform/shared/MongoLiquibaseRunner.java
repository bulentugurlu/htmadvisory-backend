package org.htmadvisory.platform.shared;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Manually runs Liquibase changesets against MongoDB on application startup.
 *
 * WHY THIS EXISTS (read before touching this class):
 * Spring Boot's built-in Liquibase auto-configuration (the spring.liquibase.*
 * properties, the SpringLiquibase bean Spring Boot wires up automatically)
 * is hard-coded around a JDBC javax.sql.DataSource. MongoDB has no
 * DataSource concept, so that auto-configuration permanently refuses to
 * activate — confirmed via --debug logging, and confirmed as a long-standing,
 * explicitly "declined" gap in Spring Boot itself (see
 * https://github.com/spring-projects/spring-boot/issues/29991).
 *
 * The liquibase-mongodb extension provides the DRIVER logic Liquibase needs
 * to talk to MongoDB, but it does NOT plug into spring.liquibase.change-log
 * auto-wiring. So changesets must be run manually, using Liquibase's core
 * API directly against a liquibase.database.Database obtained via the
 * MongoDB connection string — which is exactly what this class does.
 *
 * Do NOT add spring.liquibase.* properties to application.yml expecting
 * them to do anything for this MongoDB-only project — they will not.
 */
@Component
public class MongoLiquibaseRunner {

    private static final String CHANGELOG_PATH = "db/changelog-master.yaml";

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @EventListener(ApplicationReadyEvent.class)
    public void runChangesets() throws Exception {
        Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, new ClassLoaderResourceAccessor());

        try (Liquibase liquibase = new Liquibase(CHANGELOG_PATH, new ClassLoaderResourceAccessor(), database)) {
            liquibase.update();
        }
    }
}
