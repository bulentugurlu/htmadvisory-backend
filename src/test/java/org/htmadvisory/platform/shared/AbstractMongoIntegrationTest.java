package org.htmadvisory.platform.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real MongoDB instance.
 *
 * Per CLAUDE.md's Automated Testing Strategy: integration tests run against
 * a real, throwaway MongoDB container — never an in-memory fake — because a
 * fake can behave differently from real MongoDB in ways that matter (e.g.
 * the index "name" requirement that broke our Liquibase changesets would
 * NOT have been caught by an in-memory simulation).
 *
 * The container starts once per test class (not once per test method) for
 * speed, and Liquibase changesets run against it automatically on
 * application startup via MongoLiquibaseRunner — exactly the same code path
 * used in dev/test/stage/prod, so integration tests validate against the
 * real schema-creation mechanism, not a hand-maintained test-only schema.
 *
 * Extend this class for any integration test that needs to read/write real
 * data — do NOT instantiate repositories/services directly against a mock
 * connection for integration-level tests; that defeats the purpose.
 */
@Testcontainers
@SpringBootTest
public abstract class AbstractMongoIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

}
