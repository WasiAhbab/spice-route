# Deployment checklist

## Database

Create a PostgreSQL database with a supported provider. Use a unique strong password, encrypted transport, restricted network access and backups. Set `DATABASE_URL=jdbc:postgresql://HOST/DATABASE?sslmode=require`, `DATABASE_USER` and `DATABASE_PASSWORD`. Run migrations against a staging clone first. Separate migration and runtime credentials before launch; the current local configuration uses one database user for both. Plan database restore and rollback before deploying V2/V3.

## Spring Boot

Use Java 21 and the backend Dockerfile. Set `PUBLIC_URL`, comma-separated `ALLOWED_ORIGINS`, `SECURE_COOKIES=true`, database credentials and optionally `AI_API_KEY`, `AI_MODEL`, `AI_FALLBACK_MODEL`. `ADMIN_EMAIL` and `ADMIN_PASSWORD` may bootstrap an administrator; remove those variables after account setup. They do not change existing passwords. Health check `/actuator/health` is unauthenticated and exposes no details.

## Next.js

Set `BACKEND_URL` to the backend HTTPS origin (no `/api` suffix), then build and deploy the frontend with a Node.js runtime, not static export. Its rewrite forwards same-origin browser requests to the backend. Configure TLS, proxy timeouts, request-size limits and trusted edge rate limits. Never expose the backend's AI key to Next.js. Scan actual QR codes on an external phone after deployment.

## Release gates

Use the verification checklist, run dependency vulnerability scans, assess CSP and browser security headers, validate authorization with distinct tenants, review privacy/retention obligations and verify production resource limits. Test backup/restore and rollback. No real deployment or independent security audit has been completed for this revision.
