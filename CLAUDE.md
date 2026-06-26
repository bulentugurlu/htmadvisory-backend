# CLAUDE.md — htmadvisory-backend

> This is the backend service repo. For the FULL architecture — domain model,
> environment strategy, testing strategy, CI/CD, JIRA integration, and the
> complete build order — see the `CLAUDE.md` in the `htmadvisory` (frontend)
> repo. This file is intentionally lightweight and backend-specific only.

## What This Repo Is

Spring Boot backend service for the HTM Advisory platform. Companion to
`htmadvisory` (frontend — React/Vite, contains the canonical CLAUDE.md).

## Stack

- **Language:** Java 25 (locally installed JDK; Spring Boot 3.4.x runs on it)
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
- **First domain to build: `people`** (foundational identity anchor), then
  `profile`/`consent`/`traffic` (also foundational), then `contact` (first
  domain-specific capability: `POST /api/contacts/inquiries`).

## Current Status

- [x] Repo created
- [x] Maven + Spring Boot skeleton (this commit)
- [x] Spring Profiles configured (`application.yml` + `application-dev.yml`)
- [ ] Verify app boots locally (`mvn spring-boot:run`)
- [ ] MongoDB Atlas dev cluster connected
- [ ] Liquibase + first changesets (people, engagements collections)
- [ ] Testing harness (Testcontainers, JUnit5/Mockito/AssertJ, PIT, Cucumber)
- [ ] `people` domain built
- [ ] `profile`, `consent`, `traffic` domains built
- [ ] `contact` domain + first endpoint (`POST /api/contacts/inquiries`)

## Local Development

```bash
mvn spring-boot:run
```

Health check (once running): `http://localhost:8080/actuator/health`

## Running with the dev profile explicitly

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Notes

- `application-dev.yml` currently points to a placeholder local MongoDB URI
  (`mongodb://localhost:27017/htmadvisory_dev`) — this is intentional until
  a real MongoDB Atlas dev cluster is provisioned. Do not treat this as a
  real working connection yet.
- Never commit real connection strings or credentials. Use environment
  variables (`${MONGODB_URI}` etc.) once real infrastructure exists.
