# Refactoring summary

- Replaced JdbcTemplate application queries with Spring Data JPA and JPQL repositories.
- Introduced entities, feedback/report DTOs, access-control and authentication components; moved QR/PDF work to a service.
- Added HttpOnly cookie sessions, exact-origin checks, default-deny route authorization, persistent rate limits and expiry cleanup.
- Isolated Gemini provider requests, redaction, report validation, cache versioning and optional fallback; replaced globally synchronized generation with bounded concurrency.
- Split dashboard feature panels, moved AI styling into a separate feature stylesheet and formatted TypeScript.
- Added environment examples, Dockerfiles, health endpoint, CI workflow and tests; preserved V1 migrations.

Suggested Git commits (create only after reviewing and verifying the actual corresponding changes):

- `refactor(backend): move database access into JPA repositories`
- `feat(auth): enforce role-based access and cookie sessions`
- `feat(security): persist request limits and expire old sessions`
- `refactor(ai): validate evidence and isolate provider requests`
- `refactor(ui): split dashboard feature panels`
- `chore(ci): add verification workflow and deployment configuration`

No Git commits or remote pushes were performed for this delivery. Do not rewrite authorship or invent a history. Document assistance according to the team's policy.
