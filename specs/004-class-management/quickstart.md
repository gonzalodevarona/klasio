# Quickstart: Class and Schedule Management

**Feature**: 004-class-management
**Date**: 2026-03-25

## Prerequisites

- Docker Desktop running (PostgreSQL container)
- Backend: `cd api && mvn spring-boot:run`
- Frontend: `cd web && npm run dev`
- A tenant, program, and dev token available (see DevTokenController)
- At least one professor created (for assignment testing)

## Manual Testing Flow

### 1. Create a Recurring Class

```bash
curl -X POST http://localhost:8080/api/v1/programs/<program-id>/classes \
  -H "Authorization: Bearer <dev-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Monday Beginners",
    "level": "BEGINNER",
    "type": "RECURRING",
    "scheduleEntries": [
      { "dayOfWeek": "MONDAY", "startTime": "16:00", "endTime": "17:30" },
      { "dayOfWeek": "WEDNESDAY", "startTime": "16:00", "endTime": "17:30" }
    ],
    "maxStudents": 15
  }'
```

Expected: `201 Created` with class detail including `status: "ACTIVE"`, `type: "RECURRING"`, and two schedule entries.

### 2. Create a One-Time Class

```bash
curl -X POST http://localhost:8080/api/v1/programs/<program-id>/classes \
  -H "Authorization: Bearer <dev-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Guest Workshop",
    "level": "ADVANCED",
    "type": "ONE_TIME",
    "scheduleEntries": [
      { "specificDate": "2026-04-15", "startTime": "10:00", "endTime": "12:00" }
    ],
    "maxStudents": 20
  }'
```

Expected: `201 Created` with `type: "ONE_TIME"` and one schedule entry with `specificDate`.

### 3. List Classes for a Program

```bash
curl http://localhost:8080/api/v1/programs/<program-id>/classes \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with paginated list containing both classes.

### 4. Filter by Level

```bash
curl "http://localhost:8080/api/v1/programs/<program-id>/classes?level=BEGINNER" \
  -H "Authorization: Bearer <dev-token>"
```

Expected: Only beginner classes returned.

### 5. Filter by Status

```bash
curl "http://localhost:8080/api/v1/programs/<program-id>/classes?status=ACTIVE" \
  -H "Authorization: Bearer <dev-token>"
```

### 6. Get Class Detail

```bash
curl http://localhost:8080/api/v1/programs/<program-id>/classes/<class-id> \
  -H "Authorization: Bearer <dev-token>"
```

### 7. Update Class

```bash
curl -X PUT http://localhost:8080/api/v1/programs/<program-id>/classes/<class-id> \
  -H "Authorization: Bearer <dev-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mon-Wed Beginners",
    "level": "BEGINNER",
    "scheduleEntries": [
      { "dayOfWeek": "MONDAY", "startTime": "17:00", "endTime": "18:30" },
      { "dayOfWeek": "WEDNESDAY", "startTime": "17:00", "endTime": "18:30" }
    ],
    "maxStudents": 20
  }'
```

### 8. Assign a Professor

```bash
curl -X PUT http://localhost:8080/api/v1/programs/<program-id>/classes/<class-id>/professor \
  -H "Authorization: Bearer <dev-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "professorId": "<professor-id>"
  }'
```

Expected: `200 OK` with class detail showing `professorId` and `professorName` populated.

### 9. Remove Professor

```bash
curl -X DELETE http://localhost:8080/api/v1/programs/<program-id>/classes/<class-id>/professor \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with `professorId: null` and `professorName: null`.

### 10. Deactivate Class

```bash
curl -X POST http://localhost:8080/api/v1/programs/<program-id>/classes/<class-id>/deactivate \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with `status: "INACTIVE"`.

### 11. Reactivate Class

```bash
curl -X POST http://localhost:8080/api/v1/programs/<program-id>/classes/<class-id>/reactivate \
  -H "Authorization: Bearer <dev-token>"
```

Expected: `200 OK` with `status: "ACTIVE"`.

### 12. Frontend Testing

1. Navigate to `http://localhost:3000/programs/<program-id>/classes`
2. Click "Add Class" → fill form (name, level, type, schedule, max students) → submit
3. Verify class appears in list with schedule summary and level badge
4. Click class name → view detail page with full schedule and professor info
5. Click "Edit" → modify schedule and max students → save
6. Click "Assign Professor" → select from dropdown → confirm
7. Click "Remove Professor" → confirm → verify shows "Unassigned"
8. Click "Deactivate" → confirm → verify status badge changes
9. Click "Reactivate" → verify status returns to Active
10. Test filters: select level "Beginner" and status "Active" from dropdowns

## Running Tests

```bash
# Backend unit + integration tests
cd api && mvn test

# Frontend tests
cd web && npm test
```
