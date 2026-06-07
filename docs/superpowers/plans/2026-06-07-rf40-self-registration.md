# RF-40 Student Self-Registration (Form Parity) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a prospective student self-register at `https://{slug}.klasio.app/register` using the exact same form, validation, creation path, and account-setup email as admin student-creation — gated by a superadmin `selfRegistrationEnabled` tenant flag.

**Architecture:** Approach A — `RegisterStudentService` stops creating users/profiles itself and delegates to the existing `CreateStudentUseCase` (single source of truth). Identity-number uniqueness moves into that shared path (also fixes the admin path). A new `selfRegistrationEnabled` boolean becomes the first tenant feature-flag. The frontend retires the divergent `RegistrationForm` and reuses the canonical admin `StudentForm`, with tenant resolved from the `Host` subdomain.

**Tech Stack:** Java 21 / Spring Boot 3.4.3 / Spring Data JPA / Flyway / JUnit 5 + Mockito. Next.js 15.1 / React 19 / TS 5.9 / Tailwind / next-intl / Jest.

**Spec:** `docs/superpowers/specs/2026-06-07-rf40-self-registration-design.md`

---

## Conventions for every task

- **Backend tests:** from `api/` run `./mvnw test -Dtest=<ClassName>` (single) or `./mvnw -Dtest='<ClassName>#<method>' test`.
- **Frontend tests:** from `web/` run `npm test -- <pathOrPattern>`.
- **Commits:** Conventional Commits, scope `auth` / `student` / `tenant` / `web`. Single author (no co-author trailer).
- **Branch:** `feature/040-self-registration` (already created).
- TDD: write the failing test first, watch it fail, implement minimally, watch it pass, commit.

---

## File Structure

**Backend — new files**
- `api/src/main/java/com/klasio/shared/infrastructure/exception/StudentIdentityNumberAlreadyExistsException.java` — explicit conflict for admin path.
- `api/src/main/java/com/klasio/auth/domain/exception/SelfRegistrationDisabledException.java` — flag-off rejection.
- `api/src/main/java/com/klasio/auth/domain/exception/SelfRegistrationConflictException.java` — generic, non-enumerating conflict for the public path.
- `api/src/main/java/com/klasio/tenant/domain/event/TenantSelfRegistrationToggled.java` — audit event.
- `api/src/main/java/com/klasio/tenant/application/dto/ToggleSelfRegistrationCommand.java`
- `api/src/main/java/com/klasio/tenant/application/port/input/ToggleSelfRegistrationUseCase.java`
- `api/src/main/java/com/klasio/tenant/application/service/ToggleSelfRegistrationService.java`
- `api/src/main/resources/db/migration/V072__add_self_registration_enabled_to_tenants.sql`

**Backend — modified files**
- `student/application/service/CreateStudentService.java` — add identity-number uniqueness.
- `student/domain/port/StudentRepository.java` + `infrastructure/persistence/JpaStudentRepository.java` — add `existsByIdentityNumberInTenant`.
- `auth/application/dto/RegisterStudentCommand.java` — full structured field set.
- `auth/application/service/RegisterStudentService.java` — delegate to `CreateStudentUseCase`; flag guard; error mapping.
- `auth/infrastructure/web/RegistrationController.java` — expanded request DTO.
- `auth/application/port/TenantResolverPort.java` + its adapter — add `isSelfRegistrationEnabled`.
- `auth/application/port/StudentProfilePort.java` + `auth/infrastructure/adapter/StudentProfileAdapter.java` — delete `createStudentProfile` (divergent path).
- `tenant/domain/model/Tenant.java` — field + create/reconstitute params + toggle methods.
- `tenant/infrastructure/persistence/TenantJpaEntity.java` + `TenantMapper.java` — column + mapping.
- `tenant/application/dto/CreateTenantCommand.java` + `application/service/CreateTenantService.java` — default true / pass-through.
- `tenant/infrastructure/web/TenantController.java` — toggle endpoint (SUPERADMIN).
- `audit/infrastructure/persistence/AuditEventListener.java` — `onTenantSelfRegistrationToggled`.
- `shared/infrastructure/exception/GlobalExceptionHandler.java` — map the 3 new exceptions.

**Frontend — new files**
- `web/src/lib/tenant/tenantSlugFromHost.ts` — host→slug util.
- `web/src/lib/tenant/tenantSlugFromHost.test.ts`
- `web/src/app/register/page.tsx` — subdomain entry (replaces path route).

**Frontend — modified files**
- `web/src/components/students/StudentForm.tsx` — parametrize (`onSubmit`, `submitLabel`, `mode`).
- `web/src/app/api/auth/register/route.ts` — proxy reads slug from a body/header (replaces `[tenantSlug]/route.ts`).
- delete `web/src/app/register/[tenantSlug]/page.tsx` and `web/src/components/auth/RegistrationForm.tsx`.
- admin tenant create form — superadmin `selfRegistrationEnabled` toggle + copy-invite-link affordance (file located in Task 18).
- `web/messages/en.json` + `web/messages/es.json` — new strings.

---

# Phase 1 — Backend: single creation path + parity

## Task 1: Add identity-number uniqueness to the shared `StudentRepository`

**Files:**
- Modify: `api/src/main/java/com/klasio/student/domain/port/StudentRepository.java`
- Modify: `api/src/main/java/com/klasio/student/infrastructure/persistence/JpaStudentRepository.java`
- Test: `api/src/test/java/com/klasio/student/infrastructure/persistence/JpaStudentRepositoryIdentityTest.java` (or extend existing repo test if present)

- [ ] **Step 1: Write the failing test**

```java
package com.klasio.student.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.UUID;

class JpaStudentRepositoryIdentityTest {

    @Test
    void existsByIdentityNumberInTenant_delegatesToSpringData() {
        SpringDataStudentRepository springData = mock(SpringDataStudentRepository.class);
        UUID tenantId = UUID.randomUUID();
        when(springData.existsByTenantIdAndIdentityNumber(tenantId, "123")).thenReturn(true);

        JpaStudentRepository repo = new JpaStudentRepository(springData /* + any other ctor args */);

        assertThat(repo.existsByIdentityNumberInTenant(tenantId, "123")).isTrue();
    }
}
```

> If `JpaStudentRepository` needs an `EntityManager`/tenant-context dependency in its constructor, pass `mock(...)` for it. Mirror the existing constructor — check the top of `JpaStudentRepository.java`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=JpaStudentRepositoryIdentityTest`
Expected: FAIL — `existsByIdentityNumberInTenant` not defined on `StudentRepository`.

- [ ] **Step 3: Add the port method + impl**

In `StudentRepository.java`, add:
```java
    boolean existsByIdentityNumberInTenant(UUID tenantId, String identityNumber);
```

In `JpaStudentRepository.java`, add (mirror the existing `existsByEmailInTenant` which calls `applyTenantContext()`):
```java
    @Override
    public boolean existsByIdentityNumberInTenant(UUID tenantId, String identityNumber) {
        applyTenantContext();
        return springDataRepository.existsByTenantIdAndIdentityNumber(tenantId, identityNumber);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=JpaStudentRepositoryIdentityTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/student/domain/port/StudentRepository.java \
        api/src/main/java/com/klasio/student/infrastructure/persistence/JpaStudentRepository.java \
        api/src/test/java/com/klasio/student/infrastructure/persistence/JpaStudentRepositoryIdentityTest.java
git commit -m "feat(student): add identity-number uniqueness query to StudentRepository"
```

---

## Task 2: Enforce identity-number uniqueness in `CreateStudentService`

**Files:**
- Create: `api/src/main/java/com/klasio/shared/infrastructure/exception/StudentIdentityNumberAlreadyExistsException.java`
- Modify: `api/src/main/java/com/klasio/student/application/service/CreateStudentService.java`
- Test: `api/src/test/java/com/klasio/student/application/service/CreateStudentServiceTest.java` (extend or create)

- [ ] **Step 1: Write the failing test**

```java
@Test
void execute_rejectsDuplicateIdentityNumberInTenant() {
    UUID tenantId = UUID.randomUUID();
    when(studentRepository.existsByEmailInTenant(eq(tenantId), anyString())).thenReturn(false);
    when(studentRepository.existsByIdentityNumberInTenant(eq(tenantId), eq("CC-100"))).thenReturn(true);

    CreateStudentCommand cmd = sampleCommand(tenantId, "new@x.com", "CC-100"); // helper builds a valid command

    assertThatThrownBy(() -> service.execute(cmd))
        .isInstanceOf(StudentIdentityNumberAlreadyExistsException.class);
    verify(accountSetupCreationPort, never()).createAndDispatchSetup(any(), any(), any(), any(), any(), any(), any());
}
```

> Add a private `sampleCommand(UUID tenantId, String email, String identityNumber)` helper that returns a fully-populated `CreateStudentCommand` (use `BloodType.O_POSITIVE`, a past `dateOfBirth`, non-null phone). Reuse any existing helper in the test class.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=CreateStudentServiceTest#execute_rejectsDuplicateIdentityNumberInTenant`
Expected: FAIL — no identity check; `StudentIdentityNumberAlreadyExistsException` does not exist.

- [ ] **Step 3: Create the exception + add the check**

Create `StudentIdentityNumberAlreadyExistsException.java`:
```java
package com.klasio.shared.infrastructure.exception;

public class StudentIdentityNumberAlreadyExistsException extends RuntimeException {
    public StudentIdentityNumberAlreadyExistsException(String message) {
        super(message);
    }
}
```

In `CreateStudentService.execute`, immediately after the existing email check, add:
```java
        if (studentRepository.existsByIdentityNumberInTenant(command.tenantId(), command.identityNumber())) {
            throw new StudentIdentityNumberAlreadyExistsException(
                    "A student with identity number '%s' already exists in this tenant".formatted(command.identityNumber()));
        }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=CreateStudentServiceTest`
Expected: PASS (all methods, including pre-existing ones).

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/StudentIdentityNumberAlreadyExistsException.java \
        api/src/main/java/com/klasio/student/application/service/CreateStudentService.java \
        api/src/test/java/com/klasio/student/application/service/CreateStudentServiceTest.java
git commit -m "feat(student): enforce identity-number uniqueness per tenant on create"
```

---

## Task 3: Map the new conflict exception in `GlobalExceptionHandler` (admin path, explicit)

**Files:**
- Modify: `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`
- Test: `api/src/test/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandlerTest.java` (extend or create)

- [ ] **Step 1: Write the failing test**

```java
@Test
void handlesStudentIdentityNumberConflict_as409() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    var response = handler.handleStudentIdentityNumberConflict(
            new StudentIdentityNumberAlreadyExistsException("dup"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().error().code()).isEqualTo("STUDENT_IDENTITY_NUMBER_EXISTS");
}
```

> Confirm `ErrorResponse` accessor names (`error()`, `.code()`) against the existing file; adjust the asserts to match the record accessors used elsewhere in this test class.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest#handlesStudentIdentityNumberConflict_as409`
Expected: FAIL — handler method missing.

- [ ] **Step 3: Add the handler**

In `GlobalExceptionHandler.java` (mirror `handleProfessorEmailAlreadyExists`):
```java
    @ExceptionHandler(StudentIdentityNumberAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleStudentIdentityNumberConflict(StudentIdentityNumberAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("STUDENT_IDENTITY_NUMBER_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }
```

(If `StudentEmailAlreadyExistsException` is not already mapped, add the equivalent `STUDENT_EMAIL_EXISTS` handler the same way.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java \
        api/src/test/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandlerTest.java
git commit -m "feat(student): map identity-number conflict to 409"
```

---

# Phase 2 — Backend: tenant `selfRegistrationEnabled` flag

## Task 4: Flyway migration V072 — add column + audit action

**Files:**
- Create: `api/src/main/resources/db/migration/V072__add_self_registration_enabled_to_tenants.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V072: RF-40 — tenant self-registration flag (first tenant feature-flag)
-- Ownership note: `tenants` must be owned by klasio_app for this ALTER to succeed
-- (see CLAUDE.md "Flyway Migration Ownership Rule"). Verify before deploying to a
-- manually-reseeded DB.

ALTER TABLE tenants
    ADD COLUMN self_registration_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Extend the audit action constraint with the toggle action (keep the full V045 list).
ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS chk_audit_action_type;

ALTER TABLE audit_log
    ADD CONSTRAINT chk_audit_action_type CHECK (action_type IN (
        'TENANT_CREATED', 'TENANT_DEACTIVATED', 'TENANT_SELF_REGISTRATION_TOGGLED',
        'PROGRAM_CREATED', 'PROGRAM_UPDATED', 'PROGRAM_DEACTIVATED', 'PROGRAM_REACTIVATED',
        'PLAN_CREATED', 'PLAN_UPDATED', 'PLAN_DEACTIVATED', 'PLAN_REACTIVATED',
        'PROFESSOR_CREATED', 'PROFESSOR_UPDATED', 'PROFESSOR_DEACTIVATED', 'PROFESSOR_REACTIVATED',
        'CLASS_CREATED', 'CLASS_UPDATED', 'CLASS_DEACTIVATED', 'CLASS_REACTIVATED',
        'CLASS_PROFESSOR_ASSIGNED', 'CLASS_PROFESSOR_REMOVED',
        'STUDENT_CREATED', 'STUDENT_UPDATED', 'STUDENT_DEACTIVATED', 'STUDENT_REACTIVATED',
        'STUDENT_ENROLLED', 'STUDENT_UNENROLLED', 'STUDENT_PROMOTED', 'STUDENT_SELF_REGISTERED',
        'MEMBERSHIP_CREATED', 'MEMBERSHIP_PROOF_UPLOADED', 'MEMBERSHIP_PAYMENT_VALIDATED',
        'MEMBERSHIP_ACTIVATED', 'MEMBERSHIP_PENDING_MANAGER_ACTIVATION', 'MEMBERSHIP_DEPLETED',
        'MEMBERSHIP_EXPIRED', 'MEMBERSHIP_HOUR_ADJUSTED', 'MEMBERSHIP_EXPIRY_WARNING', 'MEMBERSHIP_RENEWED',
        'AUTH_LOGIN', 'AUTH_LOGIN_FAILED', 'AUTH_LOGOUT', 'AUTH_ACCOUNT_LOCKED', 'AUTH_ACCOUNT_UNLOCKED',
        'AUTH_EMAIL_VERIFIED', 'AUTH_VERIFICATION_RESENT', 'AUTH_PASSWORD_RESET_REQUESTED',
        'AUTH_PASSWORD_RESET_COMPLETED',
        'ROLE_ASSIGNED',
        'PAYMENT_PROOF_UPLOADED', 'PAYMENT_PROOF_APPROVED', 'PAYMENT_PROOF_REJECTED',
        'MEMBERSHIP_ACTIVATION_DELEGATED', 'DELEGATION_REMINDER_SENT'
    ));
```

> ⚠️ Before finalizing, diff this `IN (...)` list against the **latest** constraint actually in the DB. Search `db/migration` for the most recent file that re-declares `chk_audit_action_type` (currently V045, but V047/V068 may add more) and include every action already present, plus `TENANT_SELF_REGISTRATION_TOGGLED`. Dropping an existing action would break inserts.

- [ ] **Step 2: Verify it applies**

Run (Flyway runs on app boot): `./mvnw -Dtest=FlywayMigrationSmokeTest test` if such a test exists; otherwise start the app once (`./mvnw spring-boot:run`) and confirm no migration error, then stop.
Expected: migration applies; `tenants.self_registration_enabled` exists, default true.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/resources/db/migration/V072__add_self_registration_enabled_to_tenants.sql
git commit -m "feat(tenant): add self_registration_enabled column + audit action (V072)"
```

---

## Task 5: Add `selfRegistrationEnabled` to the `Tenant` aggregate

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/domain/model/Tenant.java`
- Create: `api/src/main/java/com/klasio/tenant/domain/event/TenantSelfRegistrationToggled.java`
- Test: `api/src/test/java/com/klasio/tenant/domain/model/TenantSelfRegistrationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.klasio.tenant.domain.model;

import com.klasio.tenant.domain.event.TenantSelfRegistrationToggled;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TenantSelfRegistrationTest {

    @Test
    void create_defaultsSelfRegistrationEnabledTrue() {
        Tenant t = sampleTenant(true);
        assertThat(t.isSelfRegistrationEnabled()).isTrue();
    }

    @Test
    void disable_thenEnable_flipsFlagAndEmitsEvent() {
        Tenant t = sampleTenant(true);
        t.clearDomainEvents();
        UUID actor = UUID.randomUUID();

        t.setSelfRegistration(false, actor);
        assertThat(t.isSelfRegistrationEnabled()).isFalse();
        assertThat(t.getDomainEvents()).hasSize(1)
            .first().isInstanceOf(TenantSelfRegistrationToggled.class);
    }

    @Test
    void setSelfRegistration_noOpWhenUnchanged_emitsNoEvent() {
        Tenant t = sampleTenant(true);
        t.clearDomainEvents();
        t.setSelfRegistration(true, UUID.randomUUID());
        assertThat(t.getDomainEvents()).isEmpty();
    }

    private Tenant sampleTenant(boolean selfReg) {
        return Tenant.create("League", "Football", "en", "America/Bogota",
                new TenantSlug("league"),
                new ContactInfo("a@b.com", "3000000000", "+57", "St 1", "City", "State", "CO"),
                UUID.randomUUID(), null, selfReg);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TenantSelfRegistrationTest`
Expected: FAIL — `create` has no `selfReg` param; `isSelfRegistrationEnabled`/`setSelfRegistration`/event missing.

- [ ] **Step 3: Implement**

Create `TenantSelfRegistrationToggled.java`:
```java
package com.klasio.tenant.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TenantSelfRegistrationToggled(
        UUID tenantId, String slug, boolean enabled, UUID actorId, Instant occurredAt
) implements DomainEvent {}
```

> Match `DomainEvent`'s required methods — check `TenantDeactivated` for whether it implements `occurredOn()`/`eventType()` and mirror exactly.

In `Tenant.java`:
- add field: `private boolean selfRegistrationEnabled;`
- add it as the **last** constructor parameter and assign it.
- change `create(...)` signature to append `boolean selfRegistrationEnabled`, pass it through, and pass it to the constructor.
- change `reconstitute(...)` to append `boolean selfRegistrationEnabled` and pass through.
- add:
```java
    public boolean isSelfRegistrationEnabled() { return selfRegistrationEnabled; }

    public void setSelfRegistration(boolean enabled, UUID actorId) {
        if (this.selfRegistrationEnabled == enabled) {
            return; // idempotent — no event
        }
        this.selfRegistrationEnabled = enabled;
        domainEvents.add(new TenantSelfRegistrationToggled(
                id.value(), slug.value(), enabled, actorId, Instant.now()));
    }
```

> Updating `create`/`reconstitute` signatures will break existing callers (`CreateTenantService`, `TenantMapper`, existing tenant tests). Fix them in Tasks 6–7; for now the module won't compile until those are done — that's expected. Run only this test class's compile via the test command; if the module fails to compile, proceed to Tasks 6–7 then re-run.

- [ ] **Step 4: Run test to verify it passes** (after Tasks 6–7 fix callers)

Run: `./mvnw test -Dtest=TenantSelfRegistrationTest`
Expected: PASS

- [ ] **Step 5: Commit** (commit together with Tasks 6–7 if the module only compiles once callers are fixed)

```bash
git add api/src/main/java/com/klasio/tenant/domain/event/TenantSelfRegistrationToggled.java \
        api/src/main/java/com/klasio/tenant/domain/model/Tenant.java \
        api/src/test/java/com/klasio/tenant/domain/model/TenantSelfRegistrationTest.java
git commit -m "feat(tenant): add selfRegistrationEnabled to Tenant aggregate"
```

---

## Task 6: Persist the flag — `TenantJpaEntity` + `TenantMapper`

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantJpaEntity.java`
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantMapper.java`
- Test: `api/src/test/java/com/klasio/tenant/infrastructure/persistence/TenantMapperTest.java` (extend or create)

- [ ] **Step 1: Write the failing test**

```java
@Test
void roundTrip_preservesSelfRegistrationEnabled() {
    Tenant domain = Tenant.create("L", "F", "en", "America/Bogota",
            new TenantSlug("l"),
            new ContactInfo("a@b.com", "3000000000", "+57", "S", "C", "St", "CO"),
            UUID.randomUUID(), null, false);

    TenantJpaEntity entity = TenantMapper.toEntity(domain);   // match the mapper's actual method names
    assertThat(entity.isSelfRegistrationEnabled()).isFalse();

    Tenant back = TenantMapper.toDomain(entity);
    assertThat(back.isSelfRegistrationEnabled()).isFalse();
}
```

> Use the mapper's real method names (`toEntity`/`toDomain` or `toJpa`/`toModel`) — check `TenantMapper.java`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TenantMapperTest#roundTrip_preservesSelfRegistrationEnabled`
Expected: FAIL — entity field + mapping missing.

- [ ] **Step 3: Implement**

In `TenantJpaEntity.java` add:
```java
    @Column(name = "self_registration_enabled", nullable = false)
    private boolean selfRegistrationEnabled = true;
```
plus getter/setter (match the class's accessor style — explicit getters or Lombok).

In `TenantMapper.java`:
- `toEntity`: `entity.setSelfRegistrationEnabled(tenant.isSelfRegistrationEnabled());`
- `toDomain`: pass `entity.isSelfRegistrationEnabled()` as the new last argument to `Tenant.reconstitute(...)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TenantMapperTest`
Expected: PASS

- [ ] **Step 5: Commit** (see Task 5 note about combining)

```bash
git add api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantJpaEntity.java \
        api/src/main/java/com/klasio/tenant/infrastructure/persistence/TenantMapper.java \
        api/src/test/java/com/klasio/tenant/infrastructure/persistence/TenantMapperTest.java
git commit -m "feat(tenant): persist selfRegistrationEnabled (entity + mapper)"
```

---

## Task 7: Default the flag in `CreateTenantService` + create command

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/application/dto/CreateTenantCommand.java`
- Modify: `api/src/main/java/com/klasio/tenant/application/service/CreateTenantService.java`
- Test: `api/src/test/java/com/klasio/tenant/application/service/CreateTenantServiceTest.java` (extend or create)

- [ ] **Step 1: Write the failing test**

```java
@Test
void execute_passesSelfRegistrationFlagToTenant() {
    // arrange a command with selfRegistrationEnabled=false; stub repo.existsBySlug=false, save=capture
    ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
    // ... build command (use existing test builder) with selfRegistrationEnabled=false
    service.execute(command);
    verify(tenantRepository).save(captor.capture());
    assertThat(captor.getValue().isSelfRegistrationEnabled()).isFalse();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=CreateTenantServiceTest#execute_passesSelfRegistrationFlagToTenant`
Expected: FAIL — command has no flag; `Tenant.create` arity mismatch.

- [ ] **Step 3: Implement**

In `CreateTenantCommand.java` add a field **before** `createdBy`:
```java
        boolean selfRegistrationEnabled,
```
In `CreateTenantService.execute`, pass it as the new last argument to `Tenant.create(...)`:
```java
            Tenant tenant = Tenant.create(
                    command.name(), command.discipline(), command.language(), command.timezone(),
                    slug, contactInfo, command.createdBy(), logoKey,
                    command.selfRegistrationEnabled());
```
Update the controller that builds `CreateTenantCommand` (`TenantController`) to read the flag from the create request, **defaulting to `true`** when absent:
```java
            request.selfRegistrationEnabled() == null ? true : request.selfRegistrationEnabled(),
```
(add `Boolean selfRegistrationEnabled` to the create request record).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=CreateTenantServiceTest`
Expected: PASS. Also run the whole tenant module: `./mvnw test -Dtest='com.klasio.tenant.**'` — confirm Tasks 5–7 compile together.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/application/dto/CreateTenantCommand.java \
        api/src/main/java/com/klasio/tenant/application/service/CreateTenantService.java \
        api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java \
        api/src/test/java/com/klasio/tenant/application/service/CreateTenantServiceTest.java
git commit -m "feat(tenant): accept selfRegistrationEnabled at creation (default true)"
```

---

# Phase 3 — Backend: superadmin toggle endpoint + audit

## Task 8: `ToggleSelfRegistrationService` (SUPERADMIN)

**Files:**
- Create: `api/src/main/java/com/klasio/tenant/application/dto/ToggleSelfRegistrationCommand.java`
- Create: `api/src/main/java/com/klasio/tenant/application/port/input/ToggleSelfRegistrationUseCase.java`
- Create: `api/src/main/java/com/klasio/tenant/application/service/ToggleSelfRegistrationService.java`
- Test: `api/src/test/java/com/klasio/tenant/application/service/ToggleSelfRegistrationServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void execute_loadsTogglesSavesAndPublishes() {
    UUID tenantId = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    Tenant tenant = spy(/* a Tenant.create(...) with selfReg=true */);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    service.execute(new ToggleSelfRegistrationCommand(tenantId, false, actor));

    assertThat(tenant.isSelfRegistrationEnabled()).isFalse();
    verify(tenantRepository).save(tenant);
    verify(eventPublisher).publishEvent(any(TenantSelfRegistrationToggled.class));
}

@Test
void execute_throwsWhenTenantMissing() {
    UUID tenantId = UUID.randomUUID();
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.execute(new ToggleSelfRegistrationCommand(tenantId, false, UUID.randomUUID())))
        .isInstanceOf(TenantNotFoundException.class);
}
```

> Confirm `TenantRepository` exposes `findById(UUID)`; if it returns a domain `Tenant` directly check the signature and adjust. Use the existing `TenantNotFoundException`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ToggleSelfRegistrationServiceTest`
Expected: FAIL — classes don't exist.

- [ ] **Step 3: Implement**

`ToggleSelfRegistrationCommand.java`:
```java
package com.klasio.tenant.application.dto;
import java.util.UUID;
public record ToggleSelfRegistrationCommand(UUID tenantId, boolean enabled, UUID actorId) {}
```

`ToggleSelfRegistrationUseCase.java`:
```java
package com.klasio.tenant.application.port.input;
import com.klasio.tenant.application.dto.ToggleSelfRegistrationCommand;
public interface ToggleSelfRegistrationUseCase {
    void execute(ToggleSelfRegistrationCommand command);
}
```

`ToggleSelfRegistrationService.java` (mirror `CreateTenantService`'s event-publish pattern: copy events, save, clear, publish):
```java
package com.klasio.tenant.application.service;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.ToggleSelfRegistrationCommand;
import com.klasio.tenant.application.port.input.ToggleSelfRegistrationUseCase;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantId;
import com.klasio.tenant.domain.port.TenantRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ToggleSelfRegistrationService implements ToggleSelfRegistrationUseCase {

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ToggleSelfRegistrationService(TenantRepository tenantRepository,
                                         ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(ToggleSelfRegistrationCommand command) {
        Tenant tenant = tenantRepository.findById(TenantId.of(command.tenantId()))
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant '%s' not found".formatted(command.tenantId())));

        tenant.setSelfRegistration(command.enabled(), command.actorId());

        List<DomainEvent> events = List.copyOf(tenant.getDomainEvents());
        tenantRepository.save(tenant);
        tenant.clearDomainEvents();
        events.forEach(eventPublisher::publishEvent);
    }
}
```

> Adjust `tenantRepository.findById(...)` to the real signature (it may take a raw `UUID` or `TenantId`; `TenantId.of(...)` may be `new TenantId(...)`). Check `TenantRepository` + `TenantId`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ToggleSelfRegistrationServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/application/dto/ToggleSelfRegistrationCommand.java \
        api/src/main/java/com/klasio/tenant/application/port/input/ToggleSelfRegistrationUseCase.java \
        api/src/main/java/com/klasio/tenant/application/service/ToggleSelfRegistrationService.java \
        api/src/test/java/com/klasio/tenant/application/service/ToggleSelfRegistrationServiceTest.java
git commit -m "feat(tenant): add self-registration toggle use case"
```

---

## Task 9: Audit listener for the toggle

**Files:**
- Modify: `api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java`
- Test: extend the existing audit listener test if present; else add `AuditEventListenerSelfRegToggleTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void onTenantSelfRegistrationToggled_writesAuditRow() {
    // arrange the same collaborators the existing onTenantCreated test uses (audit repo mock/captor)
    UUID tenantId = UUID.randomUUID();
    listener.onTenantSelfRegistrationToggled(new TenantSelfRegistrationToggled(
            tenantId, "league", false, UUID.randomUUID(), Instant.now()));
    // verify an audit entry with actionType "TENANT_SELF_REGISTRATION_TOGGLED" is saved
    verify(auditRepository).save(argThat(e -> "TENANT_SELF_REGISTRATION_TOGGLED".equals(e.getActionType())));
}
```

> Copy the arrange/verify shape from the existing `onTenantCreated` test (collaborator names, captor vs argThat). The audit-entry accessor (`getActionType()`/`actionType()`) must match the audit entity in this codebase.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=<AuditTestClass>#onTenantSelfRegistrationToggled_writesAuditRow`
Expected: FAIL — handler missing.

- [ ] **Step 3: Implement** — mirror `onTenantCreated` (lines ~84–106):

```java
    @EventListener
    public void onTenantSelfRegistrationToggled(TenantSelfRegistrationToggled event) {
        log.info("Recording audit log for tenant self-registration toggle: tenantId={}, enabled={}",
                event.tenantId(), event.enabled());
        // Build and save the audit entry exactly like onTenantCreated, with:
        //   actionType = "TENANT_SELF_REGISTRATION_TOGGLED"
        //   tenantId   = event.tenantId()
        //   actorId    = event.actorId()
        //   details/metadata = "{\"enabled\":" + event.enabled() + "}"
    }
```

Fill the body with the same construction call `onTenantCreated` uses (e.g. `auditRepository.save(new AuditEntry(...))` or the builder used there).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=<AuditTestClass>`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/audit/infrastructure/persistence/AuditEventListener.java \
        api/src/test/java/com/klasio/audit/**/<AuditTestClass>.java
git commit -m "feat(audit): record TENANT_SELF_REGISTRATION_TOGGLED"
```

---

## Task 10: Toggle endpoint on `TenantController` (SUPERADMIN only)

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java`
- Test: `api/src/test/java/com/klasio/tenant/infrastructure/web/TenantControllerSelfRegTest.java` (slice test) or extend existing controller test

- [ ] **Step 1: Write the failing test** (MockMvc, mirror existing tenant controller tests / security setup)

```java
@Test
@WithMockUser(roles = "SUPERADMIN")
void toggleSelfRegistration_returns204_andCallsUseCase() throws Exception {
    UUID tenantId = UUID.randomUUID();
    mockMvc.perform(patch("/api/v1/tenants/{id}/self-registration", tenantId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"enabled\":false}"))
        .andExpect(status().isNoContent());
    verify(toggleSelfRegistrationUseCase).execute(any(ToggleSelfRegistrationCommand.class));
}

@Test
@WithMockUser(roles = "ADMIN")
void toggleSelfRegistration_forbiddenForAdmin() throws Exception {
    mockMvc.perform(patch("/api/v1/tenants/{id}/self-registration", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
        .andExpect(status().isForbidden());
}
```

> Match how other superadmin-only tenant endpoints declare authorization (method-level `@PreAuthorize("hasRole('SUPERADMIN')")` vs `SecurityConfig` rules). Replicate that exact mechanism so the ADMIN-forbidden test passes.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TenantControllerSelfRegTest`
Expected: FAIL — endpoint missing.

- [ ] **Step 3: Implement** — inject `ToggleSelfRegistrationUseCase`; add:

```java
    public record SelfRegistrationToggleRequest(@NotNull Boolean enabled) {}

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PatchMapping("/{tenantId}/self-registration")
    public ResponseEntity<Void> toggleSelfRegistration(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SelfRegistrationToggleRequest request,
            Authentication authentication) {
        UUID actorId = currentUserId(authentication); // reuse the controller's existing actor-extraction helper
        toggleSelfRegistrationUseCase.execute(
                new ToggleSelfRegistrationCommand(tenantId, request.enabled(), actorId));
        return ResponseEntity.noContent().build();
    }
```

> Use the controller's existing pattern for extracting the actor `UUID` from `Authentication` (the JWT `userId` claim via `(Map<String,Object>) auth.getDetails()` — see CLAUDE.md). If no helper exists, extract inline the same way other endpoints in this controller do.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TenantControllerSelfRegTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/infrastructure/web/TenantController.java \
        api/src/test/java/com/klasio/tenant/infrastructure/web/TenantControllerSelfRegTest.java
git commit -m "feat(tenant): superadmin endpoint to toggle self-registration"
```

---

# Phase 4 — Backend: wire register → shared path + flag guard

## Task 11: Add `isSelfRegistrationEnabled` to `TenantResolverPort` + adapter

**Files:**
- Modify: `api/src/main/java/com/klasio/auth/application/port/TenantResolverPort.java`
- Modify: the adapter implementing it (find with `grep -rl "implements TenantResolverPort" api/src/main/java`)
- Test: adapter test if present; else covered by Task 12's service test via mock.

- [ ] **Step 1: Add the port method**

```java
    /** True only when the tenant exists, is ACTIVE, and self-registration is enabled. */
    boolean isSelfRegistrationEnabled(UUID tenantId);
```

- [ ] **Step 2: Implement in the adapter**

Read the tenant via the existing tenant repository/query the adapter already uses, and return:
```java
    @Override
    public boolean isSelfRegistrationEnabled(UUID tenantId) {
        return tenantRepository.findById(new TenantId(tenantId))
                .map(t -> t.getStatus() == TenantStatus.ACTIVE && t.isSelfRegistrationEnabled())
                .orElse(false);
    }
```

> Adapt to the adapter's available collaborators (it may hold a `SpringDataTenantRepository` returning `TenantJpaEntity` — then check `entity.getStatus()` + `entity.isSelfRegistrationEnabled()` directly). Keep it a single read.

- [ ] **Step 3: Build to verify it compiles**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/auth/application/port/TenantResolverPort.java \
        api/src/main/java/com/klasio/auth/infrastructure/adapter/<TenantResolverAdapter>.java
git commit -m "feat(auth): expose tenant self-registration enablement via resolver port"
```

---

## Task 12: Rewrite `RegisterStudentService` to delegate + guard the flag

**Files:**
- Modify: `api/src/main/java/com/klasio/auth/application/dto/RegisterStudentCommand.java`
- Create: `api/src/main/java/com/klasio/auth/domain/exception/SelfRegistrationDisabledException.java`
- Create: `api/src/main/java/com/klasio/auth/domain/exception/SelfRegistrationConflictException.java`
- Modify: `api/src/main/java/com/klasio/auth/application/service/RegisterStudentService.java`
- Test: `api/src/test/java/com/klasio/auth/application/service/RegisterStudentServiceTest.java` (rewrite)

- [ ] **Step 1: Write the failing tests**

```java
@ExtendWith(MockitoExtension.class)
class RegisterStudentServiceTest {

    @Mock TenantResolverPort tenantResolverPort;
    @Mock CreateStudentUseCase createStudentUseCase;
    @InjectMocks RegisterStudentService service;

    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private RegisterStudentCommand cmd() {
        return new RegisterStudentCommand("acme", "Ana", "Diaz",
                LocalDate.of(2000, 1, 1), "CC", "CC-1", "EPS", "ana@x.com",
                "A+", "3000000000",
                null, null, null, null, null); // adult → no tutor
    }

    @Test
    void register_delegatesToCreateStudentWithFullFields() {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolverPort.resolveTenantIdBySlug("acme")).thenReturn(Optional.of(tenantId));
        when(tenantResolverPort.isSelfRegistrationEnabled(tenantId)).thenReturn(true);

        service.register(cmd());

        ArgumentCaptor<CreateStudentCommand> captor = ArgumentCaptor.forClass(CreateStudentCommand.class);
        verify(createStudentUseCase).execute(captor.capture());
        CreateStudentCommand c = captor.getValue();
        assertThat(c.tenantId()).isEqualTo(tenantId);
        assertThat(c.phone()).isEqualTo("3000000000");
        assertThat(c.bloodType()).isEqualTo(BloodType.A_POSITIVE);
        assertThat(c.createdBy()).isEqualTo(SYSTEM_ACTOR);
        assertThat(c.identityDocumentType()).isEqualTo(IdentityDocumentType.CC);
    }

    @Test
    void register_throwsTenantNotFound_whenSlugUnknown() {
        when(tenantResolverPort.resolveTenantIdBySlug("acme")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.register(cmd()))
            .isInstanceOf(TenantNotFoundException.class);
        verifyNoInteractions(createStudentUseCase);
    }

    @Test
    void register_throwsDisabled_whenFlagOff() {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolverPort.resolveTenantIdBySlug("acme")).thenReturn(Optional.of(tenantId));
        when(tenantResolverPort.isSelfRegistrationEnabled(tenantId)).thenReturn(false);
        assertThatThrownBy(() -> service.register(cmd()))
            .isInstanceOf(SelfRegistrationDisabledException.class);
        verifyNoInteractions(createStudentUseCase);
    }

    @Test
    void register_mapsEmailConflictToGenericNonEnumeratingError() {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolverPort.resolveTenantIdBySlug("acme")).thenReturn(Optional.of(tenantId));
        when(tenantResolverPort.isSelfRegistrationEnabled(tenantId)).thenReturn(true);
        doThrow(new StudentEmailAlreadyExistsException("dup"))
            .when(createStudentUseCase).execute(any());
        assertThatThrownBy(() -> service.register(cmd()))
            .isInstanceOf(SelfRegistrationConflictException.class);
    }

    @Test
    void register_mapsIdentityConflictToGenericNonEnumeratingError() {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolverPort.resolveTenantIdBySlug("acme")).thenReturn(Optional.of(tenantId));
        when(tenantResolverPort.isSelfRegistrationEnabled(tenantId)).thenReturn(true);
        doThrow(new StudentIdentityNumberAlreadyExistsException("dup"))
            .when(createStudentUseCase).execute(any());
        assertThatThrownBy(() -> service.register(cmd()))
            .isInstanceOf(SelfRegistrationConflictException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=RegisterStudentServiceTest`
Expected: FAIL — command arity, new exceptions, new dependencies all missing.

- [ ] **Step 3: Implement**

`RegisterStudentCommand.java` (full structured set, parse-friendly types):
```java
package com.klasio.auth.application.dto;

import java.time.LocalDate;

public record RegisterStudentCommand(
        String tenantSlug,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String identityDocumentType,
        String identityNumber,
        String eps,
        String email,
        String bloodType,          // label e.g. "A+" or null
        String phone,
        String tutorFirstName,
        String tutorLastName,
        String tutorRelationship,
        String tutorPhone,
        String tutorEmail
) {}
```

`SelfRegistrationDisabledException.java`:
```java
package com.klasio.auth.domain.exception;
public class SelfRegistrationDisabledException extends RuntimeException {
    public SelfRegistrationDisabledException() {
        super("Self-registration is not available for this league");
    }
}
```

`SelfRegistrationConflictException.java`:
```java
package com.klasio.auth.domain.exception;
public class SelfRegistrationConflictException extends RuntimeException {
    public SelfRegistrationConflictException() {
        super("Registration could not be completed");
    }
}
```

`RegisterStudentService.java` (replace the whole class body):
```java
package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.exception.SelfRegistrationConflictException;
import com.klasio.auth.domain.exception.SelfRegistrationDisabledException;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentIdentityNumberAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.domain.model.BloodType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterStudentService {

    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final TenantResolverPort tenantResolverPort;
    private final CreateStudentUseCase createStudentUseCase;

    public RegisterStudentService(TenantResolverPort tenantResolverPort,
                                  CreateStudentUseCase createStudentUseCase) {
        this.tenantResolverPort = tenantResolverPort;
        this.createStudentUseCase = createStudentUseCase;
    }

    @Transactional
    public void register(RegisterStudentCommand command) {
        UUID tenantId = tenantResolverPort.resolveTenantIdBySlug(command.tenantSlug())
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant '%s' not found".formatted(command.tenantSlug())));

        if (!tenantResolverPort.isSelfRegistrationEnabled(tenantId)) {
            throw new SelfRegistrationDisabledException();
        }

        CreateStudentCommand createCommand = new CreateStudentCommand(
                tenantId,
                command.firstName(),
                command.lastName(),
                command.email(),
                command.dateOfBirth(),
                command.eps(),
                command.identityNumber(),
                IdentityDocumentType.valueOf(command.identityDocumentType()),
                parseBloodType(command.bloodType()),
                command.phone(),
                command.tutorFirstName(),
                command.tutorLastName(),
                command.tutorRelationship(),
                command.tutorPhone(),
                command.tutorEmail(),
                SYSTEM_ACTOR
        );

        try {
            createStudentUseCase.execute(createCommand);
        } catch (StudentEmailAlreadyExistsException | StudentIdentityNumberAlreadyExistsException ex) {
            // Non-enumerating: never disclose which field collided or that an account exists.
            throw new SelfRegistrationConflictException();
        }
    }

    private BloodType parseBloodType(String label) {
        if (label == null || label.isBlank()) return null;
        return BloodType.fromLabel(label);
    }
}
```

> `CreateStudentService` is `@Transactional` and `RegisterStudentService.register` is too; the nested call joins the same transaction. `accountSetupCreationPort` requires MANDATORY propagation — satisfied. Delete now-unused imports/fields.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -Dtest=RegisterStudentServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/auth/application/dto/RegisterStudentCommand.java \
        api/src/main/java/com/klasio/auth/domain/exception/SelfRegistrationDisabledException.java \
        api/src/main/java/com/klasio/auth/domain/exception/SelfRegistrationConflictException.java \
        api/src/main/java/com/klasio/auth/application/service/RegisterStudentService.java \
        api/src/test/java/com/klasio/auth/application/service/RegisterStudentServiceTest.java
git commit -m "feat(auth): delegate self-registration to CreateStudentUseCase with flag guard"
```

---

## Task 13: Expand `RegistrationController` request + map new exceptions

**Files:**
- Modify: `api/src/main/java/com/klasio/auth/infrastructure/web/RegistrationController.java`
- Modify: `api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java`
- Test: `api/src/test/java/com/klasio/auth/infrastructure/web/RegistrationControllerTest.java` (rewrite/extend)

- [ ] **Step 1: Write the failing tests** (MockMvc — endpoint is public, mirror existing test setup)

```java
@Test
void register_accepts202_withFullStructuredBody() throws Exception {
    mockMvc.perform(post("/api/v1/tenants/{slug}/register", "acme")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {"firstName":"Ana","lastName":"Diaz","dateOfBirth":"2000-01-01",
               "identityDocumentType":"CC","identityNumber":"CC-1","eps":"EPS",
               "email":"ana@x.com","bloodType":"A+","phone":"3000000000"}
            """))
        .andExpect(status().isAccepted());
    verify(registerStudentService).register(any(RegisterStudentCommand.class));
}

@Test
void register_returns403_whenDisabled() throws Exception {
    doThrow(new SelfRegistrationDisabledException()).when(registerStudentService).register(any());
    mockMvc.perform(post("/api/v1/tenants/{slug}/register", "acme")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {"firstName":"Ana","lastName":"Diaz","dateOfBirth":"2000-01-01",
               "identityDocumentType":"CC","identityNumber":"CC-1","eps":"EPS","email":"a@x.com"}
            """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("SELF_REGISTRATION_DISABLED"));
}

@Test
void register_returns409Generic_onConflict() throws Exception {
    doThrow(new SelfRegistrationConflictException()).when(registerStudentService).register(any());
    mockMvc.perform(post("/api/v1/tenants/{slug}/register", "acme")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {"firstName":"Ana","lastName":"Diaz","dateOfBirth":"2000-01-01",
               "identityDocumentType":"CC","identityNumber":"CC-1","eps":"EPS","email":"a@x.com"}
            """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("REGISTRATION_FAILED"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=RegistrationControllerTest`
Expected: FAIL — request DTO lacks bloodType/phone/structured tutor; exception mappings missing.

- [ ] **Step 3: Implement**

In `RegistrationController.java`, replace the `RegistrationRequest` record + command mapping:
```java
    public record RegistrationRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull LocalDate dateOfBirth,
            @NotBlank @Pattern(regexp = "^(CC|TI|CE|PA|RC)$", message = "Invalid document type") String identityDocumentType,
            @NotBlank @Size(max = 30) String identityNumber,
            @NotBlank String eps,
            @NotBlank @Email String email,
            String bloodType,
            String phone,
            String tutorFirstName,
            String tutorLastName,
            String tutorRelationship,
            String tutorPhone,
            @Email String tutorEmail
    ) {}

    @PostMapping("/{tenantSlug}/register")
    public ResponseEntity<Map<String, String>> register(
            @PathVariable String tenantSlug,
            @Valid @RequestBody RegistrationRequest request) {

        RegisterStudentCommand command = new RegisterStudentCommand(
                tenantSlug, request.firstName(), request.lastName(), request.dateOfBirth(),
                request.identityDocumentType(), request.identityNumber(), request.eps(), request.email(),
                request.bloodType(), request.phone(),
                request.tutorFirstName(), request.tutorLastName(), request.tutorRelationship(),
                request.tutorPhone(), request.tutorEmail());

        registerStudentService.register(command);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Registration successful. Please check your email to set your password and activate your account."));
    }
```

In `GlobalExceptionHandler.java` add:
```java
    @ExceptionHandler(SelfRegistrationDisabledException.class)
    public ResponseEntity<ErrorResponse> handleSelfRegDisabled(SelfRegistrationDisabledException ex) {
        var error = new ErrorResponse.ErrorDetail("SELF_REGISTRATION_DISABLED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error));
    }

    @ExceptionHandler(SelfRegistrationConflictException.class)
    public ResponseEntity<ErrorResponse> handleSelfRegConflict(SelfRegistrationConflictException ex) {
        var error = new ErrorResponse.ErrorDetail("REGISTRATION_FAILED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }
```
(Add the imports for the two `com.klasio.auth.domain.exception` classes.)

> Server-side minor/tutor enforcement: the `Student.create()` aggregate already validates the minor→tutor invariant. If it does **not**, add that validation in the aggregate (separate concern) — confirm by checking `Student.create`. Do not duplicate it in the controller.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw test -Dtest=RegistrationControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/auth/infrastructure/web/RegistrationController.java \
        api/src/main/java/com/klasio/shared/infrastructure/exception/GlobalExceptionHandler.java \
        api/src/test/java/com/klasio/auth/infrastructure/web/RegistrationControllerTest.java
git commit -m "feat(auth): full structured self-registration request + error mapping"
```

---

## Task 14: Remove the divergent `StudentProfilePort.createStudentProfile`

**Files:**
- Modify: `api/src/main/java/com/klasio/auth/application/port/StudentProfilePort.java`
- Modify: `api/src/main/java/com/klasio/auth/infrastructure/adapter/StudentProfileAdapter.java`

- [ ] **Step 1: Delete the dead method**

`RegisterStudentService` no longer calls `createStudentProfile`. Remove it from the port and the adapter. Keep `existsByIdentityNumberInTenant` on the port **only if** another caller still uses it (grep first):

```bash
grep -rn "createStudentProfile\|StudentProfilePort" api/src/main/java
```
If `StudentProfilePort` has no remaining methods/callers, delete the port + adapter entirely. Otherwise delete only `createStudentProfile`.

- [ ] **Step 2: Build to verify nothing references it**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add -A api/src/main/java/com/klasio/auth/application/port/ api/src/main/java/com/klasio/auth/infrastructure/adapter/
git commit -m "refactor(auth): drop divergent createStudentProfile path"
```

---

## Task 15: Backend integration test — end-to-end self-registration

**Files:**
- Create: `api/src/test/java/com/klasio/auth/RegisterStudentIntegrationTest.java` (mirror existing `@SpringBootTest` IT patterns + Testcontainers/H2 setup used in the repo)

- [ ] **Step 1: Write the failing IT**

```java
// @SpringBootTest, real RegisterStudentService + CreateStudentService wiring, in-memory email transport.
@Test
void selfRegistration_createsStudentAndDispatchesAccountSetupEmail() {
    // given a seeded ACTIVE tenant "acme" with self_registration_enabled = true
    // when POST /api/v1/tenants/acme/register with a full adult body
    // then:
    //  - a Student row exists with phone + bloodType + structured tutor (null for adult)
    //  - a User exists in pending-setup state (passwordHash null)
    //  - exactly one account-setup email captured by InMemoryEmailTransport (EmailType account-setup)
    //  - no membership exists for the student
}

@Test
void selfRegistration_rejectedWhenFlagDisabled() {
    // tenant with self_registration_enabled=false → 403 SELF_REGISTRATION_DISABLED, no Student created
}

@Test
void selfRegistration_duplicateEmail_returnsGeneric409_noEnumeration() {
    // pre-create a student with the email, then register again → 409 REGISTRATION_FAILED, body has no field-level detail
}
```

> Use the repo's existing IT scaffolding (seed helpers, `InMemoryEmailTransport` assertion API, tenant seeding). Look at an existing `*IntegrationTest` in `auth`/`membership` for the exact annotations and DB strategy.

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw test -Dtest=RegisterStudentIntegrationTest`
Expected: FAIL initially (assertions unimplemented) → then implement the test bodies fully (no stubs) and make them pass against the real wiring.

- [ ] **Step 3: Make it pass** — fill in the test bodies with concrete arrange/act/assert using the repo helpers. No production code change expected; if a wiring gap surfaces, fix it.

- [ ] **Step 4: Run to verify it passes**

Run: `./mvnw test -Dtest=RegisterStudentIntegrationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add api/src/test/java/com/klasio/auth/RegisterStudentIntegrationTest.java
git commit -m "test(auth): integration coverage for self-registration parity"
```

---

# Phase 5 — Frontend: subdomain entry + form parity

## Task 16: `tenantSlugFromHost` util

**Files:**
- Create: `web/src/lib/tenant/tenantSlugFromHost.ts`
- Test: `web/src/lib/tenant/tenantSlugFromHost.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { tenantSlugFromHost } from "./tenantSlugFromHost";

describe("tenantSlugFromHost", () => {
  const ROOT = "klasio.app";
  it("extracts subdomain from prod host", () => {
    expect(tenantSlugFromHost("acme.klasio.app", ROOT)).toBe("acme");
  });
  it("extracts subdomain with port (dev *.localhost)", () => {
    expect(tenantSlugFromHost("acme.localhost:3000", "localhost")).toBe("acme");
  });
  it("returns null for apex (no subdomain)", () => {
    expect(tenantSlugFromHost("klasio.app", ROOT)).toBeNull();
  });
  it("ignores reserved labels www/app", () => {
    expect(tenantSlugFromHost("www.klasio.app", ROOT)).toBeNull();
    expect(tenantSlugFromHost("app.klasio.app", ROOT)).toBeNull();
  });
  it("returns null for null/empty host", () => {
    expect(tenantSlugFromHost(null, ROOT)).toBeNull();
    expect(tenantSlugFromHost("", ROOT)).toBeNull();
  });
  it("lowercases the slug", () => {
    expect(tenantSlugFromHost("ACME.klasio.app", ROOT)).toBe("acme");
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- tenantSlugFromHost`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement**

```ts
const RESERVED = new Set(["www", "app"]);

/**
 * Resolve a tenant slug from a Host header given the configured root domain.
 * Returns null when the host is the apex, a reserved label, or has no subdomain.
 */
export function tenantSlugFromHost(host: string | null, rootDomain: string): string | null {
  if (!host) return null;
  const hostname = host.split(":")[0].toLowerCase().trim();
  const root = rootDomain.toLowerCase().trim();
  if (!hostname || hostname === root) return null;
  if (!hostname.endsWith(`.${root}`)) return null;

  const sub = hostname.slice(0, hostname.length - root.length - 1); // strip ".<root>"
  if (!sub || sub.includes(".")) return null; // only a single-label subdomain
  if (RESERVED.has(sub)) return null;
  return sub;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- tenantSlugFromHost`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/lib/tenant/tenantSlugFromHost.ts web/src/lib/tenant/tenantSlugFromHost.test.ts
git commit -m "feat(web): add tenantSlugFromHost host resolver"
```

---

## Task 17: Parametrize `StudentForm` for reuse (admin + self modes)

**Files:**
- Modify: `web/src/components/students/StudentForm.tsx`
- Test: `web/src/components/students/StudentForm.test.tsx` (create)

- [ ] **Step 1: Write the failing test**

```tsx
import { render, screen, fireEvent } from "@testing-library/react";
import StudentForm from "./StudentForm";
// Wrap with the test i18n provider the repo uses (NextIntlClientProvider with messages).

function renderForm(props = {}) {
  return render(/* <NextIntlClientProvider ...> */ <StudentForm mode="self" onSubmit={jest.fn()} {...props} /> /* </...> */);
}

it("renders the canonical field set in self mode", () => {
  renderForm();
  expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/phone/i)).toBeInTheDocument();          // present (was missing in old RegistrationForm)
  expect(screen.getByLabelText(/blood type/i)).toBeInTheDocument();      // present
});

it("reveals the structured tutor block when DOB makes the applicant a minor", () => {
  renderForm();
  const minorDob = new Date();
  minorDob.setFullYear(minorDob.getFullYear() - 10);
  fireEvent.change(screen.getByLabelText(/date of birth/i), {
    target: { value: minorDob.toISOString().split("T")[0] },
  });
  expect(screen.getByLabelText(/tutor first name/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/tutor email/i)).toBeInTheDocument();
});

it("calls onSubmit with the assembled payload in self mode (no router redirect)", async () => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  renderForm({ onSubmit });
  // fill required adult fields, submit, assert onSubmit called with {firstName, phone, bloodType, ...}
});
```

> Use the repo's existing component-test helper for `NextIntlClientProvider` (copy from another `*.test.tsx`). Mock `next/navigation`'s `useRouter` (already used by the component).

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- StudentForm`
Expected: FAIL — `StudentForm` has no `mode`/`onSubmit` props; the submit path is hard-wired to `api.post('/students')` + router.

- [ ] **Step 3: Implement**

Change the props + submit branch only — keep every field, order, and the `validate()` rule set untouched (this is the single source of truth):

```tsx
interface StudentFormProps {
  student?: StudentDetail;
  /** 'admin' = authenticated create/edit via api + redirect; 'self' = public self-registration. */
  mode?: "admin" | "self";
  /** self mode: called with the assembled CreateStudentRequest payload. */
  onSubmit?: (payload: CreateStudentRequest) => Promise<void>;
  submitLabel?: string;
}

export default function StudentForm({ student, mode = "admin", onSubmit, submitLabel }: StudentFormProps) {
  // ...existing state + validate() unchanged...
  const [selfSuccess, setSelfSuccess] = useState(false);
```

Replace the `try { ... }` body inside `handleSubmit` with:
```tsx
    try {
      const body: CreateStudentRequest = {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        dateOfBirth,
        eps: eps.trim(),
        identityNumber: identityNumber.trim(),
        identityDocumentType,
        bloodType: bloodType || null,
        phone: phone.trim(),
        tutorFirstName: tutorFirstName.trim() || null,
        tutorLastName: tutorLastName.trim() || null,
        tutorRelationship: tutorRelationship.trim() || null,
        tutorPhone: tutorPhone.trim() || null,
        tutorEmail: tutorEmail.trim() || null,
      };

      if (mode === "self") {
        await onSubmit!(body);
        setSelfSuccess(true);
        return;
      }

      if (isEdit) {
        await api.put<StudentDetail>(`/students/${student!.id}`, body);
        router.push(`/students/${student!.id}`);
      } else {
        const created = await api.post<StudentDetail>("/students", body);
        router.push(`/students/${created.id}`);
      }
    } catch (err) {
      // ...existing ApiError handling unchanged...
    } finally {
      setSubmitting(false);
    }
```

Add, before the main `return`:
```tsx
  if (mode === "self" && selfSuccess) {
    return (
      <div className="rounded-md bg-green-50 border border-green-200 p-6 text-center" role="status">
        <h2 className="text-lg font-semibold text-green-800 mb-2">{t("selfSuccessTitle")}</h2>
        <p className="text-sm text-green-700">{t("selfSuccessMessage")}</p>
        <a href="/login" className="mt-4 inline-block text-sm text-k-volt">{t("goToLogin")}</a>
      </div>
    );
  }
```

Use `submitLabel ?? (isEdit ? t("formSaveButton") : t("formCreateButton"))` for the button text.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- StudentForm`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/components/students/StudentForm.tsx web/src/components/students/StudentForm.test.tsx
git commit -m "feat(web): parametrize StudentForm for admin + self-registration reuse"
```

---

## Task 18: Subdomain `/register` page using `StudentForm`; retire path route + `RegistrationForm`

**Files:**
- Create: `web/src/app/register/page.tsx`
- Create: `web/src/app/api/auth/register/route.ts`
- Delete: `web/src/app/register/[tenantSlug]/page.tsx`
- Delete: `web/src/app/api/auth/register/[tenantSlug]/route.ts`
- Delete: `web/src/components/auth/RegistrationForm.tsx`
- Modify: `web/messages/en.json`, `web/messages/es.json`

- [ ] **Step 1: Implement the new proxy route** (slug now comes from a header set by the page, not the path)

`web/src/app/api/auth/register/route.ts`:
```ts
import { NextRequest, NextResponse } from "next/server";

const API_BASE_URL = process.env.BACKEND_URL ?? "http://localhost:8080/api/v1";

export async function POST(request: NextRequest) {
  const slug = request.headers.get("x-tenant-slug");
  if (!slug) {
    return NextResponse.json(
      { error: { code: "TENANT_REQUIRED", message: "Missing tenant" } },
      { status: 400 }
    );
  }
  const body = await request.json();
  const backendResponse = await fetch(`${API_BASE_URL}/tenants/${encodeURIComponent(slug)}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await backendResponse.json();
  return NextResponse.json(data, { status: backendResponse.status });
}
```

- [ ] **Step 2: Implement the page** (server component resolves slug from Host; renders the shared form via a thin client wrapper)

`web/src/app/register/page.tsx`:
```tsx
import { headers } from "next/headers";
import { getTranslations } from "next-intl/server";
import { tenantSlugFromHost } from "@/lib/tenant/tenantSlugFromHost";
import SelfRegistration from "@/components/auth/SelfRegistration";

export default async function RegisterPage() {
  const t = await getTranslations("registerPage");
  const host = (await headers()).get("host");
  const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN ?? "localhost";
  const slug = tenantSlugFromHost(host, rootDomain);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <img src="/logo.svg" alt="Klasio" width={48} height={48} className="mb-8" />
      <div className="w-full max-w-2xl bg-k-surface rounded-k-xl shadow-k-modal p-10">
        {slug ? (
          <>
            <div className="text-center mb-6">
              <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
              <p className="mt-2 text-sm text-k-muted">{t("subtitle", { tenantSlug: slug })}</p>
            </div>
            <SelfRegistration tenantSlug={slug} />
          </>
        ) : (
          <div className="text-center">
            <h1 className="text-[22px] font-extrabold text-k-dark">{t("noTenantTitle")}</h1>
            <p className="mt-2 text-sm text-k-muted">{t("noTenantBody")}</p>
          </div>
        )}
      </div>
    </div>
  );
}
```

Create the client wrapper `web/src/components/auth/SelfRegistration.tsx` (owns the fetch + maps the backend `error` shape):
```tsx
"use client";
import StudentForm from "@/components/students/StudentForm";
import { useTranslations } from "next-intl";
import type { CreateStudentRequest } from "@/lib/types/student";

export default function SelfRegistration({ tenantSlug }: { tenantSlug: string }) {
  const t = useTranslations("auth.register");
  async function submit(payload: CreateStudentRequest) {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json", "x-tenant-slug": tenantSlug },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => null);
      throw new Error(data?.error?.message ?? t("errorNetwork"));
    }
  }
  return <StudentForm mode="self" onSubmit={submit} submitLabel={t("submit")} />;
}
```

> `StudentForm`'s catch block handles `ApiError` with `err.details`; a plain `Error` thrown here falls to `setApiError(err.message)` — confirm the catch sets `apiError` for non-`ApiError` throwables (it currently uses `tCommon("unexpectedError")` for the `else`). Adjust the `else` branch to `setApiError(err instanceof Error ? err.message : tCommon("unexpectedError"))` so the generic backend message surfaces.

- [ ] **Step 3: Delete the retired files**

```bash
git rm web/src/app/register/[tenantSlug]/page.tsx \
       web/src/app/api/auth/register/[tenantSlug]/route.ts \
       web/src/components/auth/RegistrationForm.tsx
```

- [ ] **Step 4: Add i18n strings**

In `web/messages/en.json` add under `registerPage`: `noTenantTitle`, `noTenantBody`; under `students`: `selfSuccessTitle`, `selfSuccessMessage`, `goToLogin` (English copy). Mirror keys in `es.json` (Spanish copy — UI strings stay localized, but ensure English defaults exist and are correct per the project's English-UI rule). Confirm `auth.register.submit` / `errorNetwork` already exist (they do).

> Per project rule, user-facing strings must be **English** in `en.json`; provide Spanish in `es.json`.

- [ ] **Step 5: Build + verify routing**

Run: `cd web && npm run build`
Expected: build passes; `/register` compiles; no references to deleted files (`grep -rn "RegistrationForm\|register/\[tenantSlug\]" web/src` returns nothing).

- [ ] **Step 6: Commit**

```bash
git add -A web/src/app/register web/src/app/api/auth/register web/src/components/auth web/messages
git commit -m "feat(web): subdomain self-registration page reusing StudentForm; retire RegistrationForm"
```

---

## Task 19: Superadmin tenant-form toggle + copy-invite-link

**Files:**
- Modify: the tenant create/edit form (locate: `grep -rln "CreateTenant\|tenant" web/src/components --include=*.tsx | grep -i form`; likely `web/src/components/tenants/TenantForm.tsx`)
- Modify: tenant types (locate `web/src/lib/types/tenant.ts`)
- Test: extend that component's test if present

- [ ] **Step 1: Write the failing test** (toggle visible, default on; copy-link renders subdomain URL)

```tsx
it("renders the self-registration toggle defaulting to enabled", () => {
  // render TenantForm (create mode), assert a checkbox/switch labelled self-registration is checked by default
});
it("shows a copy-invite-link control building the subdomain URL", () => {
  // edit mode for tenant slug 'acme' → link text/value contains 'acme.' + root domain + '/register'
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- TenantForm`
Expected: FAIL — no toggle/link yet.

- [ ] **Step 3: Implement**

- Add `selfRegistrationEnabled?: boolean` to the tenant create/update request type (default `true`).
- Add a labelled toggle to the form (superadmin context — the form is already superadmin-only).
- On submit, include `selfRegistrationEnabled` in the create payload; for edit, call `PATCH /tenants/{id}/self-registration` with `{enabled}` when the value changes (via a small hook or `api.patch`).
- Add a read-only "invite link" field shown when a slug exists: `https://${slug}.${process.env.NEXT_PUBLIC_ROOT_DOMAIN}/register` with a copy button (`navigator.clipboard.writeText`).

> Match the form's existing field components (`Input`, toggle/checkbox primitive) and submission flow. If there is no edit form yet (tenant editing may be create-only in the UI), implement the toggle for **create** and surface the toggle + copy-link on the tenant detail/list view where superadmin manages a tenant — wire the PATCH there.

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- TenantForm`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/components/tenants web/src/lib/types/tenant.ts
git commit -m "feat(web): superadmin self-registration toggle + copy invite link"
```

---

# Phase 6 — Verification & docs

## Task 20: Full backend + frontend verification

- [ ] **Step 1: Backend full test suite**

Run: `cd api && ./mvnw test`
Expected: BUILD SUCCESS, all green. If coverage gate is enforced (≥70% business layer), confirm it passes.

- [ ] **Step 2: Frontend lint + test + build**

Run: `cd web && npm run lint && npm test && npm run build`
Expected: all pass; no dangling references to deleted files.

- [ ] **Step 3: Manual smoke (local subdomain)**

- Set `NEXT_PUBLIC_ROOT_DOMAIN=localhost`. Seed an ACTIVE tenant slug `acme` (self_registration_enabled true).
- Visit `http://acme.localhost:3000/register` → fill adult form → submit → success screen.
- Confirm: student row has phone+bloodType; user pending-setup; one account-setup email in the local transport; flag off → page shows disabled/no-tenant or 403 on submit.

- [ ] **Step 4: Commit any fixes**

```bash
git add -A && git commit -m "test(rf40): fix issues found in full verification"
```

---

## Task 21: Update `functional-requirements.md` + CLAUDE.md

**Files:**
- Modify: `functional-requirements.md` (mark RF-40 ✅ with a note)
- Modify: `CLAUDE.md` (add to Implemented Features table)

- [ ] **Step 1:** In `functional-requirements.md`, set RF-40 status to ✅ with a note: "self-registration unified onto `CreateStudentUseCase`; subdomain entry `{slug}.<root>/register`; superadmin `selfRegistrationEnabled` flag (V072, first tenant feature-flag); divergent `RegistrationForm` retired."

- [ ] **Step 2:** In `CLAUDE.md` "Implemented Features" table add a row for `feature/040-self-registration` → RF-40 ✅, and note V072 + the first tenant feature-flag pattern under Recent Changes.

- [ ] **Step 3: Commit**

```bash
git add functional-requirements.md CLAUDE.md
git commit -m "docs: mark RF-40 self-registration complete"
```

---

## Self-Review (performed against the spec)

**Spec coverage:**
- AC1 entry point + invitation + optional flag → Tasks 4–10 (flag), 16/18 (subdomain route), 19 (copy link). ✅
- AC2 form parity (one definition + validation) → Tasks 17–18 (reuse `StudentForm`, retire `RegistrationForm`). ✅
- AC3 same account-setup email (no weaker path) → Task 12 (delegate to `CreateStudentUseCase`, which already dispatches the RF-32 account-setup email) + Task 15 IT asserts exactly one account-setup email. ✅
- AC4 single source of truth + unique email/identity per tenant + non-enumerating + no cross-tenant leak → Tasks 1–3 (identity uniqueness in shared path), 12–13 (generic conflict mapping), 15 IT (isolation). ✅
- AC5 scope boundary (account+profile only) → Task 12 creates only User+Student; Task 15 IT asserts no membership. ✅

**Placeholder scan:** Steps that reference repo-specific accessor names (mapper method names, audit-entry construction, IT scaffolding) explicitly instruct mirroring a named existing method and provide the surrounding code shape — not "TBD". Acceptable for a brownfield codebase; flagged with `>` notes.

**Type consistency:** `CreateStudentCommand` field order matches the verified record (Task 12 mapping). `RegisterStudentCommand` redefined once (Task 12) and consumed consistently (Tasks 12–13). `tenantSlugFromHost(host, rootDomain)` signature consistent across Tasks 16/18. `setSelfRegistration(enabled, actor)` / `isSelfRegistrationEnabled()` consistent across Tasks 5/6/8/11. Audit action string `TENANT_SELF_REGISTRATION_TOGGLED` consistent across Tasks 4/9/10.

**Known cross-task compile coupling:** Tasks 5–7 change `Tenant.create`/`reconstitute` arity together — they must land as a group before the tenant module compiles (noted in Task 5).
