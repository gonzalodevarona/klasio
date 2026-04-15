# API Contracts: Payment Proof Upload and Validation (008)

**Branch**: `008-payment-proof-validation` | **Phase**: 1 | **Date**: 2026-03-31

All endpoints are tenant-scoped via JWT claims. Base path: `/api/v1`.

---

## RF-19: Student Proof Upload Endpoints

### POST /memberships/{membershipId}/payment-proof
**Roles**: STUDENT (own membership only), ADMIN, SUPERADMIN
**Content-Type**: `multipart/form-data`

Upload a payment proof file. If a previous non-superseded proof exists for this membership, it is automatically superseded.

**Path params**:
- `membershipId` (UUID) — the membership being paid for

**Form fields**:
- `file` (binary) — the proof file; max 5 MB; PDF, JPG, or PNG

**Preconditions** (400/422 if violated):
- Membership must be in `PENDING_PAYMENT_VALIDATION` status
- Membership must belong to the authenticated student's tenant
- File size ≤ 5,242,880 bytes
- MIME type (Tika-validated) must be `application/pdf`, `image/jpeg`, or `image/png`
- STUDENT role: membership must belong to the authenticated student's own account

**Response 201 Created**:
```json
{
  "proofId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "membershipId": "1a2b3c4d-...",
  "status": "PENDING",
  "originalFileName": "recibo-marzo.pdf",
  "contentType": "application/pdf",
  "fileSizeBytes": 204800,
  "uploadedAt": "2026-03-31T14:22:00Z"
}
```

**Error responses**:

| Status | Code | When |
|--------|------|------|
| 400 | `FILE_TOO_LARGE` | file > 5 MB |
| 400 | `UNSUPPORTED_FILE_TYPE` | MIME type not in allowed set |
| 400 | `INVALID_MEMBERSHIP_STATUS` | membership not in PENDING_PAYMENT_VALIDATION |
| 403 | `ACCESS_DENIED` | student tries to upload for another student's membership |
| 404 | `MEMBERSHIP_NOT_FOUND` | membership not found in this tenant |

---

### GET /memberships/{membershipId}/payment-proof
**Roles**: STUDENT (own), ADMIN, SUPERADMIN

Returns the current (non-superseded) proof for a membership.

**Response 200 OK**:
```json
{
  "proofId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "membershipId": "1a2b3c4d-...",
  "status": "REJECTED",
  "originalFileName": "recibo-marzo.pdf",
  "contentType": "application/pdf",
  "fileSizeBytes": 204800,
  "uploadedAt": "2026-03-31T14:22:00Z",
  "validatedAt": "2026-03-31T15:30:00Z",
  "rejectionReason": "El documento está cortado y no muestra el monto total pagado."
}
```

**Error responses**:

| Status | Code | When |
|--------|------|------|
| 404 | `PROOF_NOT_FOUND` | no active proof for this membership |
| 403 | `ACCESS_DENIED` | student requests another student's proof |

---

## RF-20: Admin Proof Validation Endpoints

### GET /payment-proofs
**Roles**: ADMIN, SUPERADMIN
**Query params**:
- `page` (int, default 0)
- `size` (int, default 20, max 100)

Returns all `PENDING` proofs for the authenticated tenant, ordered by `uploadedAt` ascending (oldest first).

**Response 200 OK**:
```json
{
  "content": [
    {
      "proofId": "3fa85f64-...",
      "membershipId": "1a2b3c4d-...",
      "studentId": "9f8e7d6c-...",
      "studentName": "Ana García",
      "programName": "Youth & Adults",
      "uploadedAt": "2026-03-31T14:22:00Z",
      "contentType": "application/pdf",
      "originalFileName": "recibo.pdf"
    }
  ],
  "totalElements": 14,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

### GET /payment-proofs/{proofId}/download-url
**Roles**: ADMIN, SUPERADMIN, MANAGER (own program scope)

Generates a 15-minute pre-signed S3 GET URL for viewing the proof document.

**Response 200 OK**:
```json
{
  "downloadUrl": "https://s3.amazonaws.com/klasio-bucket/proofs/...?X-Amz-Expires=900&...",
  "expiresAt": "2026-03-31T15:45:00Z",
  "contentType": "application/pdf"
}
```

**Error responses**:

| Status | Code | When |
|--------|------|------|
| 404 | `PROOF_NOT_FOUND` | proofId not found in tenant |
| 403 | `ACCESS_DENIED` | manager requests proof outside their program scope |

---

### POST /payment-proofs/{proofId}/approve
**Roles**: ADMIN, SUPERADMIN

Approves a proof and triggers membership activation.

**Request body**:
```json
{
  "activateDirectly": true
}
```

- `activateDirectly: true` → membership transitions to `ACTIVE`
- `activateDirectly: false` → membership transitions to `PENDING_MANAGER_ACTIVATION`; program manager receives notification

**Response 200 OK**:
```json
{
  "proofId": "3fa85f64-...",
  "status": "APPROVED",
  "validatedAt": "2026-03-31T15:30:00Z",
  "membershipStatus": "ACTIVE"
}
```

**Error responses**:

| Status | Code | When |
|--------|------|------|
| 409 | `PROOF_NOT_PENDING` | proof is not in PENDING status |
| 404 | `PROOF_NOT_FOUND` | proofId not found in tenant |

---

### POST /payment-proofs/{proofId}/reject
**Roles**: ADMIN, SUPERADMIN

Rejects a proof with a mandatory rejection reason.

**Request body**:
```json
{
  "rejectionReason": "El documento no muestra el monto total ni la fecha del pago."
}
```

Constraints: `rejectionReason` is required and must be between 10 and 500 characters.

**Response 200 OK**:
```json
{
  "proofId": "3fa85f64-...",
  "status": "REJECTED",
  "rejectionReason": "El documento no muestra el monto total ni la fecha del pago.",
  "validatedAt": "2026-03-31T15:30:00Z",
  "membershipStatus": "PENDING_PAYMENT_VALIDATION"
}
```

**Error responses**:

| Status | Code | When |
|--------|------|------|
| 400 | `REJECTION_REASON_REQUIRED` | rejectionReason is blank or missing |
| 400 | `REJECTION_REASON_TOO_SHORT` | rejectionReason < 10 characters |
| 409 | `PROOF_NOT_PENDING` | proof is not in PENDING status |

---

## RF-21: Manager Authorization Endpoints

### GET /memberships?status=PENDING_MANAGER_ACTIVATION
**Roles**: MANAGER (existing endpoint, existing filter)

Returns delegated memberships awaiting manager activation. The existing `GET /memberships` endpoint already supports status filtering — no new endpoint needed. The manager's program scope is enforced by the existing `ListMembershipsService`.

> **Note**: This reuses the existing endpoint. No new endpoint is required for RF-21 listing.

---

### PATCH /memberships/{membershipId}/activate
**Roles**: MANAGER, ADMIN, SUPERADMIN (existing endpoint)

Activates a delegated membership. Already implemented in `006-membership-lifecycle`. This endpoint is reused as-is for RF-21 — no changes needed.

---

## Error Envelope (Platform Standard)

All error responses use this format:

```json
{
  "error": {
    "code": "FILE_TOO_LARGE",
    "message": "The uploaded file exceeds the 5 MB limit. Received: 6,291,456 bytes."
  }
}
```

---

## OpenAPI Tags

New endpoints belong to the `payment-proofs` tag:

```yaml
tags:
  - name: payment-proofs
    description: Payment proof upload, review, and admin validation queue
```

The existing `memberships` tag covers:
- `POST /memberships/{id}/payment-proof` (new)
- `GET /memberships/{id}/payment-proof` (new)
- `PATCH /memberships/{id}/activate` (existing, unchanged for RF-21)
