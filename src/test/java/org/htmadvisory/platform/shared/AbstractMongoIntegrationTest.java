package org.htmadvisory.platform.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need a real MongoDB instance.
 *
 * Per CLAUDE.md's Automated Testing Strategy: integration tests run against
 * a real, throwaway MongoDB container — never an in-memory fake — because a
 * fake can behave differently from real MongoDB in ways that matter.
 *
 * SINGLETON CONTAINER PATTERN — read before changing this class:
 * Both containers (MongoDB and PostgreSQL) are started ONCE in a static
 * initializer block and shared across EVERY test class that extends this one,
 * for the lifetime of the whole test JVM. Ryuk destroys them automatically
 * when the JVM exits.
 *
 * WHY BOTH CONTAINERS HERE: once spring-boot-starter-data-jpa is on the
 * classpath, every @SpringBootTest context requires a DataSource. Rather than
 * splitting tests into two incompatible base classes that Spring's
 * ApplicationContext caching would treat as two separate contexts (causing
 * double startup costs and the container/context mismatch bug documented
 * below), we start both here and share one context across all tests.
 *
 * WHY NOT @Testcontainers/@Container: if each test class declares its own
 * @Container instance, Spring Boot Test's ApplicationContext caching can
 * reuse a context from a previous class against a container that has already
 * been torn down by Ryuk — causing MongoTimeoutException in the second (and
 * later) test class even though their own container started fine. The
 * singleton pattern eliminates this completely.
 *
 * Do NOT add @Container or @Testcontainers back, and do NOT call .stop()
 * on either container.
 */
@SpringBootTest
public abstract class AbstractMongoIntegrationTest {

    static final MongoDBContainer mongoDBContainer;
    static final PostgreSQLContainer<?> postgresContainer;

    static {
        mongoDBContainer = new MongoDBContainer("mongo:7");
        postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("htmadvisory_test")
                .withUsername("htmadvisory")
                .withPassword("testonly");
        mongoDBContainer.start();
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
