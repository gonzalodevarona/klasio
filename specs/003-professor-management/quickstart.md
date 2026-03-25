# Quickstart: Professor Management

**Feature**: 003-professor-management
**Date**: 2026-03-23

## Prerequisites

- Docker Desktop running (PostgreSQL container)
- Backend: `cd api && mvn spring-boot:run`
- Frontend: `cd web && npm run dev`
- A tenant and dev token available (see DevTokenController)

## Manual Testing Flow

### 1. Create a Professor

```bash
curl -X POST http://localhost:8080/api/v1/professors \
  -H "Authorization: Bearer <dev-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Carlos",
    "lastName": "Martinez",
    "email": "carlos.martinez@example.com"
  }'
```

Expected: `201 Created` with professor detail including `status: "INVITED"` and `invitationToken`.

### 2. List Professors

```bash
curl http://localhost:8080/api/v1/professors \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with paginated list containing the created professor.

### 3. Filter by Status

```bash
curl "http://localhost:8080/api/v1/professors?status=INVITED" \
  -H "Authorization: Bearer <dev-token>"
```

### 4. Get Professor Detail

```bash
curl http://localhost:8080/api/v1/professors/<professor-id> \
  -H "Authorization: Bearer <dev-token>"
```

### 5. Update Professor

```bash
curl -X PUT http://localhost:8080/api/v1/professors/<professor-id> \
  -H "Authorization: Bearer <dev-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Carlos Alberto",
    "lastName": "Martinez",
    "email": "carlos.martinez@example.com"
  }'
```

### 6. Deactivate Professor

```bash
curl -X POST http://localhost:8080/api/v1/professors/<professor-id>/deactivate \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with `status: "DEACTIVATED"`.

### 7. Reactivate Professor

```bash
curl -X POST http://localhost:8080/api/v1/professors/<professor-id>/reactivate \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with `status: "ACTIVE"`.

### 8. Frontend Testing

1. Navigate to `http://localhost:3000/professors`
2. Click "Add Professor" → fill form → submit
3. Verify professor appears in list
4. Click professor name → view detail page
5. Click "Edit" → modify name → save
6. Click "Deactivate" → confirm → verify status badge changes
7. Click "Reactivate" → verify status returns to Active

## Running Tests

```bash
# Backend unit + integration tests
cd api && mvn test

# Frontend tests
cd web && npm test
```
