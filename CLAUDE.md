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
- **Build:** Maven

## Architecture Convention (summary — see main CLAUDE.md for full detail)

- **Domain-based, capability-centric.** Code is organized by business domain
  (`org.htmadvisory.platform.contact`, `.people`, `.survey`, etc.), not by
  technical layer. Endpoints are named for what the caller is doing
  (`POST /api/contacts/inquiries`), not for the database table touched.
- **`shared/` is cross-cutting only** — the environment-token interceptor,
  common exception handling. Never a dumping ground.
- **Build order: `people`/`profile`/`consent`/`traffic` (foundational
  identity/marketing domains) FIRST, then `contact`** (first domain-specific
  capability: `POST /api/contacts/inquiries`). Confirmed 2026-06-27 — do not
  reorder this without revisiting the main CLAUDE.md's reasoning.

## Current Status (updated 2026-06-27)

- [x] Repo created
- [x] Maven + Spring Boot skeleton
- [x] Spring Profiles configured (`application.yml` + `application-dev.yml`)
- [x] Verify app boots locally with the `default` profile
- [x] **Java 25 vs. Spring Boot Maven plugin incompatibility found and fixed**
      — see "Java Version" section below
- [x] **Local dev MongoDB running in Docker, app verified connected to it
      under the `dev` profile** — see "Local MongoDB (Docker)" section below
- [ ] MongoDB Atlas dev cluster connected (cloud — separate from local Docker
      MongoDB above; not yet provisioned)
- [ ] Liquibase + first changesets (people, engagements, profiles,
      consent_records, visits collections — in that order, per main
      CLAUDE.md's Liquibase Changeset Ordering)
- [ ] Testing harness (Testcontainers, JUnit5/Mockito/AssertJ, PIT, Cucumber)
- [ ] `people` domain built
- [ ] `profile`, `consent`, `traffic` domains built
- [ ] `contact` domain + first endpoint (`POST /api/contacts/inquiries`)
- [ ] Terraform module + `htmadvisory-dev` GCP project created from code
- [ ] AWS replication of the same Terraform module pattern (confirmed
      2026-06-27 as a near-term goal, sequenced AFTER GCP dev is fully
      proven — see main CLAUDE.md's "Multi-cloud requirement — REFINED")

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

If `mvn -version` (run directly, without the wrapper) ever shows Java 25
again, that means something reset and this fix needs to be reapplied — it is
not a one-time environment fix, it's pinned at the project level intentionally.

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

### Running the app

```bash
# Default profile (no MongoDB profile-specific config — mostly for quick
# sanity checks, not real development):
./run-dev.sh spring-boot:run

# dev profile (connects to the local Docker MongoDB above — use this for
# actual development):
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
has happened before — `Ctrl+C` does not always reliably kill the forked
Maven/Spring Boot process). Find and kill it:
```bash
lsof -i :8080
kill <PID shown above>
```

## Notes

- Never commit real connection strings or credentials. Use environment
  variables (`${MONGODB_URI}` etc.) once real cloud infrastructure exists —
  the current `application-dev.yml` local Docker URI has no credentials and
  is safe to commit as-is, but this changes once MongoDB Atlas or any cloud
  database is introduced.
- The `income-request-mongodb` container belongs to a different, unrelated
  project on this machine — do not stop, remove, or modify it as part of any
  htmadvisory-backend work.
