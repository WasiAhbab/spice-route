# Refinement v1 — implementation and acceptance

## Scope

This update is based on the prior release-candidate source archive and incorporates the locally reported QR controller correction, 32 KB request header limit, five AI requests per user per minute, Gemini primary/fallback configuration, extended timeouts and privacy-safe failure logging. It was not fetched from the latest GitHub branch. Review the diff before merging. No real database, API key or Git metadata is bundled.

## Implemented

- Owner directory: restaurant/branch renaming, address editing, search, archive and restore. Archive is deliberately reversible, not permanent deletion. Archived restaurants and branches are removed from operational selectors and cannot accept feedback. Previously disabled tables remain disabled after restore. Platform suspension is independent of owner archival.
- Table management: rename without changing QR, explicit enable/disable, QR regeneration with a warning that printed links must be replaced, existing PNG/PDF functionality, search and empty states.
- Manager management: display-name editing, branch assignment/removal (last assignment must be retained; suspend instead), owner suspension/reactivation independent of platform suspension, session revocation when suspended. Manager ownership checks cover all existing assignments. Manager mutations acquire a database lock on the manager record.
- Account settings: display-name change and password change with current password verification, rate limiting, and revocation of all sessions. Email change, recovery, 2FA and session inventory are not implemented.
- Feedback review: four statuses, internal notes, version-based conflict detection, search, client-side pagination, and CSV export of the current filtered set (excludes contact details and internal notes, escapes spreadsheet formula prefixes). The original rating/comment is unchanged. This is a single editable note, not a full immutable audit trail.
- Guest customization: per-restaurant welcome and thank-you messages, optional contact field toggle (enforced by backend), translated defaults, guest link unavailable state and duplicate-click prevention. Custom messages are shared across languages. No logo upload or custom question builder yet.
- Shared native modal dialogs with keyboard/escape handling, in-dialog errors, progress indicator, responsive management rows and notices. No screenshots or visual browser verification were performed.
- AI: primary and fallback models are configurable, logs omit raw feedback and secrets, truncation is detected. Synchronous requests remain; no background queue, usage budget, automatic recurring reports, or complete retry policy yet. Upstream provider availability is not guaranteed.

## Migration safety

V1–V3 are unchanged. V4 adds archival and manager suspension flags, V5 adds feedback review fields/version, V6 adds feedback customization. The migrations are additive and preserve existing IDs, users, feedback, reports and table tokens. Back up PostgreSQL and test on a staging copy before deployment. Do not run `docker compose down -v`. Flyway applies migrations on backend startup. Once migrations have been applied, rolling back application code does not roll back the schema; use a planned database restore for destructive rollback, not manual edits to migration history.

## Verification performed in the preparation environment

- TypeScript `tsc --noEmit`: passed using dependencies extracted from the previous source archive.
- Prettier `--check src next.config.mjs`: passed.
- Java 21 syntax parsing using javac parser: passed for 48 Java source/test files. This is syntax parsing only, not compilation or dependency validation.
- Next.js production build: attempted, blocked by unavailable Linux SWC dependency and DNS/network access. The archived dependencies were macOS ARM64-specific. They are not bundled in this update.
- Maven compilation/test execution: not performed (Maven/dependency cache unavailable). PostgreSQL/Testcontainers tests not executed. Integration test added for cross-tenant rename rejection, archival/restoration and QR rotation.
- No real browser, mobile, accessibility, load, deployment or external AI report test was performed. GitHub was unreachable from this environment. These checks must be completed on the actual project before production use.

## Local upgrade procedure

1. Ensure `git status` is clean; create a refinement branch. Stop frontend/backend processes, but keep PostgreSQL running. Back up the existing database with `pg_dump`.
2. Extract the update-only ZIP into the existing project root. It contains changed/new files only, not `.git`, secrets, dependencies or build output. Review `git diff` and `git status`.
3. Run `mvn -f backend/pom.xml clean verify` with Java 21. Check whether Testcontainers tests are skipped. Fix any failure before startup.
4. Run `mvn -f backend/pom.xml spring-boot:run` using a private `AI_API_KEY` and optionally `AI_MODEL` / `AI_FALLBACK_MODEL`. Verify Flyway V4–V6 and JPA startup. From `frontend`, run `npm ci`, `npm run typecheck`, `npm run format:check`, `npm run build`, then `npm run dev`.
5. Test the workflows below with separate owner/manager accounts. Commit only after verification; keep the main branch stable until review.

## Acceptance checklist (not yet verified end-to-end)

- [ ] Existing account signs in; existing restaurant, branch, table, feedback and AI report records remain.
- [ ] Rename restaurant and branch; QR remains valid and uses new names.
- [ ] Archive branch and restaurant; guest QR stops working; restore and verify historical data and the same QR.
- [ ] Rename a table; disable and enable; regenerate QR and verify old token is invalid and new token works.
- [ ] Assign manager to a second branch; remove first assignment; reject removal of final assignment; suspend/reactivate and verify sessions and platform suspension behavior.
- [ ] Attempt cross-tenant edit, archive, assignment, review and QR changes; reject all.
- [ ] Edit profile; change password; verify prior sessions no longer work.
- [ ] Review feedback, save note/status, verify original content unchanged and stale version conflict; export filtered CSV.
- [ ] Customize guest text; toggle contact fields off and verify even a direct POST cannot save contact details.
- [ ] Test malformed requests, invalid input, duplicate names, no data, provider overload, network interruptions, keyboard interaction and mobile layout.
- [ ] Verify CI and production builds; confirm backup restore on a separate database.

## Remaining commercial production gates

Server-side pagination/aggregation and load tests, immutable audit events, email verification and recovery, secure session inventory and optional MFA, persistent AI jobs and scheduled reports, full complaint assignment/history, upload/storage for logos and QR templates, real notifications, billing and support, public form abuse/idempotency controls beyond double-click protection, dependency security audit, observability, tested backup restoration, HTTPS/staging deployment and a real-client pilot. No production-ready guarantee is made.
