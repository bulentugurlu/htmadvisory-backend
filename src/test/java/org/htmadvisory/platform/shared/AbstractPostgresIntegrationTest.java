package org.htmadvisory.platform.shared;

/**
 * Base class for audit domain integration tests that need both MongoDB and
 * PostgreSQL. Both containers are inherited from AbstractMongoIntegrationTest's
 * static init block — they are the same singleton instances shared by all tests.
 *
 * Audit tests extend this class rather than AbstractMongoIntegrationTest directly
 * for clarity, but the container setup is identical.
 */
public abstract class AbstractPostgresIntegrationTest extends AbstractMongoIntegrationTest {
    // Containers and DynamicPropertySource are inherited from AbstractMongoIntegrationTest.
}
