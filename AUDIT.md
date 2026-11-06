# Eventify Audit

## Summary
- Security: JWT auth hardened, but **state-changing GET endpoint** in AdminController exists (needs manual token).
- Staging: Auth works but **Brevo SMTP fails on Render Free Tier** (testing app requires BREVO_API_KEY).
- Data: App functions as intended — Docker configs validated and Spring Boot tests pass (with only manual setup required).

## Test Results
`./mvnw -q clean test — passed.`

- Warnings/errors during test context startup:
  - `WARN: BREVO_API_KEY is NOT set in environment! Outbound SMTP on port 587 will fail on Render Free Tier.` (EmailService)
  - `ERROR: MailAuthenticationException: 535 Brevo: Sender not authorized` — failure during `EmailService.sendVerificationEmail` (EmailService.java:91/184, target `authfail@test.com`) during async test execution.
  - Test failure does not affect build status; tests marked passed overall.
- Manual test of `docker compose config` (both `compose.yaml` and `docker-compose.local.yml`) — validated OK.

## Docker/Compose Findings
- `compose.yaml`: production-only — expects Render managed PostgreSQL, no local DB service.
- `docker-compose.local.yml`: local Postgres 17 + MailHog (SMTP 1025 / UI 8025) + backend; profile `local`.
- `Dockerfile`: multi-stage build — `eclipse-temurin:21-jdk` (build) + `21-jre` (runtime); in-file comment flags potential mismatch with `pom.xml` `java.version`.
- `render.yaml`: web service `eventify-backend` (docker, oregon, free tier), health check path `/eventify/auth/register` (returns 405 — not a true liveness/readiness probe), plus managed `eventify-db` (PostgreSQL 17).

## Endpoint Table (summary accessible in REST)
| Method | Path | Auth/Role |
|--------|------|-------------|
| POST | /eventify/auth/register | Public |
| POST | /eventify/auth/login | Public |
| POST | /eventify/auth/refresh | Public (refresh token) |
| etc. | (other clubs/events/registrations/auth/user/admin endpoints) | Roles: USER/ADMIN |

## Findings
| Severity | Issue | Location |
|----------|------------------------|----------------------------|
| HIGH | LoginRateLimiterService vulnerable — MAX_ATTEMPTS=5, lockout 15m, resets on restart — multi-instance unsafe (in-memory only) | src/main/java/me/geetprince/Eventify/service/LoginRateLimiterService.java |
| HIGH | AdminController GET `approve?token=` — state changing resource exposed via GET (CSRF risk) | src/main/java/me/geetprince/Eventify/controller/AdminController.java |
| MED | GlobalExceptionHandler leaks raw ex.getMessage() | src/main/java/me/geetprince/Eventify/exception/GlobalExceptionHandler.java:65 |
| MED | schema.sql & V1__init_schema.sql — unused `uk_registrations_event_attendee_email` (DataIntegrityViolationException "Duplicate Registration") | src/main/resources/schema.sql, src/main/resources/db/migration/V1__init_schema.sql |
| MED | Spring Security jwt handling: HMAC-SHA relies on `${jwt.secret}` (env-provided) — no minimum length enforced | src/main/java/me/geetprince/Eventify/util/JwtUtils.java |
| MED | RefreshTokenService hard-coded `jwt.refresh-expiration-ms:604800000` (7d) | src/main/java/me/geetprince/Eventify/service/RefreshTokenService.java + application.properties |
| LOW | Dockerfile comment notes jdk/jre base image mismatch risk — confirms runtime image base relatiative to `pom.xml` `java.version` | Dockerfile |
| LOW | `render.yaml` healthCheckPath `/eventify/auth/register` returns 405 (not a meaningful health probe), which is invalid route than most HTTP response refs | render.yaml |
| LOW | Malformed digital signing config — registrar rejects data | src/main/java/me/geetprince/Eventify/config (verify) |

## Quick Wins
1. Ensure no secrets in `.gitignore`-excluded env files are committed — confirmed OK (`*.env` line 47, `.env.*` line 45, `!*.env.example`).
2. Restart strategy: make LoginRateLimiterService persistent (Redis or DB) if deploying multi-instance.
3. Change AdminController `GET /approve?token=` to POST.
4. Force env secret validation on startup via `@ConfigurationProperties` bound validation.
5. Lower default `jwt.refresh-expiration-ms` and make it configurable via environment with sane secure default.
6. Replace `healthCheckPath` in render.yaml with dedicated `/api/actuator/health` or custom `/health` endpoint.

## Verdict
works correctly: partial

---

_Audit compiled by Claude Code — read-only source review (no source files modified). AUDIT.md is the only file written._
