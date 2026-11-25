# Spice Route

Restaurant feedback SaaS using Next.js, Spring Boot, Spring Data JPA, Java 21 and PostgreSQL 16. This is a refactored release candidate, not a certified production deployment.

## Local development

Requirements: Java 21, Maven 3.9+, Node.js 22, Docker Desktop and Docker Compose.

From the project root:

```sh
docker compose up -d
```

Configure backend variables in the terminal that starts Maven. The local defaults match `compose.yaml`. For AI summaries, export a valid Gemini API key using a hidden prompt in zsh:

```sh
read -rs 'AI_API_KEY?Gemini API key: '
export AI_API_KEY
echo
export AI_MODEL=gemini-3.5-flash-lite
export AI_FALLBACK_MODEL=gemini-3.5-flash
mvn -f backend/pom.xml spring-boot:run
```

In a second terminal:

```sh
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

Open http://localhost:3000. The frontend forwards `/api/spice/*` to `BACKEND_URL`; this keeps browser sessions same-origin. Sign in again after upgrading. The old `NEXT_PUBLIC_API_URL` setting is no longer used. Backend health: http://localhost:8080/actuator/health.

To seed a separate demo workspace after the backend starts, run `python3 scripts/seed.py` from the root. The script uses explicitly requested bearer sessions. Keep printed demo credentials private.

## Upgrade an existing local installation

1. Back up your database before changing code: `docker compose exec -T postgres pg_dump -U spice -d spice > ../spice-backup.sql`.
2. Stop the old frontend and backend, keep PostgreSQL and its existing volume.
3. Use the new source with the same Compose project name (`docker compose -p spice-route up -d` if needed) and database URL. Confirm the database is the intended one before booting the backend.
4. Flyway V1 is byte-for-byte preserved. V2 adds indexes and V3 adds rate-limit buckets. Do not edit V1, run `flyway clean`, or remove Docker volumes.
5. Reinstall frontend dependencies, restart backend and sign in again. Verify the workflows in `docs/verification.md`.

## Design and architecture

The backend uses domain entities, JPA repositories, feature services, request controllers and Spring Security. Application queries are JPQL or repository methods, not raw SQL. Schema changes are SQL migrations managed by Flyway. Hibernate validates the schema and does not create/drop tables. QR generation is separated from the HTTP controller. AI generation is split into provider, redaction, validation and persistence.

Browser authentication uses hashed, revocable, 12-hour opaque sessions with HttpOnly SameSite=Lax cookies. Trusted API clients can request bearer tokens using `X-Session-Mode: bearer`. Protected APIs deny access by default and enforce roles and branch ownership. Rate limits are stored in PostgreSQL with row locks. Rate-limit network buckets use the direct peer address, not untrusted forwarding headers; behind a reverse proxy users can share a bucket, so configure an appropriately trusted edge rate limiter before public traffic.

AI outputs reference actual comment indices. Counts and examples are calculated server-side; malformed/empty reports are rejected. The primary model and optional fallback are configurable. The semantic accuracy of the analysis still requires review, and regex/literal redaction does not guarantee that arbitrary free text contains no personal information. Do not send sensitive customer comments to an AI provider without an appropriate privacy policy and consent/contractual basis. Model availability, quota, and privacy terms are provider-dependent.

## Deployment

See `docs/deployment.md`. The backend and frontend have Dockerfiles; use a managed PostgreSQL instance such as Neon if appropriate. Set secrets through your host's secret manager, never in Git. Use HTTPS and `SECURE_COOKIES=true` in production. Configure `PUBLIC_URL` and `ALLOWED_ORIGINS` to the exact frontend origin. `BACKEND_URL` is a frontend server/build variable, not a client-side secret. Build with the real backend origin. The Next.js proxy must support the maximum AI request duration; select hosting accordingly.

## Verification

```sh
mvn -f backend/pom.xml clean verify
cd frontend
npm ci
npm run typecheck
npm run format:check
npm run build
```

GitHub Actions runs backend tests and the frontend type check/build. The PostgreSQL Testcontainers test needs Docker. See `docs/verification.md` for actual verification results and remaining release blockers. This revision was not compiled or integration-tested against PostgreSQL in its creation environment.

## Refinement release

See [Refinement release and acceptance checks](docs/refinement-release.md) before upgrading an existing database.

## Cloud Deployment

- Database: Neon PostgreSQL (Singapore)
- Backend: Spring Boot hosted on Railway
- Frontend: Next.js
- Database migrations: Flyway
- AI provider: Google Gemini
- Sensitive credentials are managed through environment variables.
