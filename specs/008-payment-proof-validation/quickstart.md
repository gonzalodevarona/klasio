# Quickstart: Payment Proof Upload and Validation (008)

**Branch**: `008-payment-proof-validation` | **Date**: 2026-03-31

This guide covers what you need to run the new payment proof feature locally on top of the existing Klasio dev environment.

---

## Prerequisites

The existing dev environment from `007-auth-rbac` must already be running:
- Docker Desktop with `docker compose up -d` (PostgreSQL on `localhost:5432`, LocalStack on `localhost:4566`)
- Backend: IntelliJ IDEA running `spring-boot:run` on port `8080`
- Frontend: Cursor running `npm run dev` on port `3000`

---

## New Environment Variables

### Backend (`api/src/main/resources/application-local.yml`)

No new variables required. The payment proof feature reuses the existing S3 configuration:

```yaml
klasio:
  s3:
    endpoint: http://localhost:4566   # LocalStack
    region: us-east-1
    bucket: klasio-local
```

The `S3PaymentProofStorage` adapter uses the same `S3Properties` configuration bean. The bucket already exists from the `006-membership-lifecycle` setup. File keys are in a new prefix (`proofs/`), so no bucket changes are needed.

---

## Database Migrations

Three new Flyway migrations run automatically on startup:

| Migration | Table | Purpose |
|---|---|---|
| V028 | `payment_proofs` | Proof records with status, file key, MIME type, size |
| V029 | `delegation_reminders` | 48h reminder tracking per delegation |
| V030 | `audit_log` constraint | Adds 5 new action types to the existing CHECK constraint |

To verify migrations ran:
```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('028', '029', '030')
ORDER BY version;
```

---

## Seeding Test Data

The new `DataSeeder` (or use the existing test data) requires:
1. A student user with role `STUDENT`
2. A membership in `PENDING_PAYMENT_VALIDATION` status

Use the existing seeder script or create via the API:
```bash
# Create a membership (as admin)
curl -X POST http://localhost:8080/api/v1/memberships \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "<student-uuid>",
    "programId": "<program-uuid>",
    "planId": "<plan-uuid>"
  }'
```

---

## Upload a Proof (Local Test)

```bash
# As student — upload a test PDF
curl -X POST "http://localhost:8080/api/v1/memberships/<membershipId>/payment-proof" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -F "file=@/path/to/test-recibo.pdf;type=application/pdf"
```

Expected: `201 Created` with `{ "status": "PENDING", ... }`

---

## Verify File in LocalStack S3

```bash
# List proofs in LocalStack bucket
aws --endpoint-url=http://localhost:4566 s3 ls s3://klasio-local/proofs/ --recursive
```

---

## Admin Proof Queue

```bash
# List pending proofs (as admin)
curl http://localhost:8080/api/v1/payment-proofs \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get download URL
curl http://localhost:8080/api/v1/payment-proofs/<proofId>/download-url \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Approve proof (direct activation)
curl -X POST http://localhost:8080/api/v1/payment-proofs/<proofId>/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "activateDirectly": true }'

# Reject proof
curl -X POST http://localhost:8080/api/v1/payment-proofs/<proofId>/reject \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "rejectionReason": "El comprobante no muestra el monto pagado." }'
```

---

## Notification Stubs

Email and in-app notifications are stubs (pending RF-32). When a proof is uploaded or validated, you will see log output like:

```
INFO  PaymentProofNotificationListener - [STUB] Notifying admin of new proof upload: proofId=xxx membershipId=yyy
INFO  PaymentProofNotificationListener - [STUB] Notifying student of proof rejection: studentId=zzz reason="..."
```

---

## Running Tests

```bash
# Backend — all tests
./mvnw test

# Backend — only payment proof tests
./mvnw test -pl api -Dtest="PaymentProof*,UploadPaymentProof*,ApproveProof*,RejectProof*"

# Frontend
cd web && npm test -- --testPathPattern="payment-proof|proof-queue"
```

---

## Disabling the 48h Reminder Scheduler (Local)

The `DelegationReminderJob` runs hourly. To prevent log noise during local development, disable it in `application-local.yml`:

```yaml
klasio:
  scheduler:
    delegation-reminder:
      enabled: false
```

The scheduler checks `@ConditionalOnProperty(name = "klasio.scheduler.delegation-reminder.enabled", havingValue = "true", matchIfMissing = true)`.
