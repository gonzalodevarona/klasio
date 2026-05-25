# Drop-In Students — Data Model + Registration Flow + UI Design

**Date:** 2026-05-09
**Status:** Approved (data model + backend + UI)
**Owner:** Gonzalo de Varona
**Scope of this spec:**
- §1–§9: Data model and Flyway migration (V069).
- §11–§19: Backend registration flow (REST endpoints, application service, domain layer, RBAC, audit, idempotency, edge cases).
- §20: Backend test plan (migration + domain + application + integration + controller).
- §21–§28: Frontend UI — receptionist drop-in registration modal on the class-session attendance screen (Next.js 15, next-intl).
- §29: Frontend test plan (Jest + React Testing Library).

---

## 1. Goal

Introduce a second commercial model alongside hour-based memberships: **drop-in attendees** — people who pay per class, in person, without a Klasio account. Admins register them on arrival. They may be one-time visitors or recurring. If a recurring drop-in later converts to a full student, the price of their last drop-in (within the previous 10 days) is discounted from the new membership.

This spec covers only the persistent storage shape needed to support the feature. Use cases, services, controllers, and UI are out of scope.

## 2. Non-goals

- No domain events (deferred to the implementation spec).
- No discount-application logic (the data exposes the lookup; the membership creation flow consumes it later).
- No drop-in cancellation flow — by business rule drop-ins cannot be cancelled (paid upfront, service guaranteed).
- No refund flow — drop-in classes are not cancelled by policy (drop-ins arrive minutes before class start).
- No bulk drop-in registration.
- No conversion flow implementation. The schema supports it; the service is a separate spec.

## 3. Terminology — Drop-In vs Walk-In

These are **distinct concepts**, easy to confuse:

| Concept | Definition | Has account? | Has membership? | Pays |
|---|---|---|---|---|
| **Walk-in** (RF-34, already shipped) | Existing student registered on the spot by staff | Yes | Yes (active) | Hours deducted from existing membership |
| **Drop-in** (this spec) | Non-student person who pays per class at the door | No | No | Per-class fee, upfront, cash or transfer |

The schema and language in this spec keep the two strictly separate. No code in this feature touches the walk-in path.

## 4. Data Model Overview

Three storage changes:

1. **New table `drop_in_attendees`** — the person.
2. **New table `drop_in_payments`** — the per-session fee.
3. **Modifications to `attendance_registrations`** — generalize the attendee dimension so a row can reference either a `student_id` or a `drop_in_attendee_id`.
4. **New column `programs.drop_in_price`** — per-program drop-in price (admin-editable, nullable to disable drop-ins).
5. **`audit_log.action_type` CHECK** — extended with new drop-in actions.

All tenant-scoped tables get RLS policies identical to the existing pattern (`tenant_id = current_setting('app.tenant_id')::uuid`).

## 5. Schema

### 5.1 `drop_in_attendees` (new)

```sql
CREATE TABLE drop_in_attendees (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID         NOT NULL REFERENCES tenants(id),
    full_name                   VARCHAR(200) NOT NULL,
    phone                       VARCHAR(20)  NOT NULL,
    total_visits                INTEGER      NOT NULL DEFAULT 0 CHECK (total_visits >= 0),
    first_visit_at              TIMESTAMPTZ,
    last_visit_at               TIMESTAMPTZ,
    converted_to_student_id     UUID         REFERENCES students(id),
    converted_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                  UUID         NOT NULL,
    updated_at                  TIMESTAMPTZ,
    updated_by                  UUID,

    CONSTRAINT uq_dropin_phone_per_tenant
        UNIQUE (tenant_id, phone),

    CONSTRAINT chk_dropin_conversion_pair
        CHECK ((converted_to_student_id IS NULL) = (converted_at IS NULL)),

    CONSTRAINT chk_dropin_visit_dates
        CHECK (first_visit_at IS NULL
            OR last_visit_at  IS NULL
            OR first_visit_at <= last_visit_at)
);

CREATE INDEX ix_dropin_tenant_phone
    ON drop_in_attendees(tenant_id, phone);

CREATE INDEX ix_dropin_converted_student
    ON drop_in_attendees(converted_to_student_id)
    WHERE converted_to_student_id IS NOT NULL;

ALTER TABLE drop_in_attendees ENABLE ROW LEVEL SECURITY;
CREATE POLICY drop_in_attendees_tenant_isolation ON drop_in_attendees
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
```

**Notes**
- **No `email` column.** Per spec, email is not part of the initial model. Add later if reporting needs it.
- **No `identity_document`.** Drop-ins are walk-up payers; identity is captured only at conversion to student.
- `phone` is `VARCHAR(20)` to match `students.phone` (V020) and `professors.phone_number` (V012). Country-code normalization follows the same convention as V062 (no `+57` prefix).
- `total_visits`, `first_visit_at`, `last_visit_at` are denormalized counters maintained on `REGISTERED → PRESENT` transition. Updated in the same transaction as the attendance state change.
- Conversion is a one-way link: when an attendee becomes a student, both `converted_to_student_id` and `converted_at` are set atomically. Row is never deleted (history preserved).

### 5.2 `drop_in_payments` (new)

```sql
CREATE TABLE drop_in_payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID         NOT NULL REFERENCES tenants(id),
    drop_in_attendee_id     UUID         NOT NULL REFERENCES drop_in_attendees(id),
    class_session_id        UUID         NOT NULL REFERENCES class_sessions(id),
    program_id              UUID         NOT NULL REFERENCES programs(id),
    amount                  DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    payment_method          VARCHAR(20)  NOT NULL
        CHECK (payment_method IN ('CASH', 'TRANSFER')),
    paid_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    registered_by           UUID         NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID         NOT NULL,

    CONSTRAINT uq_dropin_payment_per_session
        UNIQUE (drop_in_attendee_id, class_session_id)
);

CREATE INDEX ix_dropin_payment_attendee_paid_at
    ON drop_in_payments(drop_in_attendee_id, paid_at DESC);

CREATE INDEX ix_dropin_payment_session
    ON drop_in_payments(class_session_id);

ALTER TABLE drop_in_payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY drop_in_payments_tenant_isolation ON drop_in_payments
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
```

**Notes**
- `tenant_id` is denormalized from `drop_in_attendees.tenant_id` for clean RLS and direct queries (no join required to filter by tenant). The application layer must keep them consistent at insert time; a `BEFORE INSERT` trigger is not added (kept simple — invariant is application-enforced).
- `program_id` is denormalized from `class_session.class.program_id`. Avoids a two-hop join for "list all drop-in revenue per program" reports.
- `paid_at` is the canonical timestamp for the **discount window** (see §6). Default `NOW()` matches the upfront-payment business rule.
- `uq_dropin_payment_per_session` enforces "one drop-in per attendee per session" — same person can't pay twice for the same session.
- `payment_method` is a CHECK constraint, not an enum type. Consistent with how other modules model small closed sets (see `program.modality`).

### 5.3 `attendance_registrations` (modify)

The current schema (V046) requires `student_id`, `enrollment_id`, `membership_id`, `level_at_registration`, and `intended_hours` to be `NOT NULL`. None of these apply to drop-ins. The migration loosens these and adds two new FKs plus three CHECKs that preserve student-row invariants.

```sql
-- Loosen NOT NULLs (drop-in rows leave them empty)
ALTER TABLE attendance_registrations ALTER COLUMN student_id            DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN enrollment_id         DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN membership_id         DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN level_at_registration DROP NOT NULL;
ALTER TABLE attendance_registrations ALTER COLUMN intended_hours        DROP NOT NULL;

-- New FKs (drop-in path)
ALTER TABLE attendance_registrations
    ADD COLUMN drop_in_attendee_id UUID REFERENCES drop_in_attendees(id),
    ADD COLUMN drop_in_payment_id  UUID REFERENCES drop_in_payments(id);

-- Exactly one of (student_id, drop_in_attendee_id) is set.
ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_attendee_xor
    CHECK (
        (student_id IS NOT NULL AND drop_in_attendee_id IS NULL)
     OR (student_id IS NULL     AND drop_in_attendee_id IS NOT NULL)
    );

-- Student rows must keep their full set of references.
ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_student_full
    CHECK (
        student_id IS NULL
     OR (enrollment_id IS NOT NULL
         AND membership_id IS NOT NULL
         AND level_at_registration IS NOT NULL
         AND intended_hours IS NOT NULL)
    );

-- Drop-in rows must reference their payment.
ALTER TABLE attendance_registrations
    ADD CONSTRAINT chk_reg_dropin_payment
    CHECK (
        drop_in_attendee_id IS NULL
     OR drop_in_payment_id IS NOT NULL
    );

-- Replace the single partial unique index with two parallel ones.
DROP INDEX ux_registration_active_per_student_session;

CREATE UNIQUE INDEX ux_reg_active_student_session
    ON attendance_registrations(student_id, session_id)
    WHERE status = 'REGISTERED' AND student_id IS NOT NULL;

-- Drop-ins are created directly with status='PRESENT' (no REGISTERED stop, see §15.1).
-- Predicate matches the only reachable "active" status for drop-in rows.
CREATE UNIQUE INDEX ux_reg_active_dropin_session
    ON attendance_registrations(drop_in_attendee_id, session_id)
    WHERE drop_in_attendee_id IS NOT NULL AND status = 'PRESENT';
```

**Notes**
- The `status` enum is reused unchanged: `REGISTERED, CANCELLED_BY_STUDENT, CANCELLED_BY_SYSTEM, PRESENT, ABSENT`. Drop-in rows in practice flow only `REGISTERED → PRESENT`. `CANCELLED_BY_STUDENT` is unreachable for drop-ins (no student-side cancel action). `CANCELLED_BY_SYSTEM` remains reachable defensively (see §7.3).
- `level_at_registration` is `NULL` for drop-ins — they can attend any level class; staff judgment governs eligibility.
- `intended_hours` is `NULL` for drop-ins — they don't deduct hours.
- `class_id`, `session_id`, `session_date`, `session_start_time`, `session_end_time` remain `NOT NULL` for both paths (snapshot is the same).
- `current_capacity` on `class_sessions` is incremented for both student and drop-in registrations (one physical seat per registration).

### 5.4 `programs.drop_in_price` (new column)

```sql
ALTER TABLE programs
    ADD COLUMN drop_in_price DECIMAL(15,2);

ALTER TABLE programs
    ADD CONSTRAINT chk_program_drop_in_price_positive
    CHECK (drop_in_price IS NULL OR drop_in_price > 0);
```

**Notes**
- `NULL` means drop-ins disabled for the program. UI hides the "Register drop-in" button.
- Lives on `programs`, not `program_plans`. Plans are membership tiers; drop-in is a single per-program price orthogonal to plan choice. Putting it on plans would create the ambiguity "which plan's drop_in_price wins?" with no business answer.
- Editable by admin only (RBAC enforced in the future endpoint, out of scope here).

### 5.5 `audit_log.action_type` (extend)

The CHECK constraint (last touched in V045 + V046) gets four new values:

- `DROP_IN_ATTENDEE_REGISTERED` — new attendee created (first visit) or recognized (recurring) and registered for a session.
- `DROP_IN_PAYMENT_RECORDED` — payment row inserted.
- `DROP_IN_ATTENDANCE_MARKED` — attendance row transitioned to PRESENT for a drop-in.
- `DROP_IN_CONVERTED_TO_STUDENT` — drop-in linked to a newly created student.

Migration drops and recreates the constraint with the existing values plus these four. The pattern matches V045's approach.

## 6. Discount Mechanic (no schema state)

The 10-day discount is **computed**, not stored. At membership creation time, the new flow will:

```sql
SELECT amount, paid_at
  FROM drop_in_payments
 WHERE drop_in_attendee_id = :attendee_id_of_converted_student
   AND paid_at > NOW() - INTERVAL '10 days'
 ORDER BY paid_at DESC
 LIMIT 1;
```

If a row comes back, that `amount` is subtracted from the membership cost. No new column on `memberships` in this spec — when the discount-applying flow is implemented, it may add a `discount_amount` + `discount_source_payment_id` pair for audit. Flagged as deferred (see §8).

The `ix_dropin_payment_attendee_paid_at` index makes this lookup trivial.

## 7. Design Decisions (summary)

### 7.1 Two nullable FKs + XOR check on `attendance_registrations`

Considered alternatives:

- **Separate `drop_in_attendances` table.** Rejected. Every "list session attendees" query becomes a `UNION`. The session capacity counter has two write paths. The cancel-session fan-out duplicates. The roster page logic forks.
- **Polymorphic `(attendee_type, attendee_id)`.** Rejected. No DB-level FK integrity. Joins become ugly. RLS clarity drops.

**Chosen.** Single table, two nullable FKs (`student_id`, `drop_in_attendee_id`), three CHECK constraints. Single capacity counter, single status enum, single cancel fan-out path stays untouched.

### 7.2 Drop-in price on `programs`, not `program_plans`

Plans = membership tiers (different hour bundles, different costs). Drop-in is a single per-program price, orthogonal to plans. Per-plan drop-in price would force the question "which plan's drop_in_price applies when an unmembered person walks in?" with no business answer.

### 7.3 Drop-ins do NOT create memberships

Per business spec: drop-ins pay per class, no monthly commitment, no hour balance. The `attendance_registrations.membership_id` column is NULL for drop-in rows; no `MembershipCreated` event fires. No row in `hour_transactions`. The drop-in path is fully decoupled from the membership lifecycle.

### 7.4 Conversion link lives on `drop_in_attendees`

The `students` table is unchanged. The conversion link (`converted_to_student_id`, `converted_at`) sits on `drop_in_attendees`. Most students were never drop-ins; storing the inverse on `students` would mean a mostly-empty column. Reverse query ("is this student a converted drop-in?") is rare and indexed by `ix_dropin_converted_student`.

### 7.5 Visit counters denormalized

`total_visits`, `first_visit_at`, `last_visit_at` are maintained on the `drop_in_attendees` row. Updated in the same DB transaction as the registration's `REGISTERED → PRESENT` transition:

```sql
UPDATE drop_in_attendees
   SET total_visits  = total_visits + 1,
       last_visit_at = NOW(),
       first_visit_at = COALESCE(first_visit_at, NOW())
 WHERE id = :attendee_id;
```

Trade-off: write amplification on every PRESENT mark vs `COUNT(*)` on every read. Read path wins — recurring-attendee detection and the discount-eligibility lookup are hot, attendance marks are infrequent. Counter updates are **one-way**: a `PRESENT → ABSENT` correction does **not** decrement. Audit-friendly, corrections are rare, and any cleanup is an ops-tool concern.

### 7.6 Defensive cancel-session fan-out

Business rule says drop-in classes are not cancelled. Defensively, if `CancelSessionService` ever runs on a session that has drop-in registrations (e.g. admin registers a drop-in 5 minutes before forced cancellation), the fan-out treats drop-in rows uniformly with student rows: flips status to `CANCELLED_BY_SYSTEM`. The `drop_in_payments` row stays. Refund handled offline.

### 7.7 Cancellation reachability

Drop-ins cannot be cancelled by the customer. There is no `CANCELLED_BY_ADMIN` value and no plan to add one. The status enum keeps its current shape. A drop-in's lifecycle is `REGISTERED → PRESENT | ABSENT` (or `CANCELLED_BY_SYSTEM` if a session is force-cancelled per §7.6).

### 7.8 Conversion data flow (schema-level)

When a drop-in converts to a student (separate spec), the conversion service:

1. Creates a `students` row, copying `full_name` (split into `first_name` / `last_name` by application logic) and `phone` from the drop-in attendee.
2. The student's `email`, `identity_document_type`, `identity_number`, `tutor_*` fields, etc. are entered by the admin in the conversion form (drop-in does not have them).
3. Sets `drop_in_attendees.converted_to_student_id` and `converted_at` on the source row.

The drop-in attendee row is **not deleted** — its visit history and payment links remain intact for reporting and the discount lookup at the new membership's creation time.

## 8. Open / Deferred

These are explicitly **out of scope** for this migration but called out so the next spec catches them:

- **Discount-tracking columns on `memberships`.** When the discount flow is implemented, consider adding `discount_amount` and `discount_source_payment_id`. Decision deferred to that spec.
- **Phone normalization policy.** Drop-in phones currently entered raw (VARCHAR(20)). Tenant phones normalize via V062 (`+57` strip). Application-layer normalization recommended at write time to keep `uq_dropin_phone_per_tenant` collision-free. Deferred to the application spec.
- **Drop-in receipts / proof.** No printable receipt entity. If accounting needs it, separate spec.
- **Drop-in reporting.** Per-program drop-in revenue, attendee retention, conversion rate — all later.

## 9. Migration Plan

Single Flyway migration at the next available number:

- **`V069__create_drop_in_tables.sql`** — all five changes (two new tables, attendance generalization, programs column, audit constraint extension) in one file.

Rationale for one file rather than splitting: the changes are interlocked. The new CHECK on `attendance_registrations` references `drop_in_attendee_id`, which doesn't exist until the new column is added in the same migration. Splitting into V069/V070/... gains nothing operationally and complicates rollback reasoning. One atomic transaction either applies everything or fails.

The migration runs as `klasio_app`. All targeted tables are `klasio_app`-owned (verified via the ownership rule in `CLAUDE.md`).

## 11. Registration Flow — Goal & Scope

A staff user (`ADMIN`, `SUPERADMIN`, `MANAGER` of the program, or `PROFESSOR` assigned to the class) can register a drop-in attendee into a specific class session, capture their cash/transfer payment, and mark them PRESENT — all in a single atomic transaction. The flow handles both first-time and recurring drop-ins. No frontend in this spec; only the backend contract.

### 11.1 Non-goals

- No UI components (separate spec).
- No bulk drop-in registration (one attendee per request).
- No cancellation of a drop-in registration (per business rule §7.7).
- No refund handling (per business rule §7.6).
- No payment receipts.
- No conversion-to-student flow (separate spec).
- No SMS / email notifications to the drop-in attendee.

## 12. Module Layout

A new module `com.klasio.dropin` is introduced. It owns the `DropInAttendee` and `DropInPayment` aggregates and orchestrates the registration. It calls into the existing `attendance` module via a new outbound port to materialize the `AttendanceRegistration` row. Same shape as `RegisterWalkInService` calling `DeductHoursUseCase` across the membership boundary.

```
com.klasio.dropin
├── domain
│   ├── model
│   │   ├── DropInAttendee.java          (aggregate root)
│   │   ├── DropInAttendeeId.java         (value object)
│   │   ├── DropInPayment.java           (aggregate root)
│   │   ├── DropInPaymentId.java          (value object)
│   │   └── PaymentMethod.java            (enum: CASH, TRANSFER)
│   ├── event
│   │   ├── DropInAttendeeRegistered.java
│   │   └── DropInPaymentRecorded.java
│   └── port
│       ├── DropInAttendeeRepository.java
│       ├── DropInPaymentRepository.java
│       ├── DropInPriceLookupPort.java     (resolves programs.drop_in_price)
│       └── DropInAttendancePort.java      (driven port — implemented by attendance module)
├── application
│   ├── dto
│   │   ├── RegisterDropInCommand.java
│   │   ├── RegisterDropInResult.java
│   │   └── DropInAttendeeLookupResult.java
│   └── service
│       ├── RegisterDropInService.java
│       └── LookupDropInAttendeeService.java
└── infrastructure
    ├── persistence
    │   ├── DropInAttendeeJpaEntity.java
    │   ├── DropInPaymentJpaEntity.java
    │   ├── JpaDropInAttendeeRepository.java
    │   └── JpaDropInPaymentRepository.java
    ├── adapter
    │   └── ProgramDropInPriceAdapter.java   (implements DropInPriceLookupPort)
    └── web
        ├── DropInRegistrationController.java
        ├── DropInAttendeeLookupController.java
        └── dto
            ├── RegisterDropInRequest.java
            ├── RegisterDropInResponse.java
            └── DropInAttendeeLookupResponse.java
```

The `attendance` module gets:

- A new factory on `AttendanceRegistration`: `createDropIn(...)` — produces a row with `student_id`/`enrollment_id`/`membership_id`/`level_at_registration`/`intended_hours` all `NULL`, `drop_in_attendee_id` + `drop_in_payment_id` set, `status = PRESENT`, emitting `DropInAttendanceMarked`.
- A new domain event `DropInAttendanceMarked` (in `com.klasio.attendance.domain.event`).
- A new adapter `DropInAttendancePortAdapter` implementing the dropin module's `DropInAttendancePort` — bridges the call inbound.

## 13. Endpoints

### 13.1 Lookup (read-only, idempotent)

```
GET /api/v1/drop-in-attendees/lookup?phone={phone}
```

- **Auth:** `ADMIN, SUPERADMIN, MANAGER, PROFESSOR`
- **Tenant:** from JWT
- **200 OK** with `DropInAttendeeLookupResponse` if attendee exists in tenant
- **404 Not Found** if no attendee matches `(tenantId, phone)`
- No side effects, no domain events

### 13.2 Register (the main flow)

```
POST /api/v1/classes/{classId}/sessions/{sessionDate}/drop-in
```

- Path mirrors the walk-in convention.
- `{sessionDate}` is `yyyy-MM-dd`.
- **Auth:** `ADMIN, SUPERADMIN, MANAGER, PROFESSOR`
- **Tenant + actor:** from JWT
- **Request body:** `RegisterDropInRequest`
- **201 Created** with `RegisterDropInResponse` on first registration
- **200 OK** with `RegisterDropInResponse` on idempotent re-call (`attendeeWasNew=false`, no double-charge)
- Error codes documented in §17

### 13.3 RBAC enforcement

Same pattern as `RegisterWalkInService` (`api/src/main/java/com/klasio/attendance/application/service/RegisterWalkInService.java:82–97`):

- `PROFESSOR`: resolve `professorId` via `ProfessorIdLookupPort`; reject if `professorId != classView.professorId()`.
- `MANAGER`: reject if `programIdFromJwt != classView.programId()`.
- `ADMIN` / `SUPERADMIN`: no additional scope check.
- Failure → `AccessDeniedException` → 403.

## 14. DTOs

### 14.1 Web layer — Request

```java
public record RegisterDropInRequest(
    @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String startTime,
    @Valid @NotNull DropInAttendeeRef attendee,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotNull PaymentMethod paymentMethod
) {
    public record DropInAttendeeRef(
        UUID existingId,
        @Valid NewAttendee newAttendee
    ) {
        @AssertTrue(message = "exactly one of existingId or newAttendee must be set")
        public boolean isExactlyOne() {
            return (existingId == null) ^ (newAttendee == null);
        }
    }
    public record NewAttendee(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 20)  String phone
    ) {}
}
```

### 14.2 Web layer — Responses

```java
public record RegisterDropInResponse(
    UUID registrationId,
    UUID attendeeId,
    UUID paymentId,
    String status,                  // always "PRESENT"
    boolean attendeeWasNew,         // true if created on this call
    int attendeeTotalVisits         // counter post-increment
) {}

public record DropInAttendeeLookupResponse(
    UUID id,
    String fullName,
    String phone,
    int totalVisits,
    Instant firstVisitAt,           // nullable on a never-attended attendee (rare; only via direct create)
    Instant lastVisitAt,            // nullable likewise
    boolean converted               // converted_to_student_id IS NOT NULL
) {}
```

### 14.3 Application layer — Command

```java
public record RegisterDropInCommand(
    UUID tenantId,
    UUID classId,
    LocalDate sessionDate,
    LocalTime startTime,
    UUID existingAttendeeId,        // nullable
    String newAttendeeFullName,     // nullable
    String newAttendeePhone,        // nullable
    BigDecimal amount,
    PaymentMethod paymentMethod,
    UUID actorUserId,
    String actorRole,
    UUID programIdFromJwt           // nullable; required for MANAGER scope
) {}
```

### 14.4 Application layer — Result

```java
public record RegisterDropInResult(
    UUID registrationId,
    UUID attendeeId,
    UUID paymentId,
    boolean attendeeWasNew,
    int attendeeTotalVisits
) {}
```

## 15. Use Case — `RegisterDropInService.execute`

`@Service` with `@Transactional` (default `REQUIRED`, `READ_COMMITTED`). One execution = one DB transaction. Any exception triggers full rollback (attendee creation, payment row, attendance row, capacity increment all undone).

### 15.1 Execution order

1. **Load class view** via existing `ClassDetailsLookupPort.findForRegistration(tenantId, classId)`. Throws `ClassNotFoundException` (404) if missing.
2. **RBAC scope check** (see §13.3). Throws `AccessDeniedException` (403).
3. **Drop-in availability**: `DropInPriceLookupPort.findPrice(tenantId, classView.programId())`. If `Optional.empty()` → `DropInNotAvailableException` (422, code `DROP_IN_NOT_AVAILABLE`).
4. **Resolve schedule entry** for the session (one-time vs recurring) — reuse the helper from `RegisterWalkInService`.
5. **Time-window check**: `now ∈ [sessionStart - MARKING_WINDOW_MINUTES_BEFORE, sessionEnd + MARKING_WINDOW_MINUTES_AFTER]` using the existing `AttendanceTimeConstants` (20 / 10). Throws `MarkingWindowException` (409, code `MARKING_WINDOW_VIOLATION`).
6. **Find or create session** via `classSessionRepository.findOrCreate(...)`. Throws `SessionCancelledException` (409) if the resolved session is already cancelled.
7. **Resolve attendee** (in this exact order):
   - **If `existingAttendeeId != null`:** `dropInAttendeeRepository.findByIdAndTenant(existingAttendeeId, tenantId)`. If empty → `DropInAttendeeNotFoundException` (404, code `DROP_IN_ATTENDEE_NOT_FOUND`). RLS enforces tenant scoping; cross-tenant ids look identical to "not found" — intentional.
   - **Else (new):** `dropInAttendeeRepository.findByPhoneAndTenant(newAttendeePhone, tenantId)`:
     - If found → `PhoneAlreadyExistsException` (409, code `DROP_IN_PHONE_EXISTS`); response body carries `{existingAttendeeId, fullName, totalVisits}` so the UI can prompt "use existing?" and resubmit with `existingAttendeeId`.
     - Else → `DropInAttendee.create(tenantId, newAttendeeFullName, newAttendeePhone, actorUserId, now)`. Emits `DropInAttendeeRegistered`.
   - Set `attendeeWasNew = (existingAttendeeId == null && created in this branch)`.
8. **Idempotency lookup**: `dropInPaymentRepository.findByAttendeeAndSession(attendee.id(), session.id())`.
   - If present: load the corresponding `AttendanceRegistration` via `attendanceRegistrationRepository.findByDropInPayment(payment.id())`. Return `RegisterDropInResult` with the existing ids and `attendeeWasNew = false`. **No counter increment, no domain events, no audit row** — true idempotent return. Controller maps to **200 OK** (vs 201 on first call).
9. **Create payment**: `DropInPayment.create(tenantId, attendee.id(), session.id(), classView.programId(), amount, paymentMethod, actorUserId, now)`. Persist. Emits `DropInPaymentRecorded`.
10. **Reserve capacity + create attendance row**: call `DropInAttendancePort.recordPresent(RecordDropInPresentCommand)` — implemented by an attendance-module adapter. Inside the adapter:
    - Call `classSessionRepository.incrementCapacityIfSpace(sessionId, classView.maxStudents())`. If `false` → `SessionFullException` (409). Drop-ins occupy a physical seat (per §7 of the data model).
    - Call `AttendanceRegistration.createDropIn(tenantId, sessionId, classId, sessionDate, startTime, endTime, attendeeId, paymentId, actorUserId, now)`.
    - The factory sets `status = PRESENT` directly (no REGISTERED stop), `marked_at = now`, `marked_by = actorUserId`, `intended_hours`/`level_at_registration`/`student_id`/`enrollment_id`/`membership_id` all `NULL`.
    - Emits a single `DropInAttendanceMarked` event (not the student-only `AttendanceRegistered` + `AttendanceMarkedPresent` pair).
    - Persists. If the partial unique index `ux_reg_active_dropin_session` is violated (concurrent double-click), surfaces `DataIntegrityViolationException` → caught at the service boundary → re-runs the §15.1 step 8 idempotency lookup and returns the existing row (200 OK). This handles the rare race where two requests interleave between the lookup and the insert.
    - Caller passes `RecordDropInPresentCommand` with `maxCapacity` so the adapter has everything needed; cross-module flow stays clean (no `ClassSessionRepository` reach-through from the dropin module).
11. **Update counters** on the `DropInAttendee` aggregate via `attendee.recordVisit(now)`:
    ```
    total_visits  = total_visits + 1
    last_visit_at = now
    first_visit_at = COALESCE(first_visit_at, now)
    ```
    Persist. One-way (per §7.5). Skipped on the idempotent-return path (step 8).
12. **Return `RegisterDropInResult`**. Controller maps to 201 Created.

### 15.2 Domain invariants

- `DropInAttendee.create(...)` rejects blank `fullName` / blank `phone`.
- `DropInPayment.create(...)` rejects `amount.signum() <= 0`. Also rejects null `paymentMethod`.
- `DropInPayment` is immutable post-creation.
- `AttendanceRegistration.createDropIn(...)` rejects null `attendeeId`, null `paymentId`. Sets `status = PRESENT` exactly once.

### 15.3 Cross-module ports (new)

```java
// dropin module — outbound
public interface DropInPriceLookupPort {
    Optional<BigDecimal> findPrice(UUID tenantId, UUID programId);
}

public interface DropInAttendancePort {
    UUID recordPresent(RecordDropInPresentCommand cmd);

    record RecordDropInPresentCommand(
        UUID tenantId,
        UUID sessionId,
        UUID classId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity,           // for atomic incrementCapacityIfSpace
        UUID attendeeId,
        UUID paymentId,
        UUID actorUserId,
        Instant now
    ) {}
}
```

`DropInPriceLookupPort` is implemented inside the dropin module by `ProgramDropInPriceAdapter` (queries `programs.drop_in_price` directly via JdbcTemplate or a Spring Data projection — no need to round-trip through the program module's domain layer).

`DropInAttendancePort` is implemented inside the **attendance** module by `DropInAttendancePortAdapter`, which composes the existing `ClassSessionRepository.incrementCapacityIfSpace`, the new `AttendanceRegistration.createDropIn()` factory, and `AttendanceRegistrationRepository.save(...)`. The dropin module never reaches into attendance internals — all attendance interaction goes through this single port.

## 16. Domain Events

Three new events. All consumed by `AuditEventListener` (existing pattern: `api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java`) to write audit rows.

```java
// dropin module
public record DropInAttendeeRegistered(
    UUID attendeeId,
    UUID tenantId,
    String fullName,
    String phone,
    UUID actorId,
    Instant occurredAt
) {}

public record DropInPaymentRecorded(
    UUID paymentId,
    UUID attendeeId,
    UUID sessionId,
    UUID programId,
    UUID tenantId,
    BigDecimal amount,
    BigDecimal programDropInPrice,    // for reconciliation when admin overrides default
    PaymentMethod paymentMethod,
    UUID actorId,
    Instant occurredAt
) {}

// attendance module
public record DropInAttendanceMarked(
    UUID registrationId,
    UUID sessionId,
    UUID classId,
    UUID tenantId,
    UUID attendeeId,
    UUID paymentId,
    LocalDate sessionDate,
    UUID actorId,
    Instant occurredAt
) {}
```

### 16.1 Audit listener mappings

| Event | `action_type` | `entity_type` | `entity_id` | `details` JSON keys |
|---|---|---|---|---|
| `DropInAttendeeRegistered` | `DROP_IN_ATTENDEE_REGISTERED` | `DROP_IN_ATTENDEE` | `attendeeId` | `fullName`, `phone` |
| `DropInPaymentRecorded` | `DROP_IN_PAYMENT_RECORDED` | `DROP_IN_PAYMENT` | `paymentId` | `attendeeId`, `sessionId`, `programId`, `amount`, `programDropInPrice`, `paymentMethod` |
| `DropInAttendanceMarked` | `DROP_IN_ATTENDANCE_MARKED` | `ATTENDANCE_REGISTRATION` | `registrationId` | `sessionId`, `classId`, `attendeeId`, `paymentId`, `sessionDate` |

Including `programDropInPrice` alongside `amount` in `DROP_IN_PAYMENT_RECORDED` lets reporting reconcile admin overrides without joining `programs` historically (the program's price may change later — the snapshot captures intent at the moment of payment).

## 17. Error Catalog

Backend returns English `message` plus a stable machine-readable `code` in `ErrorResponse.ErrorDetail`. Frontend (next-intl) maps `code` → localized string. No `MessageSource` work on the backend.

| Code | HTTP | Cause | Body extras |
|---|---|---|---|
| `CLASS_NOT_FOUND` | 404 | `classId` not in tenant | — |
| `SESSION_CANCELLED` | 409 | Session already cancelled | — |
| `DROP_IN_NOT_AVAILABLE` | 422 | `programs.drop_in_price IS NULL` | — |
| `MARKING_WINDOW_VIOLATION` | 409 | Outside `[start-20m, end+10m]` | — |
| `SESSION_FULL` | 409 | `current_capacity == max_students` | — |
| `DROP_IN_PHONE_EXISTS` | 409 | `newAttendee.phone` collides with existing attendee in tenant | `existingAttendeeId`, `fullName`, `totalVisits` |
| `DROP_IN_ATTENDEE_NOT_FOUND` | 404 | `existingAttendeeId` missing or cross-tenant | — |
| `INVALID_REQUEST` | 400 | Bean validation failure (both/neither attendee branches, malformed startTime, amount ≤ 0, etc.) | field errors |
| `FORBIDDEN` | 403 | RBAC scope mismatch | — |

`DROP_IN_PHONE_EXISTS` is the explicit "do NOT silently overwrite" path. The body carries the existing attendee's identity so the client can prompt the staff user; the staff user resubmits with `existingAttendeeId`. No name update on subsequent registrations — names are captured on first visit only and treated as immutable through this flow.

## 18. Idempotency

Two layers:

1. **Pre-insert lookup (§15.1 step 8).** Catches the common case: same staff user clicks "Register" twice in close succession. The second call observes the existing payment and short-circuits to a 200 OK return.
2. **Post-insert race recovery (§15.1 step 11 fallback).** Catches the rare case: two concurrent requests for the same `(attendeeId, sessionId)` pass the lookup before either has inserted. The partial unique index `ux_reg_active_dropin_session` (and `uq_dropin_payment_per_session` on the payment table) raises `DataIntegrityViolationException`; the service catches it, re-runs the lookup, and returns the now-visible row.

No `Idempotency-Key` header convention. The `(attendeeId, sessionId)` natural key is sufficient. Counters are incremented exactly once because the `recordVisit` step is skipped on both idempotent paths.

## 19. Phone Normalization (deferred)

Drop-in `phone` is currently `VARCHAR(20)` with no application-level normalization. Strict equality on lookup means `"3001234567"` and `"+57 300 123 4567"` are treated as different attendees, which weakens the recurring-attendee detection.

A follow-up spec will:
- Decide normalization strategy (E.164? strip everything but digits? align with V062's `+57` prefix strip?).
- Add a deterministic transform at write time (controller or service layer).
- Migrate existing rows in a backfill migration.

For this release, the UI will enforce digit-only input and prepend a tenant-default country code at the form level — same convention as the tenant phone form (V060). The backend accepts whatever it receives.

## 20. Test Plan

Tests are written as part of the implementation, not this spec.

### 20.1 Migration tests (V069)

- Migration smoke test: fresh DB → apply through V069 → `pg_dump --schema-only` snapshot.
- Constraint tests (Testcontainers + JDBC):
  - `uq_dropin_phone_per_tenant`: same phone, same tenant → conflict; same phone, different tenant → OK.
  - `chk_reg_attendee_xor`: both FKs set → fail; neither set → fail; only one → OK.
  - `chk_reg_student_full`: student row missing `enrollment_id` / `membership_id` / `level_at_registration` / `intended_hours` → fail.
  - `chk_reg_dropin_payment`: drop-in row missing `drop_in_payment_id` → fail.
  - `uq_dropin_payment_per_session`: second payment for same (attendee, session) → fail.
  - `ux_reg_active_dropin_session`: second PRESENT row for same (attendee, session) → fail; a new PRESENT row when the prior row is `CANCELLED_BY_SYSTEM` (defensive cancel-session fan-out) → OK.
  - `chk_dropin_conversion_pair`: half-set conversion fields → fail.
- RLS tests across two tenants.

### 20.2 Domain tests (`com.klasio.dropin.domain.model`)

- `DropInAttendee.create` rejects blank `fullName` / `phone`.
- `DropInAttendee.recordVisit` increments `total_visits`, sets `last_visit_at`, sticks `first_visit_at`.
- `DropInAttendee.recordVisit` does NOT decrement on second call with earlier timestamp (last-write-wins for `last_visit_at`, sticky for `first_visit_at`).
- `DropInAttendee.convertToStudent(studentId, now)` sets both fields atomically; rejects double-conversion.
- `DropInPayment.create` rejects `amount <= 0`, null `paymentMethod`.
- `AttendanceRegistration.createDropIn` rejects null `attendeeId` / `paymentId`; produces row with `status = PRESENT`, all student-side fields NULL, emits exactly one `DropInAttendanceMarked`.

### 20.3 Application tests (`RegisterDropInService`, Mockito)

- **Happy path — new attendee.** Creates attendee, payment, attendance row. Counters at 1. All three domain events emitted. `attendeeWasNew = true`.
- **Happy path — existing attendee.** No `DropInAttendeeRegistered` event. Counters incremented from prior value. `attendeeWasNew = false`.
- **Idempotent re-call.** Second call with same `(attendeeId, sessionId)` returns existing ids, no duplicate payment, no counter increment, no events.
- **Phone collision, different name.** Throws `PhoneAlreadyExistsException` with existing attendee data populated.
- **Program lacks `drop_in_price`.** Throws `DropInNotAvailableException`.
- **Capacity full.** `incrementCapacityIfSpace` returns `false` → `SessionFullException`. Verify no orphan attendee or payment row (transaction rolled back).
- **Outside time window.** Three sub-cases: too early, too late, exact boundary inclusive.
- **RBAC: PROFESSOR not assigned to class.** `AccessDeniedException`.
- **RBAC: MANAGER's `programIdFromJwt` differs from class's program.** `AccessDeniedException`.
- **RBAC: ADMIN.** No scope check applied.
- **Cross-tenant `existingAttendeeId`.** Throws `DropInAttendeeNotFoundException`.
- **Amount > 0 enforcement.** Service-level (in addition to bean-validation).
- **Amount differs from program's `drop_in_price`.** Allowed; `DropInPaymentRecorded` event carries both `amount` and `programDropInPrice`.

### 20.4 Integration tests (Testcontainers)

- **Full-stack happy path.** POST → verify rows in `drop_in_attendees`, `drop_in_payments`, `attendance_registrations`, plus three `audit_log` rows with the expected `action_type` values.
- **Atomic rollback on capacity failure.** Force `SessionFullException` → verify zero rows added in any of the three tables.
- **Concurrent double-click.** Two threads POST simultaneously with identical body → exactly one 201, one 200, single payment row, single registration row, counters incremented exactly once.
- **Partial unique index enforcement.** Direct INSERT bypassing service: second `REGISTERED` row for same `(drop_in_attendee_id, session_id)` → DB-level conflict.
- **Idempotency across sessions, same day.** Same attendee, two different sessions → two payments, counters at 2 after both PRESENT.
- **Cancel-session fan-out.** Existing `CancelSessionService` cancels a session containing one student row + one drop-in row → both flip to `CANCELLED_BY_SYSTEM`. Drop-in payment row stays. (Cross-references §7.6.)
- **RLS smoke.** Two tenants, identical phone numbers, register both → no cross-tenant visibility on lookup.

### 20.5 Controller tests (MockMvc)

- 401 when unauthenticated.
- 403 when role not in {ADMIN, SUPERADMIN, MANAGER, PROFESSOR}.
- 400 on bean validation failures: missing `attendee`, both `existingId` and `newAttendee` set, neither set, malformed `startTime`, `amount = 0`, blank `fullName`, blank `phone`, `phone` length > 20, `fullName` length > 200.
- 200 vs 201 distinction (idempotent return vs first-time create).
- `DROP_IN_PHONE_EXISTS` body schema: contains `existingAttendeeId`, `fullName`, `totalVisits`.

### 20.6 GlobalExceptionHandler tests

Each new exception class → expected `ErrorResponse.ErrorDetail` with code + HTTP status as specified in §17.

---

## 21. UI — Goal & Constraints

The receptionist registers drop-in attendees from the existing class-session attendance screen. The flow runs at peak hours (e.g., 6 PM, 20 walk-up payments in a few minutes), so **speed is the primary UX requirement**. Every saved second compounds into shorter queues at the door.

### 21.1 Performance budget

- **Returning visitor (lookup hit, autofill):** end-to-end ≤ 15 s.
- **New visitor:** end-to-end ≤ 30 s.

### 21.2 Hard requirements

- Phone is the lookup key; first focused field; debounced lookup at ~300 ms.
- Keyboard-first: full Tab order, Enter submits, Escape cancels. Receptionists are faster on keyboard than mouse.
- Validation errors render inline (next to the field or in a dedicated error band above the submit row). **Never as a toast.**
- Submit button shows spinner + disables on click; double-click cannot fire two requests.
- Full English + Spanish via the existing next-intl setup (`web/messages/en.json`, `web/messages/es.json`).
- Visual consistency: reuse `Badge`, `Button`, `Input`, `Select` from `web/src/components/ui/*`. No new patterns invented.
- Drop-ins live inside the existing roster — same screen, no navigation away.

### 21.3 Non-goals (for this UI iteration)

- No bulk drop-in registration (one attendee per modal open).
- No drop-in detail page or history view.
- No drop-in cancel button (drop-ins cannot be cancelled per business rule §7.7).
- No conversion-to-student CTA in this modal (separate spec).
- No CARD payment method (UI restricted to `CASH | TRANSFER`, matching backend §5.2).
- No email field (backend has no email column per §5.1).
- No "override existing name" branch on the phone-collision dialog (backend names are immutable per §17).

## 22. Component Inventory

### 22.1 New files

| File | Purpose |
|---|---|
| `web/src/components/attendance/DropInButton.tsx` | Sibling to `WalkInButton` in `ClassRosterPanel`; opens the modal; conditionally rendered when `program.dropInPrice != null`. |
| `web/src/components/attendance/DropInModal.tsx` | The registration form modal (§23). |
| `web/src/components/attendance/PhoneCollisionDialog.tsx` | Race-recovery confirmation (§24). |
| `web/src/hooks/useDropInLookup.ts` | Debounced GET `/drop-in-attendees/lookup?phone=...`; returns `{data, isLoading, status: "idle"\|"searching"\|"found"\|"notFound"\|"error"}`. |
| `web/src/hooks/useRegisterDropIn.ts` | POST `/classes/{classId}/sessions/{sessionDate}/drop-in`; returns `{mutate, isPending, error}` mirroring `useWalkInBulkRegistration`. |
| `web/src/lib/api/dropIn.ts` | Typed API functions (`lookupDropIn`, `registerDropIn`) using the existing `api` wrapper from `web/src/lib/api.ts`. |

### 22.2 Modified files

| File | Change |
|---|---|
| `web/src/components/ui/Badge.tsx` | Add `dropIn` variant (Tailwind classes `bg-violet-100 text-violet-700`, distinct from existing `active`/`info`/`pending`/`rejected`/`inactive`). |
| `web/src/components/attendance/RegistrationStatusBadge.tsx` | Export a small helper `<DropInTag />` rendering `<Badge variant="dropIn" label="DROP-IN" small />` for clean reuse on roster rows. |
| `web/src/components/attendance/ClassRosterPanel.tsx` | Splice `<DropInButton />` next to `<WalkInButton />` (lines 179–188); render drop-in roster rows with attendee fields instead of student fields when `row.dropInAttendeeId != null`. |
| `web/src/components/attendance/AttendanceMarkingPanel.tsx` | Same drop-in row rendering (read-only — drop-ins are already PRESENT, no marking action). |
| `web/messages/en.json` + `web/messages/es.json` | New namespace `attendance.dropIn.*` (§27). |
| `web/src/types/program.ts` | Add `dropInPrice: string \| null` (BigDecimal serialized as string) to `ProgramDetail`. |
| `web/src/types/attendance.ts` | Extend roster row type with drop-in fields (§25.2). |

### 22.3 Backend roster endpoint extension (cross-cutting)

The current roster endpoint returns student-attendance rows only. To render drop-in rows inline, the response DTO is extended with optional drop-in fields. **Payment method is intentionally excluded from the roster — only attendee identity + paid amount surface.** See §25.2 for the field list.

## 23. DropInModal — Layout & States

### 23.1 Layout

```
┌─────────────────────────────────────────────────────┐
│ Register drop-in                                [×] │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Phone *                                             │
│ ┌───────┬─────────────────────────────────┐         │
│ │ +57 ▼ │ 3001234567                      │ [autofocus]
│ └───────┴─────────────────────────────────┘         │
│                                                     │
│ ✓ Recurring visitor — 4th visit       (lookup hit)  │
│                                                     │
│ Full name *                                         │
│ ┌─────────────────────────────────────────┐         │
│ │ María García                            │ ← read-only when lookup hit
│ └─────────────────────────────────────────┘         │
│                                                     │
│ Amount (COP) *           Payment method *           │
│ ┌────────────────────┐   ┌──────────┬──────────┐    │
│ │ 25,000             │   │ ● Cash   │ ○ Transfer│    │
│ └────────────────────┘   └──────────┴──────────┘    │
│                                                     │
│ ┌─ Inline error band (only when error) ─────────┐   │
│ └────────────────────────────────────────────────┘   │
│                                                     │
│              [Cancel]  [Register & mark present]    │
└─────────────────────────────────────────────────────┘
```

### 23.2 Field order + tab order

1. **Phone** — `inputMode="tel"`, autofocus on modal open, country-code prefix selector (defaults to tenant's country code; reuses the same component as the tenant phone form per V060). Max 20 chars.
2. **Full name** — disabled until lookup completes. Read-only when lookup hit; editable when miss. Max 200 chars.
3. **Amount** — pre-filled with `program.dropInPrice` formatted via the existing currency formatter. Numeric input. Editable.
4. **Payment method** — segmented control, two options: `Cash` (default selected), `Transfer`. Arrow-Left / Arrow-Right cycles. Space toggles selection.
5. **Submit** — primary button (`Register & mark present`). `Enter` from any field submits when the form is valid.

`Cancel` reachable via Shift+Tab from Submit; `Escape` closes the modal anywhere.

### 23.3 Interaction states

| State | Trigger | Behavior |
|---|---|---|
| `idle` | modal opened | Phone enabled + autofocused; Name disabled; Amount/Payment/Submit dimmed and unfocusable. |
| `searching` | phone has ≥ 7 digits, debounce fired | small `<Loader2>` inside the phone input's right edge; other fields stay disabled. |
| `found` | lookup returned 200 | green banner "Recurring visitor — Nth visit"; Name autofilled + read-only; cursor jumps to Amount; submit will use `existingAttendeeId` from the lookup result. |
| `notFound` | lookup returned 404 | Name field becomes editable; cursor jumps to Name; submit will use `newAttendee`. |
| `lookupError` | lookup network failure | Inline error band: "Could not check phone. Try again." Phone editable; Submit disabled until next successful lookup. |
| `submitting` | Submit clicked, request in flight | Submit button disabled with spinner; all fields locked. |
| `phoneCollision` | submit returned 409 `DROP_IN_PHONE_EXISTS` | overlay `PhoneCollisionDialog` (§24). |
| `done` | submit returned 201 (or 200 idempotent) | inline green banner "Registered. {fullName} marked PRESENT." for 1.5 s; modal closes; roster refetches. |
| `error` | submit returned other 4xx / 5xx | inline red banner above Submit row with localized message from error code (§17); Submit re-enabled; fields re-enabled. |

### 23.4 Loading state convention

Mirror `WalkInModal` exactly (`web/src/components/attendance/WalkInModal.tsx:347–362`):

```tsx
<button
  type="submit"
  disabled={!canSubmit || isPending}
  className="… disabled:opacity-50 disabled:cursor-not-allowed"
>
  {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
  {t("submitButton")}
</button>
```

`canSubmit` is computed from: phone length, name non-empty, amount > 0, payment method selected, lookup state ≠ `searching`/`lookupError`.

### 23.5 Success banner

No external toast library. Inline alert banner mirrors `AttendanceMarkingPanel.tsx:131–145` (green band with `Bell` icon). Auto-dismisses after 1.5 s, then closes the modal and triggers `onRegistered()` callback (parent re-fetches roster).

### 23.6 Field-level validation

- **Phone:** required, length 7–20 digits, digits-only after country prefix. Inline error below the phone row.
- **Name:** required when state is `notFound`, length 1–200, no leading/trailing whitespace. Inline error below the name row.
- **Amount:** required, > 0, decimal up to 2 places, ≤ DECIMAL(15,2) range. Inline error below the amount row.
- **Payment method:** always one of the two options; cannot become unselected.

All field-level errors render via the existing pattern (red text, `text-red-600 text-xs mt-1`, no toast).

## 24. PhoneCollisionDialog (Race Recovery)

Triggered only when the frontend submitted with `newAttendee` (lookup said 404 at debounce time) but backend returned **409 `DROP_IN_PHONE_EXISTS`** (race: another tab created the attendee between lookup and submit, or user typed a slightly different phone after lookup).

### 24.1 Layout

```
┌──────────────────────────────────────────────────┐
│ Phone already registered                         │
├──────────────────────────────────────────────────┤
│ +57 3001234567 belongs to María García           │
│ (4 previous visits).                             │
│                                                  │
│ Same person?                                     │
│                                                  │
│           [Cancel]   [Yes, use existing record]  │
└──────────────────────────────────────────────────┘
```

### 24.2 Branches

- **Yes, use existing record:** dialog closes; the parent `DropInModal` resubmits the same payment + amount but with `attendee.existingId = response.body.existingAttendeeId` instead of `newAttendee`. UX-wise the user perceives a single click → success.
- **Cancel:** dialog closes; modal returns to the form with the phone field focused so the user can correct it or close.

There is **no "override name" branch** (per C3 / §17). If staff typed a wrong name and meant a different person, they cancel and try a different phone; if they meant the same person, "Yes" is the answer.

### 24.3 Source data

Body of the 409 response carries `{ existingAttendeeId, fullName, totalVisits }` per §17. The dialog renders these directly.

## 25. Roster Integration

### 25.1 Drop-In row rendering

A drop-in row sits in the same roster list as student rows. Detection key is `row.dropInAttendeeId != null`.

```
┌────────────────────────────────────────────────┐
│ María García         [DROP-IN]      [PRESENT] │
│ +57 300 123 4567                              │
│ Paid: $25,000                                  │
└────────────────────────────────────────────────┘
```

- `[DROP-IN]` is a `<Badge variant="dropIn" small label="DROP-IN" />`, placed **before** the existing status badge.
- Student-only fields (level, available hours) are **omitted** for drop-in rows.
- "Paid: $25,000" surfaces the amount only — **no payment method** in the roster (per C4).
- Drop-in rows are **read-only** in `AttendanceMarkingPanel` — drop-ins are already PRESENT and have no actionable status changes (no cancel, no mark absent, no level change).

### 25.2 Backend roster DTO extension

The existing roster endpoint's row DTO gains optional fields. All `null` for student rows; populated for drop-in rows.

```java
// Existing (kept, simplified):
record AttendanceRosterRow(
    UUID registrationId,
    UUID studentId,                   // null for drop-in
    String studentFullName,           // null for drop-in
    String levelAtRegistration,       // null for drop-in
    Integer intendedHours,            // null for drop-in
    String status,                    // PRESENT / REGISTERED / etc.
    // ...

    // New drop-in fields (null for student rows):
    UUID dropInAttendeeId,
    String dropInAttendeeName,
    String dropInAttendeePhone,
    BigDecimal dropInPaymentAmount    // payment_method NOT included per C4
) {}
```

The roster query joins `attendance_registrations` → `drop_in_attendees` → `drop_in_payments` via the FKs added in §5.3. RLS handles tenant scoping. Sort order unchanged.

This change is in the **attendance module**, not the dropin module (the roster is an attendance-module read concern). The dropin module exposes a small inbound port for the attendance module to fetch attendee + payment info, OR the JPA query joins the tables directly via JdbcTemplate. **Implementation choice deferred to the implementation plan.**

### 25.3 "Register drop-in" button visibility

Lives next to `<WalkInButton />` in `ClassRosterPanel.tsx:179–188`. Conditional render:

```tsx
{canManage && sessionStatus !== "CANCELLED" && program.dropInPrice != null && (
  <DropInButton
    classId={classId}
    sessionDate={session.sessionDate}
    startTime={session.startTime}
    programDropInPrice={program.dropInPrice}
    onRegistered={refetch}
  />
)}
```

The check `program.dropInPrice != null` requires `ProgramDetail` to expose `dropInPrice`. Backend's program detail DTO is extended with `dropInPrice: BigDecimal | null` — small change, separate from the dropin module.

## 26. Hooks + API Client

### 26.1 `useDropInLookup`

```ts
export function useDropInLookup(phone: string, debounceMs = 300):
  { status: "idle" | "searching" | "found" | "notFound" | "error";
    data: DropInAttendeeLookupResponse | null;
    error: ApiError | null; }
```

Behavior:
- Empty/short phone (< 7 digits) → `idle`.
- After debounce, fires `GET /drop-in-attendees/lookup?phone={phone}` via the shared `api.get(...)` wrapper.
- Maps 404 → `notFound`, 200 → `found` (with `data`), other errors → `error`.
- Cancels in-flight request on phone change (AbortController).

### 26.2 `useRegisterDropIn`

```ts
export function useRegisterDropIn(classId: string, sessionDate: string):
  { mutate: (input: RegisterDropInInput) => Promise<RegisterDropInResponse>;
    isPending: boolean;
    error: ApiError | null; }
```

Mirrors `useWalkInBulkRegistration` shape. Calls `POST /classes/{classId}/sessions/{sessionDate}/drop-in`. Returns the response on 201 or 200 (idempotent). Surfaces `ApiError` for the modal to render inline.

### 26.3 API functions in `web/src/lib/api/dropIn.ts`

```ts
export async function lookupDropIn(phone: string, signal?: AbortSignal): Promise<DropInAttendeeLookupResponse | null> { … }
export async function registerDropIn(
  classId: string,
  sessionDate: string,
  payload: RegisterDropInInput
): Promise<RegisterDropInResponse> { … }
```

Both use the existing `api` wrapper which throws `ApiError` (auth refresh + cookie handling included).

## 27. i18n

New namespace `attendance.dropIn.*` added to both `web/messages/en.json` and `web/messages/es.json`. Reuses the existing nesting + plural conventions.

```json
"attendance": {
  "dropIn": {
    "buttonLabel": "Register drop-in",
    "modalTitle": "Register drop-in",
    "phoneLabel": "Phone",
    "phonePlaceholder": "300 123 4567",
    "fullNameLabel": "Full name",
    "amountLabel": "Amount (COP)",
    "paymentMethodLabel": "Payment method",
    "paymentMethod": {
      "cash": "Cash",
      "transfer": "Transfer"
    },
    "recurringVisitor": "Recurring visitor — {count, plural, one {# visit} other {# visits}}",
    "submitButton": "Register & mark present",
    "cancelButton": "Cancel",
    "successBanner": "Registered. {fullName} marked PRESENT.",
    "rosterTag": "DROP-IN",
    "rosterAmountLabel": "Paid",
    "errors": {
      "phoneRequired": "Phone is required.",
      "phoneInvalid": "Phone must be 7–20 digits.",
      "nameRequired": "Full name is required.",
      "amountRequired": "Amount is required.",
      "amountInvalid": "Amount must be greater than zero.",
      "lookupFailed": "Could not check phone. Try again.",
      "DROP_IN_NOT_AVAILABLE": "This program does not allow drop-ins.",
      "MARKING_WINDOW_VIOLATION": "Outside the registration window for this session.",
      "SESSION_FULL": "This session is full.",
      "SESSION_CANCELLED": "This session has been cancelled.",
      "DROP_IN_ATTENDEE_NOT_FOUND": "Attendee not found.",
      "FORBIDDEN": "You do not have permission to register drop-ins for this class.",
      "UNKNOWN": "Something went wrong. Try again."
    },
    "phoneCollision": {
      "title": "Phone already registered",
      "body": "{phone} belongs to {fullName} ({count, plural, one {# previous visit} other {# previous visits}}).",
      "question": "Same person?",
      "yes": "Yes, use existing record",
      "cancel": "Cancel"
    }
  }
}
```

The Spanish translations live in the parallel file with identical key shape. Error codes from §17 map 1:1 to keys under `errors.*`; an unrecognized code falls back to `errors.UNKNOWN`.

## 28. Visual & Accessibility Notes

- **Color:** drop-in violet badge (`bg-violet-100 text-violet-700`) — distinct from existing `active`/`info`/`pending`/`rejected`/`inactive` so it reads as a category, not a status.
- **Focus order matches DOM order;** no `tabindex` overrides.
- **Modal traps focus** while open (existing `WalkInModal` does not implement focus-trap; this modal should — small a11y improvement, ~10 LoC).
- **Escape** closes the modal everywhere; **Enter** submits when valid.
- **`aria-live="polite"`** on the success banner and the lookup result banner so screen readers announce them without interrupting input.
- **Field labels** are `<label>` elements wired via `htmlFor`; not placeholder-only labels.

## 29. UI Test Plan (extends §20)

Frontend tests run with the existing Jest + React Testing Library setup.

### 29.1 Component tests

- **`DropInModal` happy path — new visitor.** Type phone (lookup miss) → fill name → submit → mock POST returns 201 → success banner appears → modal closes after 1.5 s → `onRegistered` called.
- **`DropInModal` happy path — recurring visitor.** Type phone → mock GET returns 200 with `totalVisits=3` → "Recurring visitor — 4th visit" banner appears → name field is read-only and pre-filled → submit uses `existingAttendeeId`.
- **Lookup debounce.** Type phone in rapid succession → only one GET fires after the debounce window.
- **Submit disabled until form valid.** Empty phone, partial phone, missing name on miss-state, zero amount each leave button disabled.
- **Idempotent re-call.** Mock POST returns 200 instead of 201 → success banner still renders, no duplicate request fires.
- **Phone-collision dialog (race recovery).** Mock POST returns 409 `DROP_IN_PHONE_EXISTS` with body → dialog appears → "Yes, use existing" triggers exactly one resubmit with `existingAttendeeId` → success.
- **Phone-collision dialog cancel.** Dialog Cancel returns to form, phone focused.
- **Error mapping.** Each error code from §17 renders the matching `attendance.dropIn.errors.*` localized string.
- **Keyboard nav.** Tab cycles Phone → Name → Amount → Payment → Submit; Shift+Tab reverses; Enter submits when valid; Escape closes.
- **Spanish locale.** Snapshot test with `NEXT_LOCALE=es` shows Spanish strings throughout.

### 29.2 Roster integration tests

- **Drop-in row renders.** Mock roster row with `dropInAttendeeId != null` → row shows DROP-IN badge, attendee name + phone, `Paid: $25,000`, no payment method, no level/hours fields.
- **Student row unchanged.** Mock roster row without drop-in fields → row unchanged from current behavior.
- **DropInButton visibility.** When `program.dropInPrice == null` → button absent. When `dropInPrice != null` and `canManage = true` → button present. When session is `CANCELLED` → button absent.

### 29.3 Hook tests

- `useDropInLookup` state machine: `idle → searching → found / notFound / error`; aborts in-flight request on phone change.
- `useRegisterDropIn` returns `{mutate, isPending, error}`; `isPending` toggles correctly across the lifecycle of a request.

---

**Next step:** invoke the writing-plans skill to produce a single implementation plan covering V069 migration (data model), backend registration flow (dropin module + attendance module extensions + tests), and the frontend modal + roster integration + i18n + frontend tests.
