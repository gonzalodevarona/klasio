# RF-40 — Student Self-Registration (Form Parity)

**Date:** 2026-06-07
**RF:** RF-40 (Auth – Student Self-Registration, Form Parity)
**Builds on:** RF-01 (self-registration + account-setup email), RF-05 (tenant slug/subdomain), RF-11 (admin `StudentForm`), RF-32 (account-setup email), RF-38 (tenant feature-flag pattern)
**Status:** Approved design — ready for implementation plan

---

## 1. Problem

Student creation lives 100% in the admin's hands. RF-40 makes self-service onboarding a first-class workflow: a tenant shares a tenant-scoped registration link, and a prospective student fills out **the exact same form** the admin uses — same fields, order, validation — producing the **same** `User` + student profile and the **same** account-setup email.

Today two paths diverge:

- **Frontend:** admin `StudentForm` (RF-11) has `phone`, `bloodType`, structured tutor (`tutorFirstName/lastName/relationship/phone/email`), full validation, ui primitives → POST `/students`. Self-reg `RegistrationForm` (RF-01) is **missing** `phone` + `bloodType`, uses **unstructured** tutor (`tutorFullName/tutorContact`), raw inputs → POST `/api/auth/register/{slug}`.
- **Backend:** `RegisterStudentService` writes the profile via `StudentProfileAdapter.createStudentProfile`, which **bypasses** the `Student.create()` aggregate factory + domain events, hardcodes `phone=null`/`bloodType=null`, and heuristically splits `tutorFullName`. `CreateStudentService` (admin) uses the real aggregate + events + `accountSetupCreationPort.createAndDispatchSetup`.
- **Additional parity gap found:** `CreateStudentService` checks **email** uniqueness only; `RegisterStudentService` checks email **and** identity number. The unified path must enforce both per tenant (correct per RF-01/RF-11), which also fixes the admin path.

Both paths already use the account-setup token + `AccountSetupInitiated` event — that part is unified.

## 2. Decisions (locked)

| # | Decision |
|---|---|
| Backend unification | **Approach A** — `RegisterStudentService` delegates to `CreateStudentUseCase`. Delete the divergent `StudentProfileAdapter.createStudentProfile`. One creation path. |
| `createdBy` actor | `SYSTEM_ACTOR` sentinel (`00000000-0000-0000-0000-000000000000`, existing precedent in membership module) for self-registration. |
| Tenant routing | **Real subdomain now**, **register entry only**. `acme.klasio.app/register`. Login stays global-by-email (unchanged). Reusable host→slug resolver, wired to register only. |
| `selfRegistrationEnabled` flag | **Build now**, **superadmin-controlled**, default `TRUE`. First tenant feature-flag (establishes the pattern RF-38/RF-44 reuse). |
| Form parity | Mandatory (RF-40 AC2). Parametrize the canonical admin `StudentForm`; retire divergent `RegistrationForm`. One form definition, one validation rule set. |
| Non-enumerating errors | Public register maps email/identity collision to one generic 409. Admin path stays explicit. |

## 3. Backend unification (§A — core)

`RegisterStudentService` becomes a thin orchestrator:

1. `tenantResolverPort.resolveTenantIdBySlug(slug)` → 404 (`TENANT_NOT_FOUND`) if absent.
2. Load tenant → if `!selfRegistrationEnabled` → reject `403 SELF_REGISTRATION_DISABLED`.
3. Build `CreateStudentCommand` (full structured field set) with `createdBy = SYSTEM_ACTOR`.
4. `CreateStudentUseCase.execute(cmd)` — the single creation path.

Removals/changes:

- **Delete** `StudentProfilePort.createStudentProfile` + the divergent body in `StudentProfileAdapter`. (Keep `existsByIdentityNumberInTenant` only if still needed; uniqueness moves into the use case — see below.)
- `RegisterStudentService` no longer creates `User`, token, or event itself; `CreateStudentService` already does `Student.create()` + domain events + `accountSetupCreationPort.createAndDispatchSetup` (the same RF-32 account-setup email). AC3/AC4 satisfied structurally.
- **Move identity-number uniqueness into `CreateStudentService`** so both admin-create and self-register enforce email **+** identity number per tenant.
- `RegisterStudentCommand`: add `phone`, `bloodType`, `tutorFirstName`, `tutorLastName`, `tutorRelationship`, `tutorPhone`, `tutorEmail`; drop `tutorFullName`, `tutorContact`.

## 4. Tenant flag (§B — first tenant feature-flag)

- **Flyway V072:** `ALTER TABLE tenants ADD COLUMN self_registration_enabled BOOLEAN NOT NULL DEFAULT TRUE;`
  - Ownership note: confirm `tenants` is owned by `klasio_app` before the `ALTER` (see CLAUDE.md Flyway ownership rule).
- `Tenant` domain model: `selfRegistrationEnabled` field + `enableSelfRegistration()` / `disableSelfRegistration()`; `TenantJpaEntity` column; tenant creation defaults `TRUE`.
- Superadmin-only toggle at tenant **create** + **edit**. Absent from all admin/non-superadmin UIs and endpoints.
- Audit: new action type `TENANT_SELF_REGISTRATION_TOGGLED` (immutable log, 1-year retention) on toggle.

## 5. Subdomain routing (§C — register entry only)

- Invite link: `https://{slug}.klasio.app/register`.
- New route `web/src/app/register/page.tsx` (no path param). Server component reads `Host` via `headers()` → `tenantSlugFromHost(host)`:
  - strip the configured root domain, take the first label, ignore `www` / `app` / bare apex → return slug or `null`.
  - No subdomain → friendly "open your league's registration link" page (no form).
- Dev: `*.localhost` (browser-native → 127.0.0.1), e.g. `acme.localhost:3000/register`. Prod: wildcard DNS `*.klasio.app`. Root domain from env (`NEXT_PUBLIC_ROOT_DOMAIN`).
- Backend stays slug-keyed (resolver unchanged). Frontend derives slug from host and passes it to the register call (Next proxy route).
- **Retire** the existing `web/src/app/register/[tenantSlug]/page.tsx` path route (redirect to subdomain form or remove) — "slug = subdomain, not slash."
- Admin UI gains a **copy invite link** affordance that builds the subdomain URL for the tenant.

## 6. Frontend form parity (§D)

- Parametrize the canonical admin `StudentForm`:
  - props: `onSubmit(payload) => Promise<void>`, `submitLabel`, `mode: 'admin' | 'self'`.
  - `mode` controls chrome only: `admin` → `router.push` on success; `self` → success screen ("check your email to set your password") + sign-in link.
  - All fields, order, and the `validate()` rule set stay in one place (single definition).
- `/register` renders `StudentForm` in `self` mode → unauthenticated POST to the register endpoint → success screen.
- **Retire** the divergent `RegistrationForm` (RF-01) and its bespoke field set. (AC2)

## 7. Data flow

```
acme.localhost/register        (server component: Host -> slug via tenantSlugFromHost)
  -> StudentForm[self] submit
  -> POST /api/auth/register    (Next proxy; slug from host)
  -> RegistrationController
  -> RegisterStudentService     (resolve slug->tenant; selfRegistrationEnabled check)
  -> CreateStudentUseCase.execute(createdBy = SYSTEM_ACTOR)
       -> Student.create() + domain events -> StudentRepository
       -> accountSetupCreationPort.createAndDispatchSetup
            -> User(pending-setup) + AccountSetupToken + AccountSetupInitiated
  -> account-setup email (RF-32 template)
  -> student clicks link -> sets password -> User ACTIVE
  -> student has NO active membership until admin/manager enrolls + payment validated (RF-07/RF-12/RF-14)
```

## 8. Error handling / security

- **Non-enumerating (AC4):** the public register endpoint maps email/identity collisions to **one generic 409** ("registration couldn't be completed") with no field/account disclosure. The admin path keeps explicit errors. Mapping lives at the controller/error layer, not the use case.
- Flag off → `403`; the `/register` page renders a disabled/not-available state.
- Tenant RLS and tenant scoping unchanged; every write tenant-scoped.
- All validation enforced server-side (client validation is UX only).
- Minor (age < 18 from date of birth) → tutor block required, server-enforced.

## 9. Acceptance-criteria mapping

| AC | Covered by |
|----|------------|
| AC1 Entry point + invitation, optional flag | §5 subdomain route + §4 `selfRegistrationEnabled` + admin copy-link |
| AC2 Form parity (one definition + validation) | §6 parametrized `StudentForm`, retire `RegistrationForm` |
| AC3 Same account-setup email (no weaker path) | §3 delegate to `CreateStudentUseCase` (already does `createAndDispatchSetup`) |
| AC4 Single source of truth, unique email+identity per tenant, non-enumerating, no cross-tenant leak | §3 unified path + identity-uniqueness move + §8 error mapping + RLS |
| AC5 Scope boundary (account+profile only) | §3 creates only `User`+`Student`; enrollment/level/membership untouched |

## 10. Out of scope

- App-wide subdomain routing / tenant-scoped login (login stays global-by-email).
- RF-38 `managersEnabled` flag (separate RF; this RF establishes the tenant-flag pattern it will reuse).
- Program enrollment, level assignment, membership creation (RF-07/RF-12/RF-14 — admin/manager owned).

## 11. Testing (TDD — tests first)

**Backend unit:**
- register delegates → created student has `phone`, `bloodType`, structured tutor populated.
- minor without tutor block → rejected.
- duplicate email → non-enumerating generic error.
- duplicate identity number → non-enumerating generic error.
- `selfRegistrationEnabled = false` → 403.
- identity-number uniqueness now enforced on the **admin** create path too.

**Backend integration:**
- subdomain slug → correct tenant; cross-tenant isolation holds.
- account-setup email dispatched exactly once; `AccountSetupInitiated` published.
- created student is `ACTIVE` after setup, with no membership.

**Frontend:**
- `StudentForm` renders identical fields/order in `admin` vs `self` mode.
- minor toggle reveals tutor block in both modes.
- validation parity (same required fields, same messages).
- `self` mode shows success screen; no-subdomain renders the guidance page.
