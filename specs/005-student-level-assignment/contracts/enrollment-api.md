# API Contracts: Student Level Assignment

**Feature**: 005-student-level-assignment
**Base path**: `/api/v1`
**Date**: 2026-03-26

All endpoints are tenant-scoped (tenant resolved from JWT). Pagination follows the existing Spring Data Page format: `{ content, totalPages, totalElements, number, size }`.

---

## Student Endpoints (Minimal CRUD)

### POST /students

Create a new student within the authenticated tenant.

**Authorization**: ADMIN, SUPERADMIN

**Request**:
```json
{
  "firstName": "Carlos",
  "lastName": "Martinez",
  "email": "carlos@example.com"
}
```

**Validation**:
- `firstName`: required, max 100 chars
- `lastName`: required, max 100 chars
- `email`: required, valid format, unique per tenant, max 255 chars

**Response 201**:
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "firstName": "Carlos",
  "lastName": "Martinez",
  "email": "carlos@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-03-26T10:00:00Z",
  "createdBy": "uuid"
}
```

**Errors**:
- 400 `VALIDATION_ERROR` — missing/invalid fields
- 409 `STUDENT_EMAIL_ALREADY_EXISTS` — email already registered in tenant

---

### GET /students

List students in the authenticated tenant, with pagination and optional filters.

**Authorization**: ADMIN, SUPERADMIN, MANAGER

**Query parameters**:
- `page` (default 0)
- `size` (default 20)
- `status` (optional): ACTIVE, INACTIVE
- `search` (optional): searches first_name, last_name, email (case-insensitive partial match)

**Response 200**:
```json
{
  "content": [
    {
      "id": "uuid",
      "firstName": "Carlos",
      "lastName": "Martinez",
      "email": "carlos@example.com",
      "status": "ACTIVE",
      "createdAt": "2026-03-26T10:00:00Z"
    }
  ],
  "totalPages": 5,
  "totalElements": 98,
  "number": 0,
  "size": 20
}
```

---

### GET /students/{studentId}

Get a student's details including their active enrollments.

**Authorization**: ADMIN, SUPERADMIN, MANAGER

**Response 200**:
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "firstName": "Carlos",
  "lastName": "Martinez",
  "email": "carlos@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-03-26T10:00:00Z",
  "createdBy": "uuid",
  "updatedAt": null,
  "updatedBy": null,
  "enrollments": [
    {
      "id": "uuid",
      "programId": "uuid",
      "programName": "Kids Program",
      "level": "BEGINNER",
      "enrollmentDate": "2026-03-26",
      "status": "ACTIVE"
    }
  ]
}
```

**Errors**:
- 404 `STUDENT_NOT_FOUND`

---

### PUT /students/{studentId}

Update a student's basic information.

**Authorization**: ADMIN, SUPERADMIN

**Request**:
```json
{
  "firstName": "Carlos",
  "lastName": "Martinez Garcia",
  "email": "carlos.new@example.com"
}
```

**Response 200**: Same shape as GET /students/{studentId} (without enrollments).

**Errors**:
- 400 `VALIDATION_ERROR`
- 404 `STUDENT_NOT_FOUND`
- 409 `STUDENT_EMAIL_ALREADY_EXISTS`

---

### POST /students/{studentId}/deactivate

Deactivate a student.

**Authorization**: ADMIN, SUPERADMIN

**Response 200**: Updated student detail.

**Errors**:
- 404 `STUDENT_NOT_FOUND`
- 409 `STUDENT_ALREADY_INACTIVE`

---

### POST /students/{studentId}/reactivate

Reactivate a previously deactivated student.

**Authorization**: ADMIN, SUPERADMIN

**Response 200**: Updated student detail.

**Errors**:
- 404 `STUDENT_NOT_FOUND`
- 409 `STUDENT_ALREADY_ACTIVE`

---

## Enrollment Endpoints

### POST /programs/{programId}/enrollments

Enroll a student in a program with an initial level.

**Authorization**: ADMIN, SUPERADMIN, MANAGER (manager scoped to their program)

**Request**:
```json
{
  "studentId": "uuid",
  "level": "BEGINNER"
}
```

**Validation**:
- `studentId`: required, must reference an existing active student in the same tenant
- `level`: required, one of BEGINNER, INTERMEDIATE, ADVANCED

**Business rules**:
- Student must not already have an active enrollment in this program
- Program must exist and be active
- Creates an initial level history entry (previousLevel = null)

**Response 201**:
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "studentId": "uuid",
  "studentName": "Carlos Martinez",
  "programId": "uuid",
  "programName": "Kids Program",
  "level": "BEGINNER",
  "enrollmentDate": "2026-03-26",
  "status": "ACTIVE",
  "createdAt": "2026-03-26T10:00:00Z",
  "createdBy": "uuid"
}
```

**Errors**:
- 400 `VALIDATION_ERROR` — missing/invalid fields
- 404 `STUDENT_NOT_FOUND` — student doesn't exist or inactive
- 404 `PROGRAM_NOT_FOUND` — program doesn't exist or inactive
- 409 `ENROLLMENT_ALREADY_EXISTS` — student already enrolled in this program

---

### GET /programs/{programId}/enrollments

List enrollments for a specific program.

**Authorization**: ADMIN, SUPERADMIN, MANAGER (manager scoped to their program)

**Query parameters**:
- `page` (default 0)
- `size` (default 20)
- `level` (optional): BEGINNER, INTERMEDIATE, ADVANCED
- `status` (optional): ACTIVE, INACTIVE

**Response 200**:
```json
{
  "content": [
    {
      "id": "uuid",
      "studentId": "uuid",
      "studentName": "Carlos Martinez",
      "level": "BEGINNER",
      "enrollmentDate": "2026-03-26",
      "status": "ACTIVE"
    }
  ],
  "totalPages": 3,
  "totalElements": 45,
  "number": 0,
  "size": 20
}
```

---

### GET /students/{studentId}/enrollments

List all enrollments for a specific student across programs.

**Authorization**: ADMIN, SUPERADMIN, MANAGER

**Response 200**:
```json
{
  "content": [
    {
      "id": "uuid",
      "programId": "uuid",
      "programName": "Kids Program",
      "level": "BEGINNER",
      "enrollmentDate": "2026-03-26",
      "status": "ACTIVE"
    }
  ],
  "totalPages": 1,
  "totalElements": 2,
  "number": 0,
  "size": 20
}
```

---

### GET /enrollments/{enrollmentId}

Get enrollment detail including level history.

**Authorization**: ADMIN, SUPERADMIN, MANAGER

**Response 200**:
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "studentId": "uuid",
  "studentName": "Carlos Martinez",
  "programId": "uuid",
  "programName": "Kids Program",
  "level": "INTERMEDIATE",
  "enrollmentDate": "2026-03-26",
  "status": "ACTIVE",
  "createdAt": "2026-03-26T10:00:00Z",
  "createdBy": "uuid",
  "updatedAt": "2026-03-28T14:30:00Z",
  "updatedBy": "uuid"
}
```

**Errors**:
- 404 `ENROLLMENT_NOT_FOUND`

---

### GET /enrollments/{enrollmentId}/level-history

Get the chronological level change history for an enrollment.

**Authorization**: ADMIN, SUPERADMIN, MANAGER

**Response 200**:
```json
{
  "content": [
    {
      "id": "uuid",
      "previousLevel": null,
      "newLevel": "BEGINNER",
      "changedBy": "uuid",
      "changedByRole": "ADMIN",
      "changedAt": "2026-03-26T10:00:00Z",
      "justification": null
    },
    {
      "id": "uuid",
      "previousLevel": "BEGINNER",
      "newLevel": "INTERMEDIATE",
      "changedBy": "uuid",
      "changedByRole": "MANAGER",
      "changedAt": "2026-03-28T14:30:00Z",
      "justification": "Student demonstrated proficiency in all beginner drills"
    }
  ],
  "totalPages": 1,
  "totalElements": 2,
  "number": 0,
  "size": 20
}
```

---

### POST /enrollments/{enrollmentId}/deactivate

Deactivate an enrollment (remove student from program).

**Authorization**: ADMIN, SUPERADMIN, MANAGER

**Response 200**: Updated enrollment detail.

**Errors**:
- 404 `ENROLLMENT_NOT_FOUND`
- 409 `ENROLLMENT_ALREADY_INACTIVE`

---

## Error Envelope

All errors follow the existing format:

```json
{
  "error": {
    "code": "ENROLLMENT_ALREADY_EXISTS",
    "message": "Student is already enrolled in this program",
    "details": null
  }
}
```

For validation errors:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "level", "message": "Level is required" },
      { "field": "studentId", "message": "Student ID is required" }
    ]
  }
}
```
