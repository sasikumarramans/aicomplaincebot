# AI Compliance Copilot — Backend Implementation Plan

## Context

Building the backend for a multi-tenant SaaS platform that helps companies manage,
track, verify, and renew compliance certificates and prepare for audits. The repo
currently contains only a bare Spring Boot skeleton. This document is the agreed
build plan, covering all 15 PRD modules, built in phases across multiple sessions.
Each phase should land as a working, independently testable increment.

## Confirmed decisions

- **Architecture**: pragmatic layered/hexagonal — `domain` entities carry JPA
  annotations directly (no parallel pure-POJO + infra-entity mapping layer);
  `application` holds use-case services and port interfaces; `infrastructure`
  holds adapters (persistence, security, storage, ai, notification, ocr, report);
  `presentation` holds controllers/DTOs/mappers. One shared layer structure with
  feature sub-packages inside each layer, not per-module vertical slices.
- **Package/group**: `com.aicompliance` (fixing the `aicomplaince` typo in the
  skeleton now, before any real code exists).
- **Database**: PostgreSQL + Flyway migrations + Spring Data JPA.
- **Auth**: JWT (JJWT library, self-issued/self-validated, custom filter — not
  Spring Security's OAuth2 resource server), BCrypt passwords, 5 fixed roles
  (`SUPER_ADMIN, COMPANY_ADMIN, COMPLIANCE_MANAGER, DEPARTMENT_HEAD, AUDITOR`) as
  an enum, not a table. Fine-grained `Permission` entity deferred until a concrete
  need surfaces.
- **File storage**: S3 SDK v2 behind `FileStorageService` port; MinIO in dev via
  docker-compose, real S3 in prod, same client (endpoint override).
- **Multi-tenancy**: shared schema, `company_id` on every tenant-scoped entity via
  `TenantOwnedEntity` base class, enforced with a Hibernate `@Filter` enabled
  per-session from `CompanyContextProvider` (reads JWT claims). SUPER_ADMIN bypasses
  the filter. Tenant isolation gets a dedicated, continuously-run integration test
  suite, not just a one-off check.
- **AI provider**: OpenAI via a thin WebClient wrapper, behind an `AiService` port
  with focused methods per use case (extraction, chat, summary, risk, renewal
  prediction, duplicate detection, score explanation) — not one generic method.
  Duplicate Detection and Missing Document Detection are primarily rule-based/
  relational; AI is only invoked for the narrative/ambiguous-case layer, to control
  token spend.
- **OCR text extraction**: PDFBox (embedded text layer) + Tess4J/Tesseract
  (scanned PDFs rendered to images + native JPG/PNG), composed behind an
  `OcrTextExtractor` port. Native Tesseract dependency baked into the Docker image
  and documented for local dev.
- **PDF reports**: openhtmltopdf + Thymeleaf templates (LGPL-safe for commercial
  SaaS, avoids iText 7's AGPL/commercial licensing).
- **Notifications**: real Email via Spring Mail/SMTP; SMS/WhatsApp/Push/Teams/Slack
  as logging-only stubs behind a common `NotificationChannel` interface (strategy
  map keyed by enum), ready to swap in real providers later. Escalation chain:
  Manager → Dept Head → Compliance Officer → Director.
- **Chat assistant grounding**: structured-query retrieval against the DB based on
  parsed intent (not vector-embedding RAG) — simpler and more reliable for tabular
  compliance data.
- **AuditLog**: write hooks threaded in starting Phase 1 (not bolted on at the end),
  even though the full reporting/export UI for it lands in the last phase.
- **Cross-industry, not manufacturing-only**: the PRD's example categories (Fire
  NOC, Boiler Certificate, Factory License, ...) are illustrative, not a fixed
  schema. `IndustryType` is a structured lookup table (rows, not a Java enum) so
  Manufacturing/Software/Healthcare/anything else are all just data. Certificate
  categories are **never pre-seeded** — every company starts with zero categories
  and defines their own (a hospital adds "Biomedical Waste Authorization", a
  software company adds "SOC 2 Report") via the same generic CRUD, no code change
  needed per industry. `IndustryType` may optionally carry a `CategoryTemplate`
  (starter pack) that a company can explicitly import later, but nothing is
  auto-applied at company creation — day-one behavior is fully generic/empty.
  No industry-specific business logic (categories, checklists, facility types)
  is ever hardcoded in Java; it all lives in company-owned data.

## Package structure

```
com.aicompliance
├── AiComplianceApplication.java
├── domain/{company,user,certificate,vendor,employee,compliance,notification,audit,chat,shared,exception}
├── application/{company,user,certificate,category,expiry,vendor,employee,dashboard,compliance,ai,notification,auditmode,report,port}
├── infrastructure/{persistence,security,storage,ocr,ai,notification,report,scheduling,config,exception}
└── presentation/{controller,dto/{request,response},mapper,advice}
```

Ports (interfaces) live in `application.port`: `FileStorageService`,
`OcrTextExtractor`, `AiService`, `NotificationChannel`, `PdfReportGenerator`,
`ArchiveExportService`, `CompanyContextProvider`. Spring Data repository interfaces
also live in `application.port` so application services depend only on interfaces.

## Core entities (high level)

`Company` (industryId FK → `IndustryType`), `Plant`, `User` (role enum, plantId
nullable, isTemporary+accessExpiresAt for AUDITOR), `IndustryType` (lookup, e.g.
Manufacturing/Software/Healthcare — company-independent, admin-managed, no
hardcoded set), `CertificateCategory` (per-company, custom, zero pre-seeded rows —
a hospital and a manufacturer each define their own), `CategoryTemplate`/
`CategoryTemplateItem` (optional per-IndustryType starter packs a company may
import on demand; never auto-applied), `Certificate`, `CertificateVersion`
(append-only, version history/archive), `Vendor`, `VendorDocument`, `Employee`,
`EmployeeCertification`, `ComplianceScore`, `RiskAssessment`, `AuditLog`,
`AuditFolder`, `Notification`, `ChatConversation`, `ChatMessage`, plus lookups
`Department` and `FacilityTypeRequiredCategory` (facility type is also
company-defined free text/lookup, not a fixed manufacturing-only list). All
tenant-scoped entities extend `TenantOwnedEntity` (companyId) → `AuditableEntity`
(id/createdAt/updatedAt/createdBy/updatedBy/deletedAt).

## Gradle dependencies (Phase 0)

Web + validation, Spring Security, JJWT (api/impl/jackson), Spring Data JPA +
PostgreSQL driver, Flyway (core + postgresql), Spring Mail, AWS S3 SDK v2 (BOM),
PDFBox, Tess4J, Apache POI (DOCX parsing), spring-boot-starter-webflux (WebClient
only, app stays MVC), openhtmltopdf + Thymeleaf, Lombok, MapStruct (+ lombok
binding), springdoc-openapi, spring-boot-configuration-processor, Testcontainers
(postgresql, junit-jupiter), spring-security-test. Verify actual current
Boot-4.1-compatible versions at build time rather than trusting pinned versions
from memory — this is a very recent Spring Boot/Framework major line.

## Phased build order

- **Phase 0 — Foundation**: gradle deps, `application.yml` (dev/test/prod
  profiles), `docker-compose.yml` (Postgres + MinIO), base entities
  (`AuditableEntity`/`TenantOwnedEntity`), `GlobalExceptionHandler`, Flyway
  baseline, OpenAPI config. Done when `bootRun` works against docker-compose,
  Flyway applies, `/actuator/health` and `/swagger-ui.html` respond.
- **Phase 1 — Auth, Company, RBAC**: `Company`/`Plant`/`User` + migration, JWT
  provider/filter/security config, `AuthService` (login/refresh/logout via
  DB-stored hashed refresh tokens), `CompanyService`/`PlantService`/`UserService`
  + controllers, Hibernate tenant filter wiring, `AuditLog` write hook introduced
  here. Done when SUPER_ADMIN creates a Company, its COMPANY_ADMIN logs in and
  creates other-role users, and a Testcontainers integration test proves
  cross-company data isolation.
- **Phase 2 — Industry/Categories (no seed data), Certificate upload (manual
  fields), Expiry Bucketing**: `IndustryType` lookup + migration, convert
  `Company.industryType` from free text to an `industryId` FK, `CertificateCategory`
  as fully custom per-company CRUD (zero pre-seeded rows — same code path serves
  a factory, a hospital, or a software company), `Certificate`/`CertificateVersion`,
  `S3FileStorageService`, upload/list/filter endpoints, `ExpiryBucketingService` +
  nightly scheduled job (30/15/7/3/0/Expired buckets, color codes).
- **Phase 3 — AI OCR + AI field extraction**: `OcrTextExtractor` (PDFBox +
  Tess4J composite), `AiService`/`OpenAiService` (starting with
  `extractCertificateFields`), async upload→OCR→AI-extract pipeline with a
  status-polling endpoint, auto category classification.
- **Phase 4 — Workflow + Dashboard v1** (complete): `CertificateReviewService`
  (approve/reject, blocks review of certs still missing category/name from a
  pending or failed AI extraction), `CertificateVersionService` (renewal = new
  version + archive old, resets status to PENDING_REVIEW and clears prior review),
  `DashboardAggregationService` + chart endpoints (summary counts, expiry trend
  over a 13-month rolling window, category distribution). The PRD's
  "department-wise compliance" chart is grouped by **Plant** instead - Certificate
  has no department field (only Plant/Category), so Plant is the closest real
  grouping available; revisit if Employee Compliance (Phase 6) makes department a
  first-class concept certificates could also carry.
- **Phase 5 — Vendor Compliance**: `Vendor`/`VendorDocument`, review flow, status
  rollup, dashboard vendor compliance % wired to real data.
- **Phase 6 — Employee Compliance** (complete): `Employee`/`EmployeeCertification`
  (6 fixed types per PRD: Safety Training, Forklift License, Welding Certificate,
  Electrical License, Medical Certificate, PPE Training - kept as an enum since
  the PRD names these specifically, unlike certificate categories). Generalized
  expiry bucketing: `ExpiryBucket` and a new `Expirable` interface moved to
  `domain.shared`; `Certificate` and `EmployeeCertification` both implement it;
  `ExpiryBucketingService`/`ExpiryScanJob` now recompute both in one run. Removed
  4 duplicate `computeBucket` methods in favor of `ExpiryBucket.fromExpiryDate()`.
  Dashboard summary extended with an employee-certification block (total,
  expiring-in-30-days, expired, valid%).
- **Phase 7 — Notifications + Escalation** (complete): `Notification` entity, all
  6 channel impls (real Email via JavaMailSender; SMS/WhatsApp/Push/Teams/Slack
  logging stubs behind a strategy map keyed by `NotificationChannelType`).
  `ExpiryBucketingService` now dispatches a real notification whenever a
  Certificate's bucket actually changes (not on every scan - only on genuine
  threshold crossings), to the company's COMPLIANCE_MANAGER (falling back to
  COMPANY_ADMIN). `EscalationService` runs hourly, escalating unacknowledged
  CERT_EXPIRED/APPROVAL_NEEDED notifications through Manager -> Department Head
  -> Compliance Officer -> Director (the last two both resolve to COMPANY_ADMIN -
  no dedicated roles exist for those titles).
- **Phase 8 — AI Chat Assistant + AI Compliance Score** (complete):
  `ChatConversation`/`ChatMessage`. Retrieval is a full structured snapshot of
  the company's certificate data (JSON, via `ComplianceContextRetriever`) handed
  to the model alongside the question - not per-question intent parsing into
  specific queries, and not embeddings/RAG. `ComplianceScoreService`: valid
  (APPROVED, non-expired) / total ratio per category group + overall average;
  explicitly a valid/total ratio today, not valid/required - revisit once
  Missing Document Detection (Phase 9) makes "required" meaningful. AI chat calls
  are wrapped so a failed/unreachable OpenAI call degrades to a friendly
  in-conversation message (200, not a raw 500) while still persisting the
  exchange - the initial version let the exception propagate as an unhandled
  500, caught during manual verification.
- **Phase 9 — AI Risk Engine + Duplicate/Missing Document Detection**
  (complete): `RiskAssessment` + rule-based signals (expired certs, sliding-
  window overlapping-expiry clusters) with an AI narrative layer that never
  decides the score itself; `RiskAssessmentJob` sweeps all companies nightly.
  `DuplicateDetectionService`: exact certificate-number matches flagged with
  zero AI calls; near-matches (edit distance <=2) go to AI for confirmation
  only. `FacilityTypeRequiredCategory` (company-defined checklist, matched
  against `Plant.plantType`) + `MissingDocumentDetectionService` (pure
  relational set-difference, no AI) - dashboard's `missingDocuments` count now
  real. `RenewalPredictionService` wired to a certificate endpoint, with a
  historical-average-lead-time signal computed in Java feeding the AI call.
- **Phase 10 — Audit Mode + Reports**: `AuditFolder` + zip export (certs +
  generated reports + AI summary) via S3, `PdfReportGenerator` +
  openhtmltopdf/Thymeleaf templates for the 7 report types (Department, Expiry,
  Audit, Monthly, Vendor, Employee, Compliance Score), plus raw data/CSV export
  option. AUDITOR role verified read-only with time-bounded access throughout.

## Verification approach

Each phase ends with: `./gradlew build` passing, Flyway migrations applying
cleanly against a fresh Postgres, relevant Testcontainers integration tests
green, and a manual end-to-end check via the Swagger UI or curl against
docker-compose'd services for that phase's "done when" criteria above.

## Known pitfall: Spring Boot 4.1 ships both Jackson 2 and Jackson 3

Boot 4.1's auto-configured `ObjectMapper` bean is Jackson 3
(`tools.jackson.databind.ObjectMapper`), not classic Jackson 2
(`com.fasterxml.jackson.databind.ObjectMapper`) - injecting the classic type fails
context startup with "no bean of type ObjectMapper found" even though Jackson 2 is
still on the classpath transitively (AWS SDK v2 and most third-party libraries
still use it). Fixed by explicitly defining a classic-Jackson `ObjectMapper` bean
in `infrastructure/config/JacksonConfig.java` (registers `JavaTimeModule` for
`LocalDate`/`Instant` support). Always inject `com.fasterxml.jackson.databind.*`
types (not `tools.jackson.*`) in this codebase for consistency with AWS SDK/Spring
Security/etc., and rely on this explicit bean rather than assuming Boot provides
one.

## Known pitfall: `@Async` triggered from inside a `@Transactional` method races the commit

Confirmed via Phase 3 manual verification: calling an `@Async` method directly
from within a `@Transactional` service method starts the async worker thread
immediately, before the surrounding transaction commits - the worker can fail
with "not found" trying to read rows the outer transaction just inserted but
hasn't committed yet. Fixed in `CertificateService.upload` by registering a
`TransactionSynchronization` and firing the async call from `afterCommit()`
instead of inline. Apply this pattern anywhere else that triggers async/background
work from within a write transaction.

## Known pitfall: Hibernate `@Filter` does not cover `findById`

Confirmed via the Phase 1 tenant-isolation integration test: Spring Data's `findById`
(and any direct `EntityManager.find`) resolves through JPA's PK-lookup path, which
does **not** honor an enabled Hibernate `@Filter` - only derived/JPQL queries do.
Every tenant-scoped `getById`-style service method must call
`CompanyContextProvider.requireOwnership(entity, entityName, id)` after the fetch
(throws `EntityNotFoundException` on a cross-tenant match, same as not-found, so
callers can't distinguish the two). `PlantService.getById` is the reference
implementation. Apply this to every future by-id lookup: Certificate, Vendor,
Employee, etc.

## Local dev note: Testcontainers on Colima

This machine uses Colima (not Docker Desktop), whose socket lives at
`~/.colima/default/docker.sock` instead of `/var/run/docker.sock`, and its Ryuk
resource-reaper sidecar fails to start (tries to bind-mount a macOS host path
that doesn't exist inside the Colima Linux VM). Run Testcontainers-based tests
with:

```
DOCKER_HOST=unix:///Users/mac/.colima/default/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew test
```

Not baked into the repo/gradle config since it would break Docker Desktop or CI
environments where Ryuk works fine and the socket path differs.

## Phase 3 status: what's verified vs. pending real credentials

No OpenAI API key or local Tesseract install was available in the dev environment
this phase was built in. Verified live: PDFBox embedded-text extraction (exact
text pulled from a real PDF), the full async pipeline plumbing (transaction-commit
timing, status transitions PENDING -> EXTRACTING -> COMPLETED/FAILED, OCR text
persisted independently of the AI step so it survives an AI-step failure), the
category-matching logic (unit-level, matches by exact name against the company's
categories), and a real OpenAI HTTP call correctly failing with 401 (proving the
request path/auth header wiring is correct, not just that it's unreachable). NOT
verified: an actual successful OpenAI extraction response (need a real API key),
and Tesseract/Tess4J's OCR path for scanned PDFs and images (need Tesseract
installed - see infrastructure/ocr/TesseractImageExtractor). Both are pure
credential/environment gaps, not code gaps - supply `OPENAI_API_KEY` and install
Tesseract locally (or in the Docker image) to close them out.

## Known pitfall: escalation must advance the ORIGINAL notification's level

Found live in Phase 7 manual verification: `EscalationService.escalateOverdue()`
initially only dispatched a new `ESCALATION`-triggered notification without
updating the original (overdue) notification's `escalationLevel`. Since the
original notification's `sentAt` never changes, every subsequent sweep saw it as
still-overdue and re-escalated it to the *same* next tier again, indefinitely
(confirmed via a fast test cron: 3 sweeps produced 3 separate `DEPARTMENT_HEAD`
escalations of the same root notification instead of progressing
Manager -> Dept Head -> Compliance Officer -> Director). Fixed by advancing the
original notification's `escalationLevel` after each successful escalation, so
the next sweep computes the next tier and eventually terminates at DIRECTOR.
Any future "recurring reminder" feature that re-triggers off an unchanged
`sentAt`/status field should watch for this same unbounded-repeat shape.

## Known pitfall: synchronous AI calls must degrade gracefully, not abort the transaction

Recurring bug shape across Phase 8/9: several services call `AiService` methods
synchronously inside a `@Transactional` method that also computes/persists real,
valuable, non-AI data (chat message history in `ChatAssistantService`, rule-based
risk signals in `RiskAssessmentService`). Without a try/catch around the AI call,
an OpenAI failure (as in this dev environment, no API key) throws, the whole
transaction rolls back, and the deterministic work computed earlier in the same
method is lost too - not just the AI part. Fixed in `ChatAssistantService`,
`RiskAssessmentService`, `ComplianceScoreService.explainScore`, and
`RenewalPredictionService` by wrapping each `AiService` call in try/catch and
substituting a clear fallback message/value on failure, so the rest of the
method's work still commits. Apply this pattern to every future `AiService` call
site that shares a transaction with non-AI persistence - the async pipeline in
`CertificateExtractionService` (Phase 3) already does this correctly via its
outer try/catch in `processAsync`, which is the reference implementation.

## Known pitfall: fixed calendar buckets are wrong for "dates near each other"

`RiskAssessmentService`'s overlapping-expiry signal originally grouped
certificates by a fixed 14-day calendar bucket (`epochDay - epochDay % 14`).
Caught live: two certificates expiring on 2026-08-26 and 2026-08-27 - one day
apart - landed in *different* buckets (epoch-day 20678 vs 20692) because they
happened to straddle a bucket boundary, so the "2+ certs expiring close
together" signal silently failed to fire. Fixed by sorting certificates by
expiry date and scanning for genuine sliding-window clusters (each cert within
`OVERLAP_WINDOW_DAYS` of the previous one in sorted order) instead of bucketing
by a fixed calendar grid. Any future "things near each other in time" grouping
should use this sort-and-scan approach, not modulo-based fixed buckets.

## Known pitfall: temporary AUDITOR access wasn't enforced mid-session

`User.isAccessExpired()` was only checked at login and refresh - a temporary
AUDITOR whose `accessExpiresAt` passed while their access token was still valid
(up to the 15-minute TTL) could keep calling the API until the token's own
expiry, not the account's. Fixed in `JwtTokenProvider.generateAccessToken` by
capping the JWT's own `expiration` claim at `min(normalTtl, accessExpiresAt)`
for temporary users, so the token self-expires at the account's access boundary
without any per-request DB lookup. `AuthService.issueTokenPair` passes
`user.getAccessExpiresAt()` as the hard limit only when `user.isTemporary()`.

## Known pitfall: Thymeleaf standard dialect needs OGNL added explicitly, at the exact version Thymeleaf expects

`spring-boot-starter-thymeleaf` did not pull `ognl:ognl` in transitively on this
Boot 4.1 dependency tree. Thymeleaf's standard dialect (`th:each`, `th:text`,
etc. - used by every report template) needs OGNL as its default expression
language engine; without it, every report endpoint failed at runtime with
`NoClassDefFoundError: ognl/ClassResolver`, caught during Phase 10 manual
verification (all 7 report types 500'd identically). First fix attempt added
`ognl:ognl:3.4.7` (latest), which resolved the missing-class error but then
failed differently: `NoSuchMethodError: OgnlContext.<init>(...)` - OGNL 3.4.x
changed that constructor's signature, and Thymeleaf 3.1.5's OGNL adapter still
calls the old one. `thymeleaf-parent-3.1.5.RELEASE.pom` pins
`ognl.version=3.3.4`; pinning the same version in `build.gradle` fixed it.
Lesson: when a Spring Boot starter is missing a transitive dependency, check the
actual library's own managed version (its parent/BOM) rather than reaching for
"latest" - a newer major/minor can have a breaking API change the library in
question doesn't yet call correctly.

## Open risks (revisit if hit)

OpenAI model tier per use case (cost vs quality) not yet chosen — default to a
cheaper model for classification/summarization, stronger model for extraction/
chat, tune later. Refresh-token revocation is DB-based for now (no Redis) —
acceptable unless immediate-revocation requirements surface. AI-answer grounding
assumes structured retrieval is sufficient; semantic search over free-text
document content (e.g. "what does Fire NOC renewal require" from actual document
text) would need embeddings later if requested.
