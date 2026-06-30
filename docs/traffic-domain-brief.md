# Traffic Domain — Implementation Brief (v2, corrected against locked architecture docs)

> **Note:** this brief was originally drafted before the `docs/ARCHITECTURE.md`,
> `docs/BUILD_ORDER.md`, and `docs/DECISIONS.md` design (locked 2026-06-23,
> under "Marketing & Engagement Data Model: Four Domains, Not One") was found
> in the `htmadvisory` frontend repo. This version replaces the earlier draft
> — the model shape below is materially different (session-based, not
> single-page-view) and should be treated as authoritative.

## Goal
Add a `traffic` domain to `htmadvisory-backend`: anonymous-until-matched
website session/visit tracking, per the locked design in
`htmadvisory` (frontend) repo's `docs/ARCHITECTURE.md`, section
"`traffic` — Anonymous-Until-Matched Session & Visit Data." Follow the exact
same pattern used for `people`, `profile`, and `consent`: model → repository
→ service → Testcontainers integration test → Liquibase changeset.

## Before starting
1. Read `docs/ARCHITECTURE.md`, `docs/BUILD_ORDER.md`, and `docs/DECISIONS.md`
   in the `htmadvisory` (frontend) repo for the full locked design and
   rationale — do not treat this brief as the primary spec, treat it as a
   summary of those docs.
2. Read the existing `consent` domain end-to-end (model, repository, service,
   test, changeset) and mirror its conventions exactly — package structure,
   builder style, naming, annotations, test data builder pattern.
3. Read the `people` domain's `PersonService.findOrCreateByEmail()` —
   `traffic` needs to hook into this for the retroactive back-fill mechanism
   (see below), so understand its current signature and call sites before
   changing it.

## Model: `Visit`
A `Visit` represents a browsing **session**, not a single page hit — multiple
page views within the same session accumulate onto one `Visit` record via
`pagesViewed`.

```java
// traffic/Visit.java
public class Visit {
    private String id;
    private String sessionId;        // generated client-side or at first
                                      // request; groups multiple page views
                                      // into one visit
    private String personId;         // NULLABLE — most visits start with no
                                      // known Person; populated retroactively
                                      // if/when that session later provides
                                      // an email (see back-fill mechanism)
    private String ipAddress;        // captured server-side from the
                                      // request, not client-supplied
    private String city;             // derived from ipAddress via geolocation
    private String serviceProvider;  // derived from ipAddress (ISP) — field
                                      // name is `serviceProvider`, NOT `isp`
    private String deviceType;       // derived from user agent:
                                      // desktop/mobile/tablet
    private String browser;          // derived from user agent
    private Instant startedAt;
    private Instant lastActivityAt;
    private List<String> pagesViewed;
}
```

Do NOT model this as a flat single-page-view record with a `path` field —
that was an earlier, incorrect draft of this brief. `pagesViewed` is a list
that grows as the same session continues browsing.

## The anonymous-until-matched mechanism (core to this domain — do not omit)
A `Visit` is created the moment someone lands on the site, with `personId`
left null. If, later in that same session (or a return visit using the same
`sessionId`), the visitor submits the Contact form, takes the Survey, or
shares an article — any flow that calls `personService.findOrCreateByEmail()`
— that call must ALSO trigger a retroactive update: the current and recent
`Visit` record(s) for that session get stamped with the now-known `personId`.

Implementation approach to confirm with Claude Code during the build (not
fully prescribed here): this likely means `TrafficService` exposes something
like `backfillPersonId(String sessionId, String personId)`, and
`PersonService.findOrCreateByEmail()` (or its callers) invoke it once a
session is matched to an email. Confirm the cleanest place for this call to
live — probably inside `PersonService` taking a `sessionId` parameter, or as
a follow-up call from each domain's calling code (contact, survey, news) —
without creating a circular dependency between `people` and `traffic`.
Flag the chosen approach clearly in the PR description since this is the one
piece of cross-domain coupling in an otherwise foundational, dependency-free
domain.

## Geolocation lookup — provider: MaxMind GeoLite2
Populates `city` and `serviceProvider` from `ipAddress`. Implement as a
separate, injectable component (`GeoLocationService` interface, with a
`MaxMindGeoLocationService` implementation) called from `TrafficService`, not
inlined into the controller or repository. Rationale: local DB lookup means
no per-request network call, no rate limits, no external outage risk on every
visit write.

Both fields are nullable — the lookup can fail (private/local IPs, missing
database, lookup error) and the service must degrade gracefully, still
recording the `Visit` with `city`/`serviceProvider` null rather than failing
the write.

Setup notes (scaffold now, doesn't need to be fully wired up):
- `com.maxmind.geoip2:geoip2` Java library.
- Requires a free MaxMind account to download GeoLite2-City (covers city)
  and GeoLite2-ISP (covers `serviceProvider`) — separate databases, same
  account.
- `.mmdb` files need to live somewhere the app can read at runtime —
  flag as a deployment concern for Cloud Run, not just local dev.
- Data goes stale over months; a periodic refresh process will eventually be
  needed — not required this pass, don't let it get forgotten.
- The Testcontainers integration test must NOT depend on a real MaxMind DB
  or network access — mock/stub `GeoLocationService` so tests stay
  deterministic.

## Service: `TrafficService`
No update-via-merge semantics like `profile`'s null-preserving pattern.
Core methods, at minimum:
- `recordVisit(...)` — creates a new `Visit` or appends a page to an existing
  one for the same active `sessionId` (confirm "active" window with Claude
  Code — e.g. same session if `lastActivityAt` is within some reasonable
  threshold; not explicitly specified in the locked design, use judgment and
  flag the choice).
- `backfillPersonId(String sessionId, String personId)` — the retroactive
  matching mechanism described above.
- Basic read/query methods (by `sessionId`, by `personId`, by date range).

## Capability-centric endpoint
```
POST /api/traffic/visits   — record or update a visit (called on page load)
```
Matches the existing convention (`POST /api/consent/email-opt-in`, etc.) —
named for the action, not a generic REST resource path.

## Repository
Spring Data MongoDB repository. Add at minimum:
- `findBySessionId(String sessionId)`
- `findByPersonId(String personId)`
- `findByStartedAtBetween(Instant start, Instant end)`

## Liquibase
Exact changeset numbers per the locked design (confirm `consent`'s actual
changesets landed as 007–008 before assuming — check
`db/changelog-master.yaml` first):
```yaml
# 009-create-visits-collection.yaml
# 010-create-visits-sessionid-index.yaml
# 011-create-visits-personid-index.yaml   (sparse — most personId are null)
```
Use the same MongoDB Liquibase index syntax already established (index
`name` required inside `options`, not just as the sibling `indexName` key).

## Testcontainers integration test
**Critical — follow the established pattern exactly:**
- Use a `static { mongoDBContainer.start(); }` initializer block.
- Do NOT use `@Testcontainers` or `@Container` annotations — causes Spring's
  `ApplicationContext` caching to reuse a stale `MongoClient` from a
  destroyed container on the next test class, producing `Connection refused`.
- For any timestamp assertions, use `isCloseTo(expected, within(1,
  ChronoUnit.SECONDS))` — never `isEqualTo()`, since MongoDB truncates
  `Instant` to millisecond precision.

Cover, at minimum:
- Creating a new `Visit` for a fresh `sessionId` with `personId` null.
- A second `recordVisit()` call for the same active `sessionId` appends to
  `pagesViewed` rather than creating a duplicate `Visit`.
- `backfillPersonId()` correctly stamps `personId` onto an existing `Visit`
  found by `sessionId`.
- Retrieving by `sessionId`, by `personId`, by date range.
- `Visit` still saves successfully when the (mocked) `GeoLocationService`
  returns null/fails — write must not be blocked by geolocation failure.

## Test data builder
Add a `VisitTestDataBuilder` mirroring the `profile`/`consent` test data
builders, with sensible defaults and `withSessionId()`, `withPersonId()`,
`withIpAddress()` etc. builder methods.

## Privacy note (not blocking, don't let it get forgotten)
Raw IP address and ISP are personal/sensitive data in many jurisdictions
(GDPR treats IP as personal data). A privacy notice, retention policy, and
legal review before production are open items — already flagged in the
locked design docs, repeating here so it isn't lost in implementation.

## Definition of done
- All new tests pass alongside the existing 15 (people + profile + consent),
  so 15 + N total, zero flakes.
- No regressions in the existing three domains, especially
  `PersonService.findOrCreateByEmail()` if it's modified to support the
  back-fill hook.
- Liquibase changesets 009–011 apply cleanly on a fresh container, in order.
