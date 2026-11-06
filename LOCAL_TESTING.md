# Local Docker Testing Guide

> Branch: `localtest` — identical to `main` but wired for local containers.

## Stack

| Service | Container | Port(s) | Purpose |
|---|---|---|---|
| PostgreSQL 17 | `eventify-postgres` | `5432` | Database |
| MailHog | `eventify-mailhog` | `1025` (SMTP) · `8025` (UI) | Fake inbox — no real emails sent |
| Spring Boot | `eventify-backend` | `8080` | REST API |

## Quick Start

```bash
# 1. Clone / switch to this branch
git checkout localtest

# 2. Start all containers  (first run will build the JAR — ~3 min)
docker compose -f docker-compose.local.yml --env-file .env.local up --build

# 3. Verify the API is healthy
curl http://localhost:8080/eventify/auth/register   # should give 405/400, not 500

# 4. Open the MailHog web inbox
open http://localhost:8025
```

## Environment Variables (`.env.local`)

| Variable | Default (in .env.local) | Notes |
|---|---|---|
| `POSTGRES_USER` | `eventify_user` | DB user |
| `POSTGRES_PASSWORD` | `eventify_local_pass` | DB password |
| `JWT_SECRET` | `local-dev-jwt-secret-…` | Must be ≥ 32 chars |
| `ADMIN_EMAIL` | `admin@localhost` | Approval emails go here (visible in MailHog) |
| `SENDER_EMAIL` | `noreply@localhost` | From: address |

> ⚠️ `.env.local` is in `.gitignore` — never commit real secrets.

## Testing the Registration + Email Flow

```bash
# Register a club
curl -s -X POST http://localhost:8080/eventify/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Club",
    "email": "test@example.com",
    "password": "Password123!"
  }' | jq .

# Expected: HTTP 201 Created
# → Verification email appears in MailHog UI: http://localhost:8025
```

### Step-by-step Verification

1. **Register** → `POST /eventify/auth/register` → HTTP 201
2. **Open MailHog** at `http://localhost:8025` → find "Verify your email" message
3. **Click the verify link** in the email (points to `http://localhost:8080/eventify/auth/verify?token=…`)
4. **Admin approval email** appears in MailHog (sent to `ADMIN_EMAIL`)
5. **Approve** → click "Approve Club" in that email body (or `GET /eventify/admin/clubs/{id}/approve?token=…`)
6. **Login** → `POST /eventify/auth/login` → HTTP 200 + JWT

## Running Without Docker (Host JVM)

If you want to run the Spring Boot app directly on your machine (with only Postgres + MailHog in Docker):

```bash
# Start only the infrastructure
docker compose -f docker-compose.local.yml --env-file .env.local up postgres-db mailhog

# Run the app with the local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` Spring profile (`application-local.properties`) sets:
- DB → `localhost:5432`
- SMTP → `localhost:1025` (MailHog)
- URLs → `http://localhost:8080` / `http://localhost:3000`

## Useful Docker Commands

```bash
# Stop everything (keep volumes)
docker compose -f docker-compose.local.yml down

# Stop + wipe database (clean slate)
docker compose -f docker-compose.local.yml down -v

# View backend logs live
docker logs -f eventify-backend

# View all logs together
docker compose -f docker-compose.local.yml logs -f

# Rebuild only the backend (after code change)
docker compose -f docker-compose.local.yml up --build eventify-backend
```

## Differences From `main` (Production)

| Aspect | `main` (Render) | `localtest` |
|---|---|---|
| SMTP | Brevo (real, authenticated) | MailHog (fake, no auth) |
| DB host | Render managed Postgres | Local Docker container |
| Java image | `eclipse-temurin:21` | Same |
| `SPRING_PROFILES_ACTIVE` | _(default)_ | `local` |
| Env secrets | Render dashboard env vars | `.env.local` file |
