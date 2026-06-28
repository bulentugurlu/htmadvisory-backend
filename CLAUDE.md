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
  + MongoDB" section below — this is a real, non-obvious gap and the fix is
  load-bearing for the rest of this codebase)
- **Build:** Maven

## Architecture Convention (summary — see main CLAUDE.md for full detail)

- **Domain-based, capability-centric.** Code is organized by business domain
  (`org.htmadvisory.platform.contact`, `.people`, `.survey`, etc.), not by
  technical layer. Endpoints are named for what the caller is doing
  (`POST /api/contacts/inquiries`), not for the database table touched.
- **`shared/` is cross-cutting only** — the environment-token interceptor,
  common exception handling, AND the `MongoLiquibaseRunner` (see below).
  Never a dumping ground.
- **Build order: `people`/`profile`/`consent`/`traffic` (foundational
  identity/marketing domains) FIRST, then `contact`** (first domain-specific
  capability: `POST /api/contacts/inquiries`).

## Current Status (updated 2026-06-27)

- [x] Repo created
- [x] Maven + Spring Boot skeleton
- [x] Spring Profiles configured (`application.yml` + `application-dev.yml`)
- [x] Java 25 vs. Spring Boot Maven plugin incompatibility found and fixed
      (Java 21 pinned, `run-dev.sh` wrapper)
- [x] Local dev MongoDB running in Docker, app verified connected under the
      `dev` profile (port 27018 — see "Local MongoDB" below)
- [x] **Liquibase dependencies correctly added to `pom.xml`** (fixed a real
      structural bug — see "Liquibase + MongoDB" below)
- [x] **Liquibase running successfully against MongoDB via a manual runner**
      — `people` and `engagements` collections created, both indexes
      verified correct (`idx_people_email_unique` unique on `email`,
      `idx_engagements_personid` on `personId`)
- [ ] MongoDB Atlas dev cluster connected (cloud — separate from local Docker
      MongoDB above; not yet provisioned)
- [ ] Testing harness (Testcontainers, JUnit5/Mockito/AssertJ, PIT, Cucumber)
- [ ] `Person` and `Engagement` Java model classes (next immediate step —
      collections/indexes exist, but no Java code defines their shape yet)
- [ ] `people` domain service layer (`PersonService.findOrCreateByEmail()`, etc.)
- [ ] `profile`, `consent`, `traffic` domains built
- [ ] `contact` domain + first endpoint (`POST /api/contacts/inquiries`)
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
```

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
```

### Liquibase + MongoDB — READ BEFORE TOUCHING ANY CHANGESET OR LIQUIBASE CONFIG

**Spring Boot's built-in Liquibase auto-configuration (`spring.liquibase.*`
properties) DOES NOT WORK with MongoDB and never will, in its current form.**
This was confirmed two ways: (1) `--debug` logging showed
`LiquibaseAutoConfiguration` permanently failing its activation condition
because it requires a JDBC `javax.sql.DataSource`, which doesn't exist in
this MongoDB-only project; (2) this is a long-standing, explicitly **declined**
issue in Spring Boot itself
(https://github.com/spring-projects/spring-boot/issues/29991) — the
maintainers decided not to fix it. Do not add `spring.liquibase.url`,
`spring.liquibase.change-log`, etc. to `application.yml` expecting them to
do anything — they are silently ignored for this project.

**The fix in place:** `org.htmadvisory.platform.shared.MongoLiquibaseRunner`
manually runs Liquibase on application startup using Liquibase's own core
API (`liquibase.Liquibase`, `DatabaseFactory.openDatabase(...)`) directly
against the MongoDB connection string, completely bypassing Spring Boot's
`SpringLiquibase` bean. It listens for `ApplicationReadyEvent` and runs
`liquibase.update()` against `classpath:db/changelog-master.yaml`. **This
class is load-bearing — do not remove it or "simplify" it back toward
`spring.liquibase.*` properties.**

**Changelog file location:** `src/main/resources/db/changelog-master.yaml`
(note: NOT `db/changelog/db.changelog-master.yaml` as some Liquibase docs
assume — that nested path is a convention for the JDBC-based Spring Boot
integration we are NOT using). Individual changesets live in
`src/main/resources/db/changelog/*.yaml` and are `include`d by the master
file, in order.

**MongoDB-specific Liquibase quirk #2 — index `name` is required in
`options`, not just as the sibling `indexName` YAML key:**
```yaml
# WRONG — fails with "The 'name' field is a required property of an
# index specification" (MongoCommandException, error code 9)
- createIndex:
    collectionName: people
    indexName: idx_people_email_unique
    keys: '{ "email": 1 }'
    options: '{ "unique": true }'

# CORRECT — name must ALSO be inside options
- createIndex:
    collectionName: people
    indexName: idx_people_email_unique
    keys: '{ "email": 1 }'
    options: '{ "unique": true, "name": "idx_people_email_unique" }'
```
Every future `createIndex` changeset must include `name` inside `options`,
matching `indexName`, or it will fail identically.

**Current changesets (all verified working, in this exact order):**
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

### Running the app

```bash
# Default profile (no MongoDB profile-specific config — mostly for quick
# sanity checks, not real development):
./run-dev.sh spring-boot:run

# dev profile (connects to the local Docker MongoDB above, runs Liquibase
# changesets — use this for actual development):
./run-dev.sh spring-boot:run -Dspring-boot.run.profiles=dev
```

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
run of this same app is probably still alive in another terminal tab (this
has happened multiple times — `Ctrl+C` does not always reliably kill the
forked Maven/Spring Boot process). Find and kill it:
```bash
lsof -i :8080
kill <PID shown above>
```
**Check this FIRST whenever a run fails immediately with no other obvious
cause** — it's been the actual root cause more than once already.

## Notes

- Never commit real connection strings or credentials. Use environment
  variables (`${MONGODB_URI}` etc.) once real cloud infrastructure exists —
  the current `application-dev.yml` local Docker URI has no credentials and
  is safe to commit as-is, but this changes once MongoDB Atlas or any cloud
  database is introduced.
- The `income-request-mongodb` container belongs to a different, unrelated
  project on this machine — do not stop, remove, or modify it as part of any
  htmadvisory-backend work.
- **`pom.xml` structural gotcha already hit once:** when adding new
  dependencies via scripted text insertion, double check they land inside
  the actual `<dependencies>` block, NOT inside `<dependencyManagement>`.
  `<dependencyManagement>` only declares default versions — it does not
  pull a dependency into the build unless it's ALSO listed under
  `<dependencies>`. This exact mistake caused Liquibase to silently not be
  on the classpath despite `mvn dependency:resolve` succeeding. Always
  verify with `./run-dev.sh dependency:tree | grep -i <artifact>` after
  adding a new dependency — a clean `dependency:resolve` is NOT sufficient
  proof a dependency actually made it into the build.
