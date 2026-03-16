# Quickstart: Tenant Management Local Development

**Feature**: 001-tenant-management
**Date**: 2026-03-15

---

## Prerequisites

- Java 21 (JDK)
- Node.js (latest LTS) + npm/pnpm
- Docker & Docker Compose
- AWS CLI configured (for S3 integration tests; LocalStack for local dev)

## 1. Start PostgreSQL

```bash
cd docker/
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with:
- Database: `klasio`
- App user: `klasio_app` / `klasio_pass`
- Admin user: `postgres` / `postgres`

## 2. Start the Backend (API)

```bash
cd api/
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The API starts on `http://localhost:8080`. Flyway migrations run automatically on startup.

Verify:
```bash
curl http://localhost:8080/actuator/health
```

## 3. Start the Frontend (Web)

```bash
cd web/
npm install
npm run dev
```

The frontend starts on `http://localhost:3000`.

## 4. Run Backend Tests

```bash
cd api/

# Unit tests only (fast)
./mvnw test

# Integration tests (requires Docker — Testcontainers)
./mvnw verify -Pfailsafe
```

## 5. Run Frontend Tests

```bash
cd web/
npm test
```

## 6. API Quick Test

```bash
# Create a tenant (requires a valid superadmin JWT — use a test token for local dev)
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer <SUPERADMIN_JWT>" \
  -F "name=Fútbol Bogotá" \
  -F "sportDiscipline=Fútbol" \
  -F "contactEmail=info@futbolbogota.co"

# List tenants
curl http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer <SUPERADMIN_JWT>"

# Get tenant detail
curl http://localhost:8080/api/v1/tenants/futbol-bogota \
  -H "Authorization: Bearer <SUPERADMIN_JWT>"

# Deactivate tenant
curl -X POST http://localhost:8080/api/v1/tenants/futbol-bogota/deactivate \
  -H "Authorization: Bearer <SUPERADMIN_JWT>"
```

## Environment Variables

| Variable | Default (local) | Description |
|----------|----------------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/klasio` | PostgreSQL connection |
| `DATABASE_USER` | `klasio_app` | DB application user |
| `DATABASE_PASSWORD` | `klasio_pass` | DB password |
| `JWT_SECRET` | (dev key in application-local.yml) | JWT signing key |
| `AWS_S3_BUCKET` | `klasio-logos-dev` | S3 bucket for logos |
| `AWS_REGION` | `us-east-1` | AWS region |
| `AWS_ACCESS_KEY_ID` | (local dev via LocalStack) | AWS credentials |
| `AWS_SECRET_ACCESS_KEY` | (local dev via LocalStack) | AWS credentials |

## Docker Compose Services (local dev)

| Service | Port | Purpose |
|---------|------|---------|
| `postgres` | 5432 | PostgreSQL database |
| `localstack` | 4566 | S3-compatible storage (local dev) |
