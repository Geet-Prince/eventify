# Local Docker Testing Guide

> Branch: `localtest` — mirrors `main` but wired for local containers.

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRODUCTION  (Render + Vercel)                 │
│                                                                   │
│  ┌──────────────┐     HTTPS      ┌──────────────────────────┐   │
│  │   Frontend   │ ─────────────► │    Backend (Docker)      │   │
│  │   (Vercel)   │                │  eventify-backend-bcb0   │   │
│  │  eventify.   │                │  .onrender.com : 8080    │   │
│  │ geetprince.me│                └────────────┬─────────────┘   │
│  └──────────────┘                             │ Render Internal  │
│                                               │ Network (JDBC)   │
│                                    ┌──────────▼──────────────┐   │
│                                    │  PostgreSQL 17 (Managed) │  │
│                                    │  Render Database Service  │  │
│                                    │  eventify-db              │  │
│                                    └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    LOCAL  (Docker Compose)                        │
│                                                                   │
│  ┌──────────────┐    localhost    ┌──────────────────────────┐   │
│  │  Frontend    │ ─────────────► │  eventify-backend        │   │
│  │  (Vite/npm)  │   :3000/:5173  │  localhost:8080           │   │
│  └──────────────┘                └────────────┬─────────────┘   │
│                                               │ Docker Network   │
│                             ┌─────────────────┼──────────────┐   │
│                             │                 │              │   │
│                   ┌─────────▼────┐  ┌─────────▼──────────┐  │   │
│                   │ postgres-db   │  │  mailhog            │  │   │
│                   │ :5432         │  │  SMTP :1025         │  │   │
│                   │ (Docker)      │  │  UI   :8025         │  │   │
│                   └──────────────┘  └────────────────────┘  │   │
│                                                               │   │
└─────────────────────────────────────────────────────────────────┘
```

| Component | Platform | Config File |
|---|---|---|
| **Frontend** | Vercel | _(Vercel dashboard)_ |
| **Backend** | Render Web Service (Docker) | `Dockerfile` · `compose.yaml` · `render.yaml` |
| **PostgreSQL** | Render Managed Database | `render.yaml` → `eventify-db` |
| **Email (SMTP)** | Brevo relay | Render env vars |

---

## Local Stack

| Service | Container | Port(s) | Purpose |
|---|---|---|---|
| PostgreSQL 17 | `eventify-postgres` | `5432` | Local database |
| MailHog | `eventify-mailhog` | `1025` (SMTP) · `8025` (UI) | Fake inbox — no real emails sent |
| Spring Boot | `eventify-backend` | `8080` | REST API |

## Quick Start

```bash
# 1. Switch to localtest branch
git checkout localtest

# 2. Start all containers  (first run builds the JAR — ~3 min)
docker compose -f docker-compose.local.yml --env-file .env.local up --build

# 3. Verify the API is healthy
curl http://localhost:8080/eventify/auth/register   # 405/400 = alive, not 500

# 4. Open MailHog inbox (see all sent emails)
open http://localhost:8025
```

## Environment Variables (`.env.local`)

| Variable | Default | Notes |
|---|---|---|
| `POSTGRES_USER` | `eventify_user` | Local DB user |
| `POSTGRES_PASSWORD` | `eventify_local_pass` | Local DB password |
| `JWT_SECRET` | `local-dev-jwt-secret-…` | Must be ≥ 32 chars |
| `ADMIN_EMAIL` | `admin@localhost` | Approval emails go here (visible in MailHog) |
| `SENDER_EMAIL` | `noreply@localhost` | From: address |
| `BREVO_API_KEY` | _(empty)_ | Optional locally — MailHog needs no auth; only set to test real Brevo delivery |

> ⚠️ `.env.local` and `.env` are in `.gitignore` — never commit real secrets.

## Testing the Registration + Email Flow

```bash
# Register a club
curl -s -X POST http://localhost:8080/eventify/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Club",
    "email": "test@example.com",
    "password": "Password1!"
  }' | jq .

# Expected: HTTP 201 Created
# → Verification email appears in MailHog UI: http://localhost:8025
```

### Step-by-step Verification

1. **Register** → `POST /eventify/auth/register` → HTTP 201
2. **Open MailHog** at `http://localhost:8025` → find "Verify your email" message
3. **Click the verify link** in the email → `http://localhost:8080/eventify/auth/verify?token=…`
4. **Admin approval email** appears in MailHog (sent to `ADMIN_EMAIL`)
5. **Approve** → click "Approve Club" in that email (or hit the approve URL directly)
6. **Login** → `POST /eventify/auth/login` → HTTP 200 + JWT

## Running Unit Tests (No Docker Needed)

```bash
# Runs all 14 tests using H2 in-memory DB, MockitoBean for SMTP
./mvnw test

# Run only the root cause regression suite
./mvnw test -Dtest=RegistrationRootCauseRegressionTest
```

## Running Without Docker (Host JVM)

```bash
# Start only infrastructure
docker compose -f docker-compose.local.yml --env-file .env.local up postgres-db mailhog

# Run the app with the local profile (connects to localhost:5432 + localhost:1025)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

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

# Rebuild only the backend after a code change
docker compose -f docker-compose.local.yml up --build eventify-backend
```

## Production vs Local: Key Differences

| Aspect | Production (Render + Vercel) | Local (`localtest` branch) |
|---|---|---|
| **Database** | Render Managed PostgreSQL (external) | Docker `postgres:17` container |
| **DB connection** | Render internal network URL | `localhost:5432` |
| **SMTP** | Brevo (real, authenticated) | MailHog (fake, no auth needed) |
| **DDL policy** | `validate` (safe) | `update` (auto-sync schema) |
| **CORS origins** | Vercel + custom domain only | `localhost:3000`, `:5173`, `:4173` |
| **Spring profile** | _(default)_ | `local` |
| **Config file** | `compose.yaml` / `render.yaml` | `docker-compose.local.yml` |
| **Env secrets** | Render dashboard env vars | `.env.local` file |

## Render Deployment Reference

### Environment Variables to Set in Render Dashboard

| Variable | Where to Get It |
|---|---|
| `SPRING_DATASOURCE_URL` | Render PostgreSQL → "Internal Database URL" (convert `postgres://` → `jdbc:postgresql://`) |
| `SPRING_DATASOURCE_USERNAME` | Render PostgreSQL → Credentials |
| `SPRING_DATASOURCE_PASSWORD` | Render PostgreSQL → Credentials |
| `JWT_SECRET` | Generate: `openssl rand -hex 32` |
| `SPRING_MAIL_USERNAME` | Brevo dashboard → SMTP credentials |
| `SPRING_MAIL_PASSWORD` | Brevo dashboard → SMTP credentials |
| `ADMIN_EMAIL` | `prince.raj.ds@gmail.com` |
| `SENDER_EMAIL` | `prince.raj.ds@gmail.com` |

> **Important:** Render's PostgreSQL connection string uses `postgres://` scheme.
> Spring Boot requires `jdbc:postgresql://`.
> Replace the prefix when pasting into `SPRING_DATASOURCE_URL`.

### Using `render.yaml` (Blueprint)

If using the `render.yaml` blueprint, Render auto-provisions the DB and injects
credentials — no manual copy-paste needed. Push `render.yaml` to your repo and
create a new Blueprint in the Render dashboard.
