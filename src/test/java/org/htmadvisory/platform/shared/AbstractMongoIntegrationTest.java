package org.htmadvisory.platform.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Base class for integration tests that need a real MongoDB instance.
 *
 * Per CLAUDE.md's Automated Testing Strategy: integration tests run against
 * a real, throwaway MongoDB container — never an in-memory fake — because a
 * fake can behave differently from real MongoDB in ways that matter.
 *
 * SINGLETON CONTAINER PATTERN — read before changing this class:
 * The MongoDB container is started ONCE in a static initializer block and
 * shared across EVERY test class that extends this one, for the lifetime of
 * the whole test JVM. It is intentionally never stopped explicitly — Ryuk
 * (Testcontainers' own reaper container) destroys it automatically when the
 * JVM exits.
 *
 * WHY THIS MATTERS (a real bug this fixes, found 2026-06-28): if each test
 * class instead declares its own @Container-managed instance (one container
 * per class), Spring Boot Test's ApplicationContext caching can reuse a
 * cached context — including its MongoClient bean — from a PREVIOUS test
 * class against a container that has ALREADY been torn down by Ryuk after
 * that previous class finished. The symptom is every test in the second (and
 * later) test class timing out after ~30s with "Connection refused" /
 * "MongoTimeoutException", even though the first test class's container
 * worked perfectly and the second test class's OWN new container also
 * started successfully — the bug is that Spring never connects to the new
 * one. The singleton pattern eliminates this: there genuinely is only ever
 * one container and one port for the entire test run, so there is nothing
 * for a stale cached context to point at incorrectly.
 *
 * Do NOT add @Container or @Testcontainers back to this class, and do NOT
 * make the container non-static or call mongoDBContainer.stop() anywhere —
 * both reintroduce the exact bug this pattern fixes.
 */
@SpringBootTest
public abstract class AbstractMongoIntegrationTest {

    static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer("mongo:7");
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

}
