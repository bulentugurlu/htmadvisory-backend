package org.htmadvisory.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the HTM Advisory platform backend.
 *
 * Domain code lives in sibling packages under org.htmadvisory.platform
 * (e.g. org.htmadvisory.platform.contact, .people, .survey) — this class
 * is intentionally minimal and contains no business logic. See CLAUDE.md
 * in the project root for the full domain-based, capability-centric
 * architecture this codebase follows.
 */
@SpringBootApplication
public class PlatformBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformBackendApplication.class, args);
    }

}
