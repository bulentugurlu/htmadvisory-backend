# CLAUDE.md — htmadvisory-backend

> This is the backend service repo. For the FULL architecture — domain model,
> environment strategy, testing strategy, CI/CD, JIRA integration, and the
> complete build order — see the `CLAUDE.md` in the `htmadvisory` (frontend)
> repo. This file is intentionally lightweight and backend-specific only.

## What This Repo Is

Spring Boot backend service for the HTM Advisory platform. Companion to
`htmadvisory` (frontend — React/Vite, contains the canonical CLAUDE.md).

## Stack

- **Language:** Java 21 (see "Java Version" note below — do NOT assume the
  globally-installed Java 25 works for this project)
- **Framework:** Spring Boot 3.4.1
- **Database:** MongoDB (via Spring Data MongoDB)
- **Schema management:** Liquibase + liquibase-mongodb — **run MANUALLY, NOT
  via Spring Boot's spring.liquibase.* auto-configuration** (see "Liquibase
  + MongoDB" section below)
- **Testing:** JUnit5/Mockito/AssertJ + Testcontainers (real MongoDB per
  test run) — **see "Testcontainers + Docker Desktop" section below before
  running tests for the first time on a new machine, or if tests suddenly
  fail with Docker-connection errors**
- **Build:** Maven

## Architecture Convention (summary — see main CLAUDE.md for full detail)

- **Domain-based, capability-centric.** Code is organized by business domain
  (`org.htmadvisory.platform.contact`, `.people`, `.survey`, etc.), not by
  technical layer. Endpoints are named for what the caller is doing
  (`POST /api/contacts/inquiries`), not for the database table touched.
- **`shared/` is cross-cutting only** — the environment-token interceptor,
  common exception handling, the `MongoLiquibaseRunner`, AND
  `AbstractMongoIntegrationTest` (see below). Never a dumping ground.
- **Build order: `people`/`profile`/`consent`/`traffic` (foundational
  identity/marketing domains) FIRST, then `contact`** (first domain-specific
  capability: `POST /api/contacts/inquiries`).

## Current Status (updated 2026-07-02)

- [x] Repo created
- [x] Maven + Spring Boot skeleton
- [x] Spring Profiles configured (`application.yml` + `application-dev.yml`)
- [x] Java 25 vs. Spring Boot Maven plugin incompatibility found and fixed
      (Java 21 pinned, `run-dev.sh` wrapper)
- [x] Local dev MongoDB running in Docker, app verified connected under the
      `dev` profile (port 27018 — see "Local MongoDB" below)
- [x] Liquibase dependencies correctly added to `pom.xml`
- [x] Liquibase running successfully against MongoDB via a manual runner —
      changesets 001–013 applied and verified
- [x] `people` domain Java code written: `Person`, `Engagement`,
      `PersonRepository`, `EngagementRepository`, `PersonService` with
      `findOrCreateByEmail()` and `recordEngagement()`
- [x] **Testing harness fully working and proven:** `AbstractMongoIntegrationTest`
      (Testcontainers base class), `PersonTestDataBuilder`, and
      `PersonServiceIntegrationTest` (5 tests) — **all 5 passing**, run
      against a real, throwaway MongoDB container with Liquibase changesets
      applied automatically, exactly mirroring the real dev/prod schema
      path. This closes the testing-harness sequencing gap flagged on
      2026-06-27.
- [x] **Major Docker/Testcontainers compatibility issue diagnosed and fixed**
      — see "Testcontainers + Docker Desktop" section below. This took a
      genuinely long debugging session; the fix is now permanent and
      machine-level, should never need rediscovering.
- [x] **`profile` domain built** — `PersonProfile`, `ProfileRepository`,
      `ProfileService` with null-preserving merge-in-place semantics;
      Liquibase changesets 005–006; 4 integration tests passing
- [x] **`consent` domain built** — `ConsentRecord`, `ConsentRepository`,
      `ConsentService`; append-only audit trail (status change = new record,
      never an overwrite); Liquibase changesets 007–008; 6 integration tests
      passing
- [x] **`traffic` domain built** — `Visit`, `VisitRepository`,
      `GeoLocationService` interface + `MaxMindGeoLocationService` (scaffolded,
      degrades gracefully); `TrafficService` with `recordVisit()` (30-min
      session window), `backfillPersonId()`; Liquibase changesets 009–011
      (personId index is sparse); 8 integration tests passing; MaxMind
      `geoip2` 4.2.0 added to `pom.xml`
- [x] **`contact` domain built** — `ContactInquiry`, `ContactRepository`,
      `ContactService`, `ContactController` (`POST /api/contacts/inquiries`,
      201 Created); `ContactInquiryRequest` DTO with `@NotBlank`/`@Email`
      validation; Liquibase changesets 012–013; **environment-token
      interceptor** (`EnvironmentTokenInterceptor` + `WebMvcConfig`) applied
      to `/api/**`, checks `X-HTM-Env-Token` header against `${HTM_ENV_TOKEN}`,
      bypasses with warning when unset; 7 Mockito unit tests + 8 Testcontainers
      integration tests passing; **38/38 total tests passing**
- [x] **Dockerfile added** — multi-stage build (JDK builder + JRE runtime),
      non-root user, container-aware JVM flags (`-XX:+UseContainerSupport`,
      `-XX:MaxRAMPercentage=75.0`); `.dockerignore` added; image verified
      building and starting cleanly locally
- [x] **CI workflow added** (`.github/workflows/ci.yml`) — runs on every
      push and PR to main: 38/38 tests, JAR build, Docker image build;
      first run passed in 2m 12s on GitHub-hosted runner
- [x] **Deploy workflow added** (`.github/workflows/deploy-dev.yml`) —
      runs on push to main: builds JAR, pushes Docker image to GCP Artifact
      Registry (`us-central1-docker.pkg.dev/htmadvisory/htmadvisory/htmadvisory-backend`),
      deploys to Cloud Run (`htmadvisory-backend-dev`, `--no-allow-unauthenticated`
      per architecture decision — IAM-only, not public-facing)
- [x] **Workload Identity Federation configured** — GitHub Actions
      authenticates to GCP without a long-lived JSON key; pool and provider
      created, `github-actions-deploy` service account bound; 3 GitHub
      secrets added: `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`,
      `MONGODB_URI`
- [x] **MongoDB Atlas dev cluster provisioned** — `htmadvisory-dev` cluster
      on GCP / Iowa (us-central1), M0 free tier, `0.0.0.0/0` IP allowlist
      for Cloud Run compatibility; connection string stored as `MONGODB_URI`
      GitHub secret; `SPRING_PROFILES_ACTIVE=dev` + `MONGODB_URI` passed
      to Cloud Run via deploy workflow
- [x] MongoDB Atlas dev cluster connected (cloud — separate from local Docker
      MongoDB above; not yet provisioned)
- [ ] PIT mutation testing configured (part of the full testing strategy in
      the main CLAUDE.md, not yet set up in this repo)
- [ ] Cucumber configured (same — not yet set up)
- [ ] Terraform module + `htmadvisory-dev` GCP project created from code
- [ ] AWS replication of the same Terraform module pattern (confirmed
      2026-06-26 as a near-term goal, sequenced AFTER GCP dev is fully
      proven)

## Local Development

### Java Version (read this before running anything)

The globally-installed JDK on this machine is **Java 25**, which is
**incompatible with the Spring Boot 3.4.1 Maven plugin's embedded run
tooling** — running `mvn spring-boot:run` directly under Java 25 fails with:
```
Unsupported class file major version 69
```
**Fix already applied:** Java 21 is installed alongside Java 25 via
`brew install openjdk@21`, `pom.xml` is pinned to `<java.version>21</java.version>`,
and a wrapper script (`run-dev.sh`) sets `JAVA_HOME` to the Java 21 install for
every Maven command. **Always use `./run-dev.sh` instead of `mvn` directly**
in this repo:

```bash
./run-dev.sh spring-boot:run -Dspring-boot.run.profiles=dev
./run-dev.sh test
```

### Testcontainers + Docker Desktop — READ THIS BEFORE RUNNING TESTS ON A NEW MACHINE

**This machine's Docker Desktop (Docker Engine 29.5.2) was initially
completely incompatible with Testcontainers, and the failure mode was
genuinely confusing — it looked like a socket/path problem but was actually
a library-version problem. Two fixes were required TOGETHER; do not assume
either one alone is sufficient.**

**Symptom (if this regresses):** every test extending `AbstractMongoIntegrationTest`
fails immediately with:
```
java.lang.IllegalStateException: Could not find a valid Docker environment.
...
BadRequestException (Status 400: {"ID":"","Containers":0,...,"ServerVersion":"",...})
```
The telltale sign this is THIS specific issue (not Docker actually being
down): the JSON in the error is a fully-formed but completely BLANK info
object — every field present but empty (`""`, `0`, `null`). A real
Docker-is-down error looks different (connection refused, no response at
all). A blank-but-well-formed response means Docker received the request
and rejected it at the API layer — which is exactly what an unsupported API
version negotiation looks like.

**What did NOT fix it (ruled out, in this order, do not waste time
re-trying these):**
1. Different `docker.host` socket paths in `~/.testcontainers.properties`
   (tried `/Users/<user>/.docker/run/docker.sock`,
   `/Users/<user>/Library/Containers/com.docker.docker/Data/docker-cli.sock`
   — both are real, valid sockets, confirmed via direct `curl
   --unix-socket ... http://localhost/info`, which returned fully populated
   real responses both times)
2. `DOCKER_HOST` environment variable pointing at the same sockets
3. Restarting Docker Desktop fully (quit + reopen)
4. Docker Desktop's "Allow the default Docker socket to be used" setting
   (Settings → Advanced) — already enabled by default on this machine
5. Confirming Docker itself works fine via `docker ps` and raw `curl` to the
   socket — it always did. **The bug was never in Docker. It was entirely in
   the Testcontainers/docker-java Java library version.**

**What ACTUALLY fixed it — BOTH steps required:**

**Step 1 — Upgrade Testcontainers from 1.20.4 to 1.21.3 in `pom.xml`:**
```xml
<!-- pom.xml, inside <dependencyManagement><dependencies> -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-bom</artifactId>
    <version>1.21.3</version>  <!-- was 1.20.4 -->
    <type>pom</type>
    <scope>import</scope>
</dependency>
```
Testcontainers 1.20.4's bundled `docker-java` client does not correctly
negotiate API versions with Docker Engine 29.x (a documented, known
incompatibility — see
https://github.com/testcontainers/testcontainers-java/issues/11419,
https://github.com/testcontainers/testcontainers-java/issues/11422,
https://github.com/testcontainers/testcontainers-java/issues/11240, all
reporting the IDENTICAL blank-JSON `BadRequestException` symptom against
recent Docker Desktop versions). **If `docker version --format
'{{.Server.Version}}'` on a future machine shows something in the 29.x+
range, check whether the Testcontainers version in pom.xml needs bumping
again** — this is an evolving compatibility boundary, not a one-time fix.

**Step 2 — Create `~/.docker-java.properties` (a machine-level file, NOT
project-level) forcing a specific, known-compatible Docker API version:**
```bash
cat > ~/.docker-java.properties << 'EOF'
api.version=1.45
EOF
```
This file is read by the `docker-java` library directly (not Testcontainers'
own `.testcontainers.properties`) and overrides automatic API version
negotiation, which was the actual point of failure. **This file is global to
the machine, not specific to this repo** — if tests fail on a fresh clone of
this repo on a NEW machine, check whether this file exists there too; it
will not be present automatically since it lives outside the repo and is
intentionally not committed (it's a per-machine Docker workaround, not
project configuration).

**Verification that both fixes are active**, from a clean test run, look
for this exact sequence early in the log:
```
INFO org.testcontainers.dockerclient.DockerClientProviderStrategy -- Found Docker environment with local Unix socket (unix:///var/run/docker.sock)
INFO org.testcontainers.DockerClientFactory -- Connected to docker:
  Server Version: 29.5.2
  API Version: 1.54
```
If you see this instead of the blank-JSON `BadRequestException`, both fixes
are working correctly.

### Local MongoDB (Docker)

**Critical: do NOT use the default MongoDB port (27017) on this machine.**
Port 27017 is already occupied by an unrelated project's MongoDB container
(`income-request-mongodb`) running persistently in Docker. Connecting this
app to port 27017 risks accidentally reading/writing into that unrelated
project's database.

**This project's local dev MongoDB runs on port 27018 instead:**
```bash
docker run -d \
  --name htmadvisory-mongo-dev \
  -p 27018:27017 \
  -v htmadvisory-mongo-data:/data/db \
  mongo:7
```

`application-dev.yml` is already configured to point at
`mongodb://localhost:27018/htmadvisory_dev` — do not change this back to
27017. If the container ever needs to be recreated, always map back to
`27018:27017` (host:container), never `27017:27017`.

**Note: this is separate from Testcontainers' MongoDB.** The `dev` profile
(running the actual app via `./run-dev.sh spring-boot:run`) uses the
persistent `htmadvisory-mongo-dev` container above on port 27018.
Integration TESTS (via `AbstractMongoIntegrationTest`) use a completely
different, throwaway MongoDB container that Testcontainers creates and
destroys automatically per test run, on a random port — the two never
collide and you do not need to start anything extra before running tests.

**Check if it's running:**
```bash
docker ps
```
You should see `htmadvisory-mongo-dev` with `0.0.0.0:27018->27017/tcp` in
the PORTS column.

**Inspect the database directly (useful for debugging Liquibase/data issues):**
```bash
docker exec -it htmadvisory-mongo-dev mongosh htmadvisory_dev --eval "db.getCollectionNames()"
docker exec -it htmadvisory-mongo-dev mongosh htmadvisory_dev --eval "db.people.getIndexes()"
docker exec -it htmadvisory-mongo-dev mongosh htmadvisory_dev --eval "db.people.find().pretty()"
```

### Liquibase + MongoDB — READ BEFORE TOUCHING ANY CHANGESET OR LIQUIBASE CONFIG

**Spring Boot's built-in Liquibase auto-configuration (`spring.liquibase.*`
properties) DOES NOT WORK with MongoDB and never will, in its current form.**
Confirmed via `--debug` logging (`LiquibaseAutoConfiguration` permanently
fails its activation condition because it requires a JDBC
`javax.sql.DataSource`, which doesn't exist in this MongoDB-only project)
and via a long-standing, explicitly **declined** Spring Boot issue
(https://github.com/spring-projects/spring-boot/issues/29991). Do not add
`spring.liquibase.url`, `spring.liquibase.change-log`, etc. to
`application.yml` expecting them to do anything.

**The fix in place:** `org.htmadvisory.platform.shared.MongoLiquibaseRunner`
manually runs Liquibase on application startup using Liquibase's own core
API directly against the MongoDB connection string, completely bypassing
Spring Boot's `SpringLiquibase` bean. **This class is load-bearing — do not
remove it or "simplify" it back toward `spring.liquibase.*` properties.**
The same runner/mechanism is exercised automatically inside integration
tests too (via `AbstractMongoIntegrationTest`'s Testcontainers MongoDB
instance), so tests validate against the identical schema-creation path
used in dev/prod.

**Changelog file location:** `src/main/resources/db/changelog-master.yaml`
(NOT `db/changelog/db.changelog-master.yaml`). Individual changesets live in
`src/main/resources/db/changelog/*.yaml`, `include`d by the master file, in
order.

**MongoDB-specific Liquibase quirk — index `name` is required in `options`,
not just as the sibling `indexName` YAML key:**
```yaml
# CORRECT
- createIndex:
    collectionName: people
    indexName: idx_people_email_unique
    keys: '{ "email": 1 }'
    options: '{ "unique": true, "name": "idx_people_email_unique" }'
```

**Current changesets (all verified working via both manual app boot AND
automated integration tests, in this exact order):**
```
src/main/resources/db/changelog-master.yaml
  └── db/changelog/001-create-people-collection.yaml
  └── db/changelog/002-create-people-email-index.yaml
  └── db/changelog/003-create-engagements-collection.yaml
  └── db/changelog/004-create-engagements-personid-index.yaml
```
Per the main CLAUDE.md's Liquibase Changeset Ordering: `profiles`,
`consent_records`, and `visits` collections/indexes come next, BEFORE
`contacts` — do not reorder.

## The `people` Domain — Current Code

```
src/main/java/org/htmadvisory/platform/people/
├── Person.java               # @Document(collection = "people")
├── Engagement.java            # @Document(collection = "engagements")
├── PersonRepository.java      # Spring Data auto-implemented; findByEmail()
├── EngagementRepository.java  # Spring Data auto-implemented; findByPersonId()
└── PersonService.java         # findOrCreateByEmail(), recordEngagement()

src/test/java/org/htmadvisory/platform/
├── shared/
│   └── AbstractMongoIntegrationTest.java   # Testcontainers base class
└── people/
    ├── PersonTestDataBuilder.java           # aPerson().withEmail(...).build()
    └── PersonServiceIntegrationTest.java    # 5 tests, all passing
```

**How every future domain should use the service layer** (not yet exercised
by real calling code — `contact` will be the first):
```java
Person person = personService.findOrCreateByEmail(email, name);
personService.recordEngagement(person.getId(), "contact", "inquiry_submitted", metadata);
```

**Fully verified, via automated tests (not just manual boot/log inspection
as of the prior update):**
- A new email creates a new `Person`
- The same email called twice returns the SAME `Person` — confirmed no
  duplicate is created (the unique index plus the find-first logic both work
  correctly together)
- `firstSeenAt` stays stable across repeat visits; `lastSeenAt` updates
  (test tolerates MongoDB's millisecond-precision truncation of `Instant`
  values — see note below)
- `recordEngagement()` correctly links an engagement to the right person
- **One person accumulates engagements across multiple DIFFERENT domains**
  (contact, survey, news) in one query — this is the actual cross-domain
  identity payoff the whole `people` design exists for, and it's now proven,
  not just designed

**Known test-writing gotcha (relevant to all future domains): MongoDB
truncates `Instant` to millisecond precision on write.** A Java `Instant`
captured via `Instant.now()` often has microsecond or nanosecond precision;
after a round-trip through MongoDB, the value read back will have only
millisecond precision. Do NOT assert exact equality (`isEqualTo`) on an
`Instant` field that has been written to and read back from MongoDB if you
also hold a reference to the original in-memory value — use
`isCloseTo(expected, within(1, ChronoUnit.SECONDS))` instead. This is not a
bug in the application code; it's expected MongoDB BSON datetime behavior.

### Running the app

```bash
# Default profile (no MongoDB profile-specific config — mostly for quick
# sanity checks, not real development):
./run-dev.sh spring-boot:run

# dev profile (connects to the local Docker MongoDB above, runs Liquibase
# changesets — use this for actual development):
./run-dev.sh spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running tests

```bash
./run-dev.sh test
```
First run on a clean machine will pull the `mongo:7` and
`testcontainers/ryuk` images — expect this to take longer than subsequent
runs. See "Testcontainers + Docker Desktop" above if this fails with a
Docker-connection error.

### Health check

Once running:
```bash
curl http://localhost:8080/actuator/health
```
Under the `dev` profile, a healthy response includes a `"mongo":{"status":"UP", ...}`
block — if that block is missing or shows `"DOWN"`, the app is not actually
talking to MongoDB even if the overall status says `"UP"` elsewhere; check
the Docker container is running first.

### Common gotcha: port 8080 already in use

If `spring-boot:run` fails with `Port 8080 was already in use`, a previous
run of this same app is probably still alive in another terminal tab. This
has happened repeatedly. Find and kill it:
```bash
lsof -i :8080
kill <PID shown above>
```
**If a plain `kill` does not actually stop the process (verify with `lsof -i
:8080` again — the PID may still show up), use `kill -9 <PID>` instead.**

**Check this FIRST whenever a run fails immediately with no other obvious
cause** — it's been the actual root cause more than once already.

## Notes

- Never commit real connection strings or credentials. Use environment
  variables (`${MONGODB_URI}` etc.) once real cloud infrastructure exists.
- The `income-request-mongodb` container belongs to a different, unrelated
  project on this machine — do not stop, remove, or modify it.
- **`pom.xml` structural gotcha already hit once:** when adding new
  dependencies via scripted text insertion, double check they land inside
  the actual `<dependencies>` block, NOT inside `<dependencyManagement>`.
  `<dependencyManagement>` only declares default versions — it does not pull
  a dependency into the build unless it's ALSO listed under
  `<dependencies>`. Always verify with `./run-dev.sh dependency:tree | grep
  -i <artifact>` after adding a new dependency.
- **`~/.docker-java.properties` and `~/.testcontainers.properties` are
  machine-level files, NOT part of this repo** (they live in the home
  directory, outside any git-tracked folder, and are intentionally not
  committed). If tests fail with Docker-connection errors on a brand new
  machine, the FIRST thing to check is whether `~/.docker-java.properties`
  exists there with `api.version=1.45` — see "Testcontainers + Docker
  Desktop" above for the full diagnosis before re-debugging from scratch.
