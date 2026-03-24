# Quickstart: Tenant Program Configuration

**Feature**: 002-program-configuration | **Date**: 2026-03-20

## Prerequisites

- Docker Desktop running (PostgreSQL + LocalStack containers)
- Java 21 installed
- Node.js 18+ installed
- `docker/docker-compose.yml` services started

## 1. Start Infrastructure

```bash
cd docker/
docker compose up -d
```

Verify services are healthy:
```bash
docker compose ps
# klasio-postgres: healthy
# klasio-localstack: healthy
```

## 2. Run Backend

```bash
cd api/

# Run Flyway migrations + start Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on `http://localhost:8080`. Flyway applies migrations V001–V005 automatically.

Verify:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## 3. Generate Dev Tokens

Use the existing dev token endpoint to generate JWT tokens for testing:

```bash
# Admin token (tenant-scoped)
curl -X POST http://localhost:8080/dev/token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantId": "<YOUR_TENANT_UUID>",
    "roles": ["ADMIN"]
  }'

# Superadmin token (platform-level, no tenantId)
curl -X POST http://localhost:8080/dev/token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440099",
    "roles": ["SUPERADMIN"]
  }'

# Manager token (tenant-scoped, read-only for programs)
curl -X POST http://localhost:8080/dev/token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "tenantId": "<YOUR_TENANT_UUID>",
    "roles": ["MANAGER"]
  }'
```

## 4. Test Program Endpoints

### Create a program (as ADMIN)

```bash
TOKEN="<admin_token>"

curl -X POST http://localhost:8080/api/v1/programs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kids Swimming",
    "modality": "HOURS_BASED",
    "cost": 120000,
    "managerId": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

### List programs

```bash
curl http://localhost:8080/api/v1/programs \
  -H "Authorization: Bearer $TOKEN"
```

### Get program detail

```bash
curl http://localhost:8080/api/v1/programs/<PROGRAM_UUID> \
  -H "Authorization: Bearer $TOKEN"
```

### Update a program

```bash
curl -X PUT http://localhost:8080/api/v1/programs/<PROGRAM_UUID> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Youth Swimming",
    "cost": 130000,
    "managerId": "550e8400-e29b-41d4-a716-446655440002"
  }'
```

### Deactivate a program

```bash
curl -X POST http://localhost:8080/api/v1/programs/<PROGRAM_UUID>/deactivate \
  -H "Authorization: Bearer $TOKEN"
```

### Reactivate a program

```bash
curl -X POST http://localhost:8080/api/v1/programs/<PROGRAM_UUID>/reactivate \
  -H "Authorization: Bearer $TOKEN"
```

### Superadmin accessing tenant programs

```bash
SA_TOKEN="<superadmin_token>"

curl http://localhost:8080/api/v1/programs \
  -H "Authorization: Bearer $SA_TOKEN" \
  -H "X-Tenant-Id: <TENANT_UUID>"
```

## 5. Run Tests

### Backend unit tests

```bash
cd api/
./mvnw test
```

### Backend integration tests (requires Docker for TestContainers)

```bash
cd api/
./mvnw verify
```

### Frontend tests

```bash
cd web/
npm test
```

## 6. Run Frontend

```bash
cd web/
npm run dev
```

Frontend starts on `http://localhost:3000`. Navigate to `/programs` to manage programs.

## 7. Verify RLS Tenant Isolation

Connect to PostgreSQL and verify RLS is enforced:

```bash
docker exec -it klasio-postgres psql -U postgres -d klasio
```

```sql
-- Without tenant context: should return 0 rows (RLS blocks)
SELECT * FROM programs;

-- With tenant context: returns only that tenant's programs
SET LOCAL app.current_tenant = '<TENANT_UUID>';
SELECT * FROM programs;
```

## API Documentation

Swagger UI: http://localhost:8080/swagger-ui.html

OpenAPI spec: `api/src/main/resources/static/program-api.yaml`
