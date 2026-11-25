# Verification and release status

## Checks performed during preparation

- Source archive was inspected and the original Flyway V1 migration was preserved byte for byte.
- Frontend TypeScript type check passed using the original archive's installed dependencies.
- Frontend Prettier check passed after refactoring.
- Next.js production build was attempted but failed while fetching the Linux SWC binary: the uploaded dependencies are macOS-specific and package registry DNS is unavailable in this environment.
- Java syntax parser check is included in the release log. This is not a Maven compile or a JPA startup check.
- Maven, Docker, PostgreSQL runtime verification, provider generation and browser E2E tests were not available in the preparation environment. The added tests and CI must run on a real development/CI machine before release.

## Manual regression checklist

- Register two owners; confirm each sees only their own restaurant and branches. Try the other owner's IDs in management, analytics and QR requests; expect 403.
- Create a branch manager. Confirm the assigned branch works and another branch returns 403. Check owner-only manager creation and admin-only operations.
- Verify cookie flags in browser devtools, expired/revoked session behavior, wrong-origin mutations, unauthenticated endpoints (401), sign-in rate limits and logout.
- Create a restaurant, branch and tables. Download individual PNG and bulk PDF; scan from a phone. Disable a table and confirm its public form is unavailable.
- Submit anonymous and named feedback in English, Bangla and Banglish. Confirm 1–5 bounds, invalid input rejection, spam limits, and rating/date/table filters. Cross-check dashboard totals.
- Generate AI with fewer than five meaningful comments (rejected), exactly five, and multiple languages. Check unique valid evidence IDs, cache reuse, fallback behavior and rejection of malformed output. Confirm redaction and test without a key or provider availability.
- Check mobile keyboard navigation, error states, refresh, responsive charts, empty lists, and overlapping actions. Verify production build and repeat from a clean database.
- Back up and restore a staging database, then migrate an existing V1 database and confirm retained data.

## Remaining release blockers and limitations

The full Maven compilation, Hibernate query validation and integration tests have not run here. The frontend build must run with platform-correct dependencies. Public release additionally requires deployment-specific security review, dependency audit, load and concurrency tests, browser accessibility tests and production monitoring.

Feedback and directory listings remain unpaginated; branch performance is explicitly all-time. AI generation is synchronous with bounded in-process concurrency; distributed job handling and full asynchronous workflows are not implemented. AI classification is not guaranteed factual even when references are validated. Privacy redaction is best effort, and no automatic retention/anonymization policy or account recovery flow has been implemented. Existing UI exposes table activation and platform suspension; restaurant/branch editing and manager assignment endpoints have no corresponding UI yet. Feedback resolution, CSV export and report history are not part of this revision. Do not market this candidate as a fully tested production release.
