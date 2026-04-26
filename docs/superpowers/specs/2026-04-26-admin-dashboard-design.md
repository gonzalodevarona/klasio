# Admin Dashboard — Design Spec (RF-31)

**Date:** 2026-04-26  
**Branch:** `feature/full-redesign`  
**RF:** RF-31 (Admin Dashboard)  
**Status:** Approved — ready for implementation

---

## 1. Goal

Replace the placeholder admin dashboard (`web/src/app/(dashboard)/admin/dashboard/page.tsx`) with a live, data-driven page. Four KPI stat cards and a filterable student attendance-control table. Single backend request — no waterfall.

---

## 2. Scope

| Layer | What changes |
|---|---|
| Backend | New `GET /api/v1/admin/dashboard` endpoint + supporting repository methods |
| Frontend hook | New `web/src/hooks/useAdminDashboard.ts` |
| Frontend page | Full replacement of `admin/dashboard/page.tsx` |
| Proxy route | **None** — `api.get()` calls backend directly via `credentials: include` |
| DB migrations | **None** — no new tables |

---

## 3. Backend

### 3.1 Module placement

New package `com.klasio.admin.dashboard` — not inside `auth` or `membership` because this endpoint aggregates across three domain modules.

```
api/src/main/java/com/klasio/admin/dashboard/
  application/
    dto/     AdminDashboardDto.java
             DashboardStudentDto.java
    service/ AdminDashboardService.java
    port/    AdminDashboardRepository.java   (output port)
  infrastructure/
    persistence/ AdminDashboardAdapter.java  (JPA/native SQL)
    web/         AdminDashboardController.java
```

### 3.2 Endpoint

```
GET /api/v1/admin/dashboard
Authorization: ADMIN | MANAGER | SUPERADMIN
```

TenantId extracted from JWT claims (`(Map<String,Object>) auth.getDetails()` → `tenantId` key), same as every other controller.

### 3.3 Response body

```json
{
  "studentCount": 248,
  "newStudentsThisMonth": 12,
  "totalHoursConsumed": 1840,
  "pendingPaymentProofs": 14,
  "activeProgramCount": 7,
  "students": [
    {
      "id": "uuid",
      "name": "Carlos Rodríguez",
      "programName": "Natación Avanzado",
      "membershipStatus": "ACTIVE",
      "availableHours": 4,
      "purchasedHours": 24
    }
  ]
}
```

All fields for `students[]` are nullable except `id` and `name`.

### 3.4 Query strategy

`AdminDashboardService` depends on ONE output port: `AdminDashboardRepository` (declared in `application/port/`). The adapter implements all methods by querying across tables — the service never imports repositories from other modules.

Five methods on `AdminDashboardRepository`:

| Method | Logical source | Query |
|---|---|---|
| `countStudents(tenantId)` | users + user_roles | `COUNT WHERE role = STUDENT AND tenantId = :t` |
| `countNewStudentsThisMonth(tenantId)` | same | same + `created_at >= first day of current month (UTC)` |
| `sumConsumedHours(tenantId)` | memberships | `SUM(purchased_hours - available_hours) WHERE status = ACTIVE AND tenantId = :t` |
| `countPendingProofs(tenantId)` | payment_proofs | `COUNT WHERE status = 'PENDING_REVIEW' AND tenantId = :t` |
| `countActivePrograms(tenantId)` | programs | `COUNT WHERE status = ACTIVE AND tenantId = :t` |
| `findStudentSummaries(tenantId)` | cross-table native SQL | See §3.5 |

### 3.5 Student list query

One native SQL query to avoid N+1 across three modules. Returns up to 50 rows ordered by membership `updated_at DESC` (most recently active students first).

```sql
SELECT
    u.id,
    u.first_name || ' ' || u.last_name AS name,
    p.name                              AS program_name,
    m.status                            AS membership_status,
    m.available_hours,
    m.purchased_hours
FROM users u
-- most recent ACTIVE enrollment per student
LEFT JOIN LATERAL (
    SELECT e.program_id
    FROM enrollments e
    WHERE e.student_id = u.id
      AND e.tenant_id  = :tenantId
      AND e.status     = 'ACTIVE'
    ORDER BY e.created_at DESC
    LIMIT 1
) enr ON TRUE
LEFT JOIN programs p ON p.id = enr.program_id AND p.tenant_id = :tenantId
-- most recent ACTIVE membership per student
LEFT JOIN LATERAL (
    SELECT m2.status, m2.available_hours, m2.purchased_hours, m2.updated_at
    FROM memberships m2
    WHERE m2.student_id = u.id
      AND m2.tenant_id  = :tenantId
      AND m2.status     = 'ACTIVE'
    ORDER BY m2.updated_at DESC
    LIMIT 1
) m ON TRUE
WHERE u.tenant_id = :tenantId
  AND EXISTS (
      SELECT 1 FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role = 'STUDENT'
  )
ORDER BY COALESCE(m.updated_at, u.created_at) DESC
LIMIT 50
```

Implemented via `@Query(nativeQuery = true)` in `AdminDashboardAdapter`.

### 3.6 TDD — tests to write first

- `AdminDashboardServiceTest` (unit): mock repository port, verify each field maps correctly; verify tenant isolation (service passes tenantId to every call).
- `AdminDashboardControllerTest` (slice): verify 200 with ADMIN token, 403 with STUDENT token, response shape matches DTO.

---

## 4. Frontend hook

**File:** `web/src/hooks/useAdminDashboard.ts`

```ts
"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";

export interface DashboardStudent {
  id: string;
  name: string;
  programName: string | null;
  membershipStatus: string | null;
  availableHours: number | null;
  purchasedHours: number | null;
}

export interface AdminDashboardData {
  studentCount: number;
  newStudentsThisMonth: number;
  totalHoursConsumed: number;
  pendingPaymentProofs: number;
  activeProgramCount: number;
  students: DashboardStudent[];
}

export function useAdminDashboard() {
  const [data, setData] = useState<AdminDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.get<AdminDashboardData>("/admin/dashboard");
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load dashboard.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  return { data, loading, error, refetch: load };
}
```

No proxy route. `api.get()` hits the backend directly (`NEXT_PUBLIC_API_URL/admin/dashboard`) with `credentials: include`.

---

## 5. Dashboard page

**File:** `web/src/app/(dashboard)/admin/dashboard/page.tsx`  
**Change:** Full replacement — server component → client component.

### 5.1 Layout

```
┌─ Header: "Dashboard" / "Período actual" ──────────────────┐
├─ Quick actions: [Validar pago] [Registrar clase] [Ver estudiantes] [Programas] ─┤
├─ KPI grid (1→2→4 cols responsive) ───────────────────────┤
│  [Estudiantes]  [Horas consumidas ◼]  [Pagos pendientes]  [Programas activos]  │
├─ "Control de asistencia" card ────────────────────────────┤
│  Filter pills: Todos | Activo | Por vencer | Inactivo | Vencida | Nuevo         │
│  Table: Estudiante | Programa | Horas | Estado                                  │
└───────────────────────────────────────────────────────────┘
```

### 5.2 KPI cards

| Card | Field | Dark bg? | Sub-line |
|---|---|---|---|
| Estudiantes | `studentCount` | No | `↑ N este mes` when `newStudentsThisMonth > 0` |
| Horas consumidas | `totalHoursConsumed` | **Yes** | "Este período" |
| Pagos pendientes | `pendingPaymentProofs` | No | "Requieren acción" (amber) or "Al día" |
| Programas activos | `activeProgramCount` | No | — |

### 5.3 HoursBar

Color keyed to consumption percentage (`consumed = purchased - available`):

| Consumed % | Color |
|---|---|
| ≤ 33% | `#CAFF4D` (volt) |
| ≤ 66% | `#8AE800` (green) |
| ≤ 85% | `#FFC107` (amber) |
| > 85% | `#CC2200` (red) |

### 5.4 Status badges

| Backend status | Label | Style |
|---|---|---|
| `ACTIVE` | Activo | volt bg `#CAFF4D` |
| `INACTIVE` | Inactivo | gray bg |
| `EXPIRED` | Vencida | red tint |
| `EXPIRING` | Por vencer | yellow tint (future) |
| `NEW` | Nuevo | blue tint (future) |
| `null` | — | muted |

`EXPIRING` and `NEW` are UI-only filter pills today; backend does not emit them yet. Filter shows 0 results — not an error.

### 5.5 States

- **Loading:** Skeleton shimmer cards + skeleton table rows
- **Error:** Red banner with message text
- **Empty table:** "No hay estudiantes con este estado."
- **Nominal:** Full grid + table

### 5.6 Quick-action links

| Button | href |
|---|---|
| Validar pago | `/payment-proofs` |
| Registrar clase | `/classes` |
| Ver estudiantes | `/students` |
| Programas | `/programs` |

### 5.7 Labels

Hardcoded Spanish strings (matching existing page intent and design mockup). i18n migration is a separate task.

---

## 6. Out of scope

- Pagination for the student table (50-row limit is sufficient for v1.0)
- Real-time refresh / websocket updates
- CSV export from the dashboard
- `EXPIRING` / `NEW` backend status values
- Next.js proxy route for this endpoint

---

## 7. Verification checklist

1. `npx tsc --noEmit` — zero errors
2. ADMIN login → all 4 KPI cards show real numbers
3. Filter pills filter the attendance table correctly
4. HoursBar turns volt → amber → red as consumption rises
5. "Validar pago" → `/payment-proofs`, "Iniciar clase" → `/classes`
6. Backend not running → page renders error banner, does not crash
7. STUDENT JWT → `GET /api/v1/admin/dashboard` returns 403
