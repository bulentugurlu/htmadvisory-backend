# Website Audit Framework — Implementation Brief

> **Status:** New domain — not yet built. This brief covers the full
> implementation: backend audit engine, PostgreSQL integration, Claude AI
> enrichment, Playwright headless browser, and frontend audit page.
>
> **Repos:** Backend in `htmadvisory-backend`, frontend in `htmadvisory-frontend`.
> **Read first:** `CLAUDE.md` (backend), `docs/ARCHITECTURE.md`, `docs/BUILD_ORDER.md`,
> `docs/DECISIONS.md` in the frontend repo before implementing anything.

---

## 1. Purpose

A private, extensible website audit tool for HTM Advisory's consulting practice.
A CEO enters a competitor's or prospect's URL, selects one or more audit types
(SEO, Accessibility, Usability, Performance — more added over time), and receives
a scored report with Claude-generated, CEO-friendly recommendations they can hand
directly to their CTO or CIO.

Each audit run is persisted to PostgreSQL for historical querying — useful before
client meetings ("what did we find when we audited Acme Corp last month?").

---

## 2. Architecture Overview

### Backend — New `audit` domain

```
org.htmadvisory.platform.audit/
├── AuditController.java          # POST /api/audits/run, GET /api/audits/{id}, GET /api/audits
├── AuditRequest.java             # DTO: { url, companyName, requestedByEmail, auditTypes[] }
├── AuditResponse.java            # DTO: full audit result returned to frontend
├── AuditService.java             # Orchestrates: fetch → audit → enrich → persist
├── model/
│   ├── Audit.java                # PostgreSQL entity — top-level audit run
│   ├── AuditDimension.java       # PostgreSQL entity — one scored dimension
│   ├── AuditFinding.java         # PostgreSQL entity — individual finding within a dimension
│   └── AuditStatus.java         # Enum: PENDING, RUNNING, COMPLETED, FAILED
├── repository/
│   ├── AuditRepository.java      # JPA repository for Audit
│   ├── AuditDimensionRepository.java
│   └── AuditFindingRepository.java
├── fetcher/
│   └── PlaywrightPageFetcher.java # Headless browser fetch — returns parsed HTML + metadata
├── auditors/
│   ├── Auditor.java              # Interface: String name(); String type(); DimensionResult audit(PageContent page);
│   ├── DimensionResult.java      # Value object: dimensionName, score(0-100), findings[]
│   ├── FindingResult.java        # Value object: severity(HIGH/MEDIUM/LOW), finding, rawData
│   ├── seo/
│   │   ├── MetaTagsAuditor.java
│   │   ├── OpenGraphAuditor.java
│   │   ├── SchemaOrgAuditor.java
│   │   ├── GeoContentAuditor.java
│   │   └── RobotsTxtAuditor.java
│   └── accessibility/
│       ├── AltTextAuditor.java
│       ├── HeadingStructureAuditor.java
│       ├── LangAttributeAuditor.java
│       └── AriaLandmarksAuditor.java
└── enricher/
    └── ClaudeAuditEnricher.java  # Calls Claude API — turns raw findings into recommendations
```

### Frontend — New `/audit` page

```
src/pages/Audit.jsx               # URL input, audit type selection, results dashboard
src/pages/AuditHistory.jsx        # Table of past audit runs (optional, phase 2)
```

---

## 3. PostgreSQL Setup

### Why PostgreSQL for this domain

Audit results are relational and analytical — one audit has many dimensions, each
dimension has many findings. Future queries like "show audits for companies with
SEO score < 50" or "compare two audits for the same domain" are natural SQL but
awkward in MongoDB. The existing 5 domains stay on MongoDB — this is additive.

### Spring Boot dual-datasource configuration

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Add to `application-dev.yml` (use env var, never hardcode):
```yaml
spring:
  datasource:
    url: ${POSTGRESQL_URL:jdbc:postgresql://localhost:5432/htmadvisory_dev}
    username: ${POSTGRESQL_USER:htmadvisory}
    password: ${POSTGRESQL_PASSWORD:}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

Use Flyway (NOT Liquibase) for PostgreSQL schema migrations — Liquibase is wired
for MongoDB in this project and adding it for PostgreSQL creates config conflicts.
Flyway is the standard choice for JPA/PostgreSQL projects.

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Flyway migration files go in `src/main/resources/db/migration/` (separate from
the existing `db/changelog/` directory which is MongoDB/Liquibase):
```
V1__create_audits_table.sql
V2__create_audit_dimensions_table.sql
V3__create_audit_findings_table.sql
```

### Database Schema

```sql
-- V1__create_audits_table.sql
CREATE TABLE audits (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url                 VARCHAR(2048) NOT NULL,
    company_name        VARCHAR(255),
    requested_by_email  VARCHAR(255),
    overall_score       INTEGER,
    overall_grade       VARCHAR(2),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audit_types         TEXT[],              -- e.g. {'SEO','ACCESSIBILITY'}
    claude_summary      TEXT,                -- Claude's executive summary
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at        TIMESTAMP WITH TIME ZONE
);

-- V2__create_audit_dimensions_table.sql
CREATE TABLE audit_dimensions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id        UUID NOT NULL REFERENCES audits(id) ON DELETE CASCADE,
    audit_type      VARCHAR(50) NOT NULL,    -- SEO, ACCESSIBILITY, etc.
    dimension_name  VARCHAR(255) NOT NULL,   -- Meta Tags, Open Graph, etc.
    score           INTEGER NOT NULL,        -- 0-100
    grade           VARCHAR(2) NOT NULL,     -- A/B/C/D/F
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX idx_audit_dimensions_audit_id ON audit_dimensions(audit_id);

-- V3__create_audit_findings_table.sql
CREATE TABLE audit_findings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dimension_id        UUID NOT NULL REFERENCES audit_dimensions(id) ON DELETE CASCADE,
    severity            VARCHAR(10) NOT NULL, -- HIGH, MEDIUM, LOW
    finding             TEXT NOT NULL,        -- what was found (machine-readable)
    recommendation      TEXT,                 -- Claude-generated recommendation
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX idx_audit_findings_dimension_id ON audit_findings(dimension_id);
```

---

## 4. Headless Browser — Playwright

Use `com.microsoft.playwright:playwright` Java library. Do NOT use Selenium —
Playwright is faster, more reliable, and has better SPA support.

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.44.0</version>
</dependency>
```

### PlaywrightPageFetcher

```java
@Component
public class PlaywrightPageFetcher {

    public PageContent fetch(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();
            page.navigate(url, new Page.NavigateOptions()
                .setTimeout(15000)
                .setWaitUntil(WaitUntilState.NETWORKIDLE));

            String html = page.content();
            String title = page.title();
            String finalUrl = page.url();

            // Also fetch robots.txt separately
            String robotsUrl = extractBaseUrl(url) + "/robots.txt";
            String robotsTxt = fetchRobotsTxt(robotsUrl);

            browser.close();
            return new PageContent(html, title, finalUrl, robotsTxt);
        }
    }
}
```

`PageContent` is a simple record/value object holding: `html`, `title`,
`finalUrl`, `robotsTxt`. Auditors receive this object — they never fetch URLs
themselves.

**Graceful degradation:** if Playwright fails (timeout, blocked, invalid URL),
catch the exception and return a `PageContent` with empty/null fields. The
auditors handle null gracefully and produce LOW scores with a finding like
"Page could not be fetched — site may block automated access."

**Docker consideration:** Playwright requires Chromium browser binaries. Add
to the Dockerfile's runtime stage:
```dockerfile
RUN apt-get update && apt-get install -y \
    chromium \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
ENV PLAYWRIGHT_BROWSERS_PATH=/usr/bin/chromium
```

---

## 5. Auditor Interface & Implementations

### Interface

```java
public interface Auditor {
    String name();           // "Meta Tags"
    String auditType();      // "SEO"
    DimensionResult audit(PageContent page);
}
```

### SEO Auditors

**MetaTagsAuditor** — checks for:
- `<title>` present and 30-60 chars: +25 points
- `<meta name="description">` present and 120-160 chars: +25 points
- `<meta name="robots">` not blocking indexing: +25 points
- `<link rel="canonical">` present: +25 points

**OpenGraphAuditor** — checks for:
- `og:title`, `og:description`, `og:image`, `og:url` all present: 25 points each

**SchemaOrgAuditor** — checks for:
- Any `<script type="application/ld+json">` present: +40 points
- Contains `@type` of Organization, ProfessionalService, or LocalBusiness: +30 points
- Contains `name`, `description`, `url`: +30 points

**GeoContentAuditor** — checks for:
- FAQPage schema present: +40 points
- robots.txt allows GPTBot: +20 points
- robots.txt allows PerplexityBot: +20 points
- robots.txt allows Claude-Web: +20 points

**RobotsTxtAuditor** — checks for:
- robots.txt accessible (200 response): +30 points
- Sitemap declared in robots.txt: +40 points
- No `Disallow: /` for `*`: +30 points

### Accessibility Auditors

**AltTextAuditor** — checks for:
- All `<img>` tags have non-empty `alt` attribute
- Score = percentage of images with alt text

**HeadingStructureAuditor** — checks for:
- Exactly one `<h1>` present: +40 points
- Headings in logical order (h1→h2→h3, no skips): +30 points
- At least one heading present: +30 points

**LangAttributeAuditor** — checks for:
- `<html lang="...">` present and non-empty: 100 or 0

**AriaLandmarksAuditor** — checks for:
- `<main>` or `role="main"` present: +25 points
- `<nav>` or `role="navigation"` present: +25 points
- `<header>` or `role="banner"` present: +25 points
- `<footer>` or `role="contentinfo"` present: +25 points

---

## 6. Claude Enrichment

After all auditors run, pass the full set of findings to Claude for enrichment.
Claude produces: per-finding recommendations + an executive summary.

```java
@Component
public class ClaudeAuditEnricher {

    private final RestTemplate restTemplate;

    public EnrichmentResult enrich(String url, String companyName,
                                    List<DimensionResult> dimensions) {
        String findingsSummary = buildFindingsSummary(dimensions);

        String prompt = """
            You are an expert digital consultant advising a CEO.
            
            We audited the website: %s
            Company: %s
            
            Here are the raw technical findings:
            %s
            
            Your task:
            1. Write a 3-4 sentence executive summary a CEO can understand,
               explaining what the overall findings mean for their business.
               Focus on business impact, not technical jargon.
            
            2. For each finding marked HIGH or MEDIUM severity, write a
               1-2 sentence recommendation in plain English that a CEO can
               hand to their CTO or CIO. Start each with an action verb.
               Format as bullet points.
            
            3. End with a 2-sentence closing on why fixing these issues matters
               for competitive positioning and AI-era visibility.
            
            Respond in this exact JSON format:
            {
              "executiveSummary": "...",
              "recommendations": [
                { "finding": "exact finding text", "recommendation": "..." }
              ],
              "closingStatement": "..."
            }
            """.formatted(url, companyName != null ? companyName : "Unknown", findingsSummary);

        // Call Claude API (claude-sonnet-4-6)
        // Parse JSON response
        // Return EnrichmentResult
    }
}
```

Use the Anthropic API directly via RestTemplate — no SDK needed. Model:
`claude-sonnet-4-6`. Max tokens: 2000. Store the API key as `ANTHROPIC_API_KEY`
environment variable on Cloud Run.

---

## 7. API Endpoints

### POST /api/audits/run
Request:
```json
{
  "url": "https://acmecorp.com",
  "companyName": "Acme Corporation",
  "requestedByEmail": "bulent@htmadvisory.org",
  "auditTypes": ["SEO", "ACCESSIBILITY"]
}
```

Response (202 Accepted — audit runs async):
```json
{
  "auditId": "uuid",
  "status": "RUNNING",
  "message": "Audit started — check /api/audits/{id} for results"
}
```

The audit runs asynchronously (`@Async`) since Playwright + Claude can take
10-30 seconds. The frontend polls `/api/audits/{id}` every 2 seconds until
status is COMPLETED or FAILED.

### GET /api/audits/{id}
Returns full audit result including all dimensions, findings, and
Claude recommendations once status = COMPLETED.

### GET /api/audits
Returns list of past audits (id, url, companyName, overallScore, overallGrade,
status, createdAt) — for the audit history view.

---

## 8. Frontend — Audit.jsx

### Layout
```
/audit page (private — add to nav behind login check later)

┌─────────────────────────────────────────────┐
│  Website Audit Tool                         │
│  ─────────────────────────────────────────  │
│  URL: [https://acmecorp.com          ] [▶]  │
│  Company: [Acme Corp] Email: [me@...]       │
│  ☑ SEO Audit   ☑ Accessibility Audit        │
│  [Run Audit]                                │
└─────────────────────────────────────────────┘

[While running: animated progress bar + "Analyzing..."]

[Results:]
┌─── Overall Score: 42/100  Grade: D ──────────┐
│  🔴 SEO: 38/100    🟡 Accessibility: 58/100  │
└──────────────────────────────────────────────┘

[Executive Summary from Claude]

[Per-dimension cards, expandable:]
  Meta Tags: 25/100 ████░░░░░░ F
    ● HIGH: No meta description found
      → Recommendation: ...
    ● MEDIUM: Title tag is 8 characters...
      → Recommendation: ...

[CTA: "HTM Advisory can fix these — Contact us →"]
```

### Polling logic
```javascript
const runAudit = async () => {
  const { auditId } = await POST('/api/audits/run', payload)
  setStatus('RUNNING')
  const poll = setInterval(async () => {
    const result = await GET(`/api/audits/${auditId}`)
    if (result.status === 'COMPLETED' || result.status === 'FAILED') {
      clearInterval(poll)
      setAudit(result)
      setStatus(result.status)
    }
  }, 2000)
}
```

---

## 9. Local PostgreSQL for Development

Run PostgreSQL locally via Docker for dev testing:
```bash
docker run -d \
  --name htmadvisory-postgres \
  -e POSTGRES_DB=htmadvisory_dev \
  -e POSTGRES_USER=htmadvisory \
  -e POSTGRES_PASSWORD=localdevonly \
  -p 5432:5432 \
  postgres:16-alpine
```

Add to `application-dev.yml` fallback:
```yaml
spring:
  datasource:
    url: ${POSTGRESQL_URL:jdbc:postgresql://localhost:5432/htmadvisory_dev}
    username: ${POSTGRESQL_USER:htmadvisory}
    password: ${POSTGRESQL_PASSWORD:localdevonly}
```

---

## 10. Testing

Integration tests use Testcontainers for BOTH MongoDB (existing pattern) AND
PostgreSQL (new). Add to test dependencies:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

Use the same singleton container pattern established for MongoDB — static init
block in a new `AbstractPostgresIntegrationTest.java`. Do NOT use
`@Testcontainers`/`@Container` annotations.

**Mock Playwright and Claude in tests** — do not make real HTTP calls or
browser launches in the test suite. Use `@MockBean` for `PlaywrightPageFetcher`
and `ClaudeAuditEnricher`. Test the auditor logic with hardcoded HTML strings.

**Test coverage required:**
- Each auditor with sample HTML covering found/not-found cases
- AuditService orchestration with mocked fetcher and enricher
- AuditController endpoint tests (MockMvc)
- PostgreSQL persistence via Testcontainers

---

## 11. Cloud Run Deployment

Add these environment variables to the backend Cloud Run service:
- `POSTGRESQL_URL` — Cloud SQL PostgreSQL connection string
- `POSTGRESQL_USER` — database user
- `POSTGRESQL_PASSWORD` — database password
- `ANTHROPIC_API_KEY` — Claude API key

**Cloud SQL (PostgreSQL on GCP):** provision a Cloud SQL PostgreSQL 16 instance
(smallest tier — db-f1-micro for dev, free tier eligible). This is separate from
MongoDB Atlas. Connection from Cloud Run to Cloud SQL uses the Cloud SQL connector
(no public IP needed).

---

## 12. Extensibility — Adding a New Audit Type

When adding Usability, Performance, Security, or any future audit:
1. Create `auditors/usability/` package
2. Implement `Auditor` interface for each dimension
3. Add `"USABILITY"` as a valid value in `AuditRequest` validation
4. Register the new auditors in `AuditService` (via Spring `@Component` + autowired list)
5. No changes to controller, database schema, or frontend framework needed

The frontend already shows any audit type returned by the backend — adding
a new audit type automatically appears in the results dashboard.

---

## 13. Definition of Done

- [ ] PostgreSQL + Flyway migrations run cleanly on startup
- [ ] All existing 38 MongoDB tests still pass (zero regressions)
- [ ] New audit tests pass (Testcontainers PostgreSQL)
- [ ] `POST /api/audits/run` returns 202, audit completes asynchronously
- [ ] `GET /api/audits/{id}` returns full result when COMPLETED
- [ ] All 5 SEO auditors scoring correctly against sample HTML
- [ ] All 4 accessibility auditors scoring correctly
- [ ] Claude enrichment producing executive summary and recommendations
- [ ] Audit.jsx renders results with scores, grades, and recommendations
- [ ] CTA links to /contact
- [ ] Deployed to Cloud Run dev with PostgreSQL Cloud SQL
