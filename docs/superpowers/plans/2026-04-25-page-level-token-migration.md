# Page-Level Design Token Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate all ~51 `src/app` page files from raw Tailwind grays/blues/whites to the `k-*` palette and replace inline primitives with `Button`, `Card`, `StatCard`, `Badge` components.

**Architecture:** Sequential group-by-group execution (dashboards → list → detail → form → student zone → auth → skeletons). Each group ends with `tsc --noEmit` gate and a conventional commit. No data-fetching, no API, no server/client boundary changes (except removing `"use client"` from 4 placeholder dashboards that lose their sign-out button).

**Tech Stack:** Next.js 15 App Router, next-intl, Tailwind CSS 3.4 with `k-*` tokens, `@/components/ui` (Button, Card, StatCard, Badge, Input, Select).

---

## File Map

| Group | Files | Key changes |
|---|---|---|
| Dashboards | `admin/dashboard/page.tsx`, `manager/dashboard/page.tsx`, `professor/dashboard/page.tsx`, `superadmin/dashboard/page.tsx`, `student/dashboard/page.tsx` | Drop useAuth/sign-out; 4 become server components; StatCard grids; Card sections |
| List pages | `students/page.tsx`, `professors/page.tsx`, `programs/page.tsx`, `tenants/page.tsx`, `admins/page.tsx`, `managers/page.tsx`, `payment-proofs/page.tsx`, `notifications/page.tsx`, `programs/[id]/classes/page.tsx`, `classes/page.tsx`, `plans/page.tsx` | Heading h1 style, Button volt CTA, strip duplicate wrappers, Input/Select primitives |
| Detail pages | `students/[id]/page.tsx`, `professors/[id]/page.tsx`, `tenants/[slug]/page.tsx`, `programs/[id]/page.tsx`, `programs/[id]/classes/[classId]/page.tsx`, `programs/[id]/plans/[planId]/page.tsx`, `students/[id]/memberships/[membershipId]/page.tsx`, `student/memberships/[membershipId]/page.tsx`, `students/[id]/memberships/page.tsx` | Back button + mono breadcrumb, danger error state, k-muted loading |
| Form pages | `students/new/page.tsx`, `students/[id]/edit/page.tsx`, `professors/new/page.tsx`, `professors/[id]/edit/page.tsx`, `programs/new/page.tsx`, `programs/[id]/edit/page.tsx`, `programs/[id]/classes/new/page.tsx`, `programs/[id]/classes/[classId]/edit/page.tsx`, `programs/[id]/plans/new/page.tsx`, `programs/[id]/plans/[planId]/edit/page.tsx`, `students/[id]/memberships/new/page.tsx`, `tenants/new/page.tsx`, `student/memberships/new/page.tsx` | Back button + mono breadcrumb, Card wrapper for form body |
| Student zone | `student/classes/page.tsx`, `student/registrations/page.tsx`, `student/enrollments/page.tsx`, `student/memberships/page.tsx` | Badge level, Button primitives, tab pill colors, modal shell |
| Auth pages | `(auth)/login/page.tsx`, `(auth)/forgot-password/page.tsx`, `(auth)/reset-password/page.tsx`, `(auth)/setup-account/page.tsx`, `register/[tenantSlug]/page.tsx` | Dark shell, wordmark, danger tokens |
| Skeletons | `programs/loading.tsx`, `programs/[id]/loading.tsx`, `tenants/loading.tsx`, `tenants/[slug]/loading.tsx`, `student/memberships/new/loading.tsx` | `bg-k-line`, `bg-k-surface`, `rounded-k-sm` |

---

## Task 1 — Add i18n keys for 4 new dashboard namespaces

**Files:**
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Add keys to en.json**

Open `web/messages/en.json`. Insert the following four objects **after** the `"studentEnrollmentsPage"` block (end of file, before the closing `}`):

```json
  "adminDashboard": {
    "title": "Admin Dashboard",
    "subtitle": "League overview",
    "statStudents": "Students",
    "statActiveMemberships": "Active memberships",
    "statPendingProofs": "Pending proofs",
    "statPrograms": "Programs"
  },
  "managerDashboard": {
    "title": "Manager Dashboard",
    "subtitle": "Program overview",
    "statClassesThisWeek": "Classes this week",
    "statStudentsInProgram": "Students in program",
    "statPendingActivations": "Pending activations",
    "statHoursLogged": "Hours logged",
    "membershipsHeading": "Memberships awaiting activation"
  },
  "professorDashboard": {
    "title": "Professor Dashboard",
    "subtitle": "Today's overview",
    "statClassesToday": "Classes today",
    "statStudentsPresent": "Students present",
    "statSessionsThisMonth": "Sessions this month",
    "statHoursTaught": "Hours taught"
  },
  "superadminDashboard": {
    "title": "Superadmin Dashboard",
    "subtitle": "Platform overview",
    "statTenants": "Tenants",
    "statTotalStudents": "Total students",
    "statActiveMemberships": "Active memberships",
    "statMonthlyRevenue": "Monthly revenue"
  }
```

- [ ] **Step 2: Add keys to es.json**

Open `web/messages/es.json`. Insert after the `"studentEnrollmentsPage"` block:

```json
  "adminDashboard": {
    "title": "Panel de Administrador",
    "subtitle": "Resumen de la liga",
    "statStudents": "Estudiantes",
    "statActiveMemberships": "Membresías activas",
    "statPendingProofs": "Comprobantes pendientes",
    "statPrograms": "Programas"
  },
  "managerDashboard": {
    "title": "Panel de Coordinador",
    "subtitle": "Resumen del programa",
    "statClassesThisWeek": "Clases esta semana",
    "statStudentsInProgram": "Estudiantes en el programa",
    "statPendingActivations": "Activaciones pendientes",
    "statHoursLogged": "Horas registradas",
    "membershipsHeading": "Membresías pendientes de activación"
  },
  "professorDashboard": {
    "title": "Panel del Profesor",
    "subtitle": "Resumen de hoy",
    "statClassesToday": "Clases hoy",
    "statStudentsPresent": "Estudiantes presentes",
    "statSessionsThisMonth": "Sesiones este mes",
    "statHoursTaught": "Horas impartidas"
  },
  "superadminDashboard": {
    "title": "Panel de Superadmin",
    "subtitle": "Resumen de la plataforma",
    "statTenants": "Tenants",
    "statTotalStudents": "Total estudiantes",
    "statActiveMemberships": "Membresías activas",
    "statMonthlyRevenue": "Ingresos mensuales"
  }
```

- [ ] **Step 3: Validate JSON**

```bash
python3 -c "import json; json.load(open('web/messages/en.json')); print('en.json OK')"
python3 -c "import json; json.load(open('web/messages/es.json')); print('es.json OK')"
```

Expected: both print `OK` with no errors.

---

## Task 2 — Migrate Admin, Professor, Superadmin dashboards (3 files)

**Files:**
- Rewrite: `web/src/app/(dashboard)/admin/dashboard/page.tsx`
- Rewrite: `web/src/app/(dashboard)/professor/dashboard/page.tsx`
- Rewrite: `web/src/app/(dashboard)/superadmin/dashboard/page.tsx`

All three follow the same pattern: drop `"use client"`, drop `useAuth`, become server components, add StatCard grid.

- [ ] **Step 1: Replace admin/dashboard/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";

export const metadata = { title: "Admin Dashboard - Klasio" };

export default async function AdminDashboard() {
  const t = await getTranslations("adminDashboard");
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{t("subtitle")}</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label={t("statStudents")} value="—" dark />
        <StatCard label={t("statActiveMemberships")} value="—" />
        <StatCard label={t("statPendingProofs")} value="—" />
        <StatCard label={t("statPrograms")} value="—" />
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Replace professor/dashboard/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";

export const metadata = { title: "Professor Dashboard - Klasio" };

export default async function ProfessorDashboard() {
  const t = await getTranslations("professorDashboard");
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{t("subtitle")}</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label={t("statClassesToday")} value="—" dark />
        <StatCard label={t("statStudentsPresent")} value="—" />
        <StatCard label={t("statSessionsThisMonth")} value="—" />
        <StatCard label={t("statHoursTaught")} value="—" />
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Replace superadmin/dashboard/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";

export const metadata = { title: "Superadmin Dashboard - Klasio" };

export default async function SuperadminDashboard() {
  const t = await getTranslations("superadminDashboard");
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{t("subtitle")}</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label={t("statTenants")} value="—" dark />
        <StatCard label={t("statTotalStudents")} value="—" />
        <StatCard label={t("statActiveMemberships")} value="—" />
        <StatCard label={t("statMonthlyRevenue")} value="—" />
      </div>
    </div>
  );
}
```

---

## Task 3 — Migrate Manager dashboard

**Files:**
- Rewrite: `web/src/app/(dashboard)/manager/dashboard/page.tsx`

`DelegatedMembershipList` is a client component — it can be rendered from a server component in Next.js App Router.

- [ ] **Step 1: Replace manager/dashboard/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";
import { DelegatedMembershipList } from "@/components/payment-proofs/DelegatedMembershipList";

export const metadata = { title: "Manager Dashboard - Klasio" };

export default async function ManagerDashboard() {
  const t = await getTranslations("managerDashboard");
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{t("subtitle")}</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label={t("statClassesThisWeek")} value="—" dark />
        <StatCard label={t("statStudentsInProgram")} value="—" />
        <StatCard label={t("statPendingActivations")} value="—" />
        <StatCard label={t("statHoursLogged")} value="—" />
      </div>
      <div className="mt-8">
        <h2 className="text-base font-semibold text-k-dark mb-4">{t("membershipsHeading")}</h2>
        <DelegatedMembershipList />
      </div>
    </div>
  );
}
```

---

## Task 4 — Migrate Student dashboard

**Files:**
- Modify: `web/src/app/(dashboard)/student/dashboard/page.tsx`

Key changes: remove `useAuth` (the `user` variable was unused); import `Badge, Button, Card`; replace `rounded-lg border border-gray-200 bg-white p-5` → `<Card padding="md">`; replace section headings; replace "View all" link classes; replace inline status/level spans with `<Badge>`; replace quick link `<Link className="...">` with `<Button variant="outline" asChild>`.

- [ ] **Step 1: Replace student/dashboard/page.tsx**

```tsx
"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { useTranslations } from "next-intl";
import { useMyMemberships } from "@/hooks/useMemberships";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import HourBalance from "@/components/memberships/HourBalance";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import { Badge, Button, Card } from "@/components/ui";
import { todayInTenantZone, formatSessionDate } from "@/lib/attendanceConstants";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentDashboard() {
  const t = useTranslations("studentDashboard");
  const today = todayInTenantZone();

  const { memberships, loading: membershipsLoading } = useMyMemberships();
  const { enrollments, loading: enrollmentsLoading } = useMyEnrollments();
  const { registrations, loading: registrationsLoading } = useMyRegistrations({
    status: "REGISTERED",
    from: today,
  });
  const upcomingRegistrations = registrations.slice(0, 3);

  const activeMembership = memberships.find(
    (m) =>
      m.status === "ACTIVE" ||
      m.status === "PENDING_PAYMENT" ||
      m.status === "PENDING_PAYMENT_VALIDATION" ||
      m.status === "PENDING_MANAGER_ACTIVATION"
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          {t("subtitle")}
        </p>
      </div>

      <Card padding="md">
        <h2 className="text-base font-semibold text-k-dark mb-4">
          {t("activeMembership")}
        </h2>
        {membershipsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : activeMembership ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-k-dark">
                {activeMembership.planName}
              </span>
              <MembershipStatusBadge status={activeMembership.status} />
            </div>
            <HourBalance
              available={activeMembership.availableHours}
              purchased={activeMembership.purchasedHours}
            />
            <p className="text-xs text-k-muted">
              {t("membershipExpires", { date: formatDate(activeMembership.expirationDate) })}
            </p>
            <Link
              href={`/student/memberships/${activeMembership.id}`}
              className="inline-block text-sm text-k-subtle hover:text-k-dark font-medium"
            >
              {t("viewDetails")}
            </Link>
          </div>
        ) : (
          <p className="text-sm text-k-muted">{t("noMembership")}</p>
        )}
      </Card>

      <Card padding="md">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-k-dark">
            {t("enrollmentsTitle")}
          </h2>
          <Link
            href="/student/enrollments"
            className="text-xs text-k-subtle hover:text-k-dark font-medium"
          >
            {t("viewAll")}
          </Link>
        </div>
        {enrollmentsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : enrollments.length === 0 ? (
          <p className="text-sm text-k-muted">{t("noEnrollments")}</p>
        ) : (
          <ul className="space-y-2">
            {enrollments.slice(0, 3).map((e) => (
              <li key={e.id} className="flex items-center justify-between text-sm">
                <span className="text-k-dark">{e.programName}</span>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-k-muted">{e.level}</span>
                  <Badge
                    variant={e.status === "ACTIVE" ? "active" : "inactive"}
                    label={e.status}
                    small
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card padding="md">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-k-dark">
            {t("upcomingRegistrations")}
          </h2>
          <Link
            href="/student/registrations"
            className="text-xs text-k-subtle hover:text-k-dark font-medium"
          >
            {t("viewAll")}
          </Link>
        </div>
        {registrationsLoading ? (
          <p className="text-sm text-k-muted">{t("loading")}</p>
        ) : upcomingRegistrations.length === 0 ? (
          <p className="text-sm text-k-muted">{t("noRegistrations")}</p>
        ) : (
          <ul className="space-y-2">
            {upcomingRegistrations.map((r) => (
              <li key={r.id} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-3">
                  <span className="text-k-dark">
                    {formatSessionDate(r.sessionDate)}
                  </span>
                  <span className="text-k-muted">
                    {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                  </span>
                  {r.sessionStatus === "ALERTED" && (
                    <span
                      title={r.sessionAlertReason ?? t("alertTooltip")}
                      className="inline-flex text-k-warn-text"
                    >
                      <AlertTriangle className="w-4 h-4" />
                    </span>
                  )}
                </div>
                <Badge
                  variant={
                    r.level === "BEGINNER"
                      ? "beginner"
                      : r.level === "INTERMEDIATE"
                      ? "intermediate"
                      : r.level === "ADVANCED"
                      ? "advanced"
                      : "info"
                  }
                  label={r.level}
                  small
                />
              </li>
            ))}
          </ul>
        )}
      </Card>

      <div className="grid grid-cols-3 gap-3">
        {[
          { label: t("quickLinksMemberships"), href: "/student/memberships" },
          { label: t("quickLinksEnrollments"), href: "/student/enrollments" },
          { label: t("quickLinksClasses"), href: "/student/classes" },
        ].map(({ label, href }) => (
          <Button key={href} variant="outline" asChild>
            <Link href={href}>{label}</Link>
          </Button>
        ))}
      </div>
    </div>
  );
}
```

---

## Task 5 — tsc gate + commit dashboards

**Files:** no file changes

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors. If errors appear, fix them before continuing.

- [ ] **Step 2: Commit**

```bash
git add web/src/app/\(dashboard\)/admin/dashboard/page.tsx \
        web/src/app/\(dashboard\)/manager/dashboard/page.tsx \
        web/src/app/\(dashboard\)/professor/dashboard/page.tsx \
        web/src/app/\(dashboard\)/superadmin/dashboard/page.tsx \
        web/src/app/\(dashboard\)/student/dashboard/page.tsx \
        web/messages/en.json \
        web/messages/es.json
git commit -m "refactor(pages): migrate dashboard pages to design tokens"
```

---

## Task 6 — Migrate simple list pages: students, professors, programs, tenants, admins

**Files:**
- Modify: `web/src/app/(dashboard)/students/page.tsx`
- Modify: `web/src/app/(dashboard)/professors/page.tsx`
- Modify: `web/src/app/(dashboard)/programs/page.tsx`
- Modify: `web/src/app/(dashboard)/tenants/page.tsx`
- Modify: `web/src/app/(dashboard)/admins/page.tsx`

Pattern for pages with CTA: add `Button` import, replace `<Link className="inline-flex ... bg-blue-600 ...">` with `<Button variant="volt" asChild><Link>`. Pattern for all: `text-2xl font-bold text-gray-900` → `text-[26px] font-extrabold tracking-[-0.02em] text-k-dark`.

- [ ] **Step 1: Replace students/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import StudentList from "@/components/students/StudentList";

export const metadata = {
  title: "Students - Klasio",
};

export default async function StudentsPage() {
  const t = await getTranslations("students");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href="/students/new">+ {t("addButton")}</Link>
        </Button>
      </div>

      <StudentList />
    </div>
  );
}
```

- [ ] **Step 2: Replace professors/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import ProfessorList from "@/components/professors/ProfessorList";

export const metadata = {
  title: "Professors - Klasio",
};

export default async function ProfessorsPage() {
  const t = await getTranslations("professors");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>

      <ProfessorList />
    </div>
  );
}
```

- [ ] **Step 3: Replace programs/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ProgramList from "@/components/programs/ProgramList";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("pageTitle")} - Klasio` };
}

export default async function ProgramsPage() {
  const t = await getTranslations("programs");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href="/programs/new">+ {t("createButton")}</Link>
        </Button>
      </div>

      <ProgramList />
    </div>
  );
}
```

- [ ] **Step 4: Replace tenants/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import TenantList from "@/components/tenants/TenantList";

export const metadata = {
  title: "Tenants - Klasio",
};

export default async function TenantsPage() {
  const t = await getTranslations("tenants");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href="/tenants/new">+ {t("createButton")}</Link>
        </Button>
      </div>

      <TenantList />
    </div>
  );
}
```

- [ ] **Step 5: Replace admins/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import AdminList from "@/components/admins/AdminList";

export const metadata = {
  title: "Admins - Klasio",
};

export default async function AdminsPage() {
  const t = await getTranslations("admins");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>

      <AdminList />
    </div>
  );
}
```

---

## Task 7 — Migrate list pages: managers, payment-proofs, notifications

**Files:**
- Modify: `web/src/app/(dashboard)/managers/page.tsx`
- Modify: `web/src/app/(dashboard)/payment-proofs/page.tsx`
- Modify: `web/src/app/(dashboard)/notifications/page.tsx`

`managers/page.tsx`: strip the duplicate `p-6 max-w-7xl mx-auto` outer wrapper — layout already owns padding.
`notifications/page.tsx`: strip `<main className="p-6">`, replace with `<div>`.

- [ ] **Step 1: Replace managers/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import ManagerList from "@/components/managers/ManagerList";

export const metadata = {
  title: "Managers - Klasio",
};

export default async function ManagersPage() {
  const t = await getTranslations("managers");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>
      <ManagerList />
    </div>
  );
}
```

- [ ] **Step 2: Replace payment-proofs/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import { ProofQueue } from "@/components/payment-proofs/ProofQueue";

export default async function PaymentProofsPage() {
  const t = await getTranslations("paymentProofs");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          {t("pageSubtitle")}
        </p>
      </div>
      <ProofQueue />
    </div>
  );
}
```

- [ ] **Step 3: Replace notifications/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import NotificationList from "@/components/notifications/NotificationList";

export default async function NotificationsPage() {
  const t = await getTranslations("notifications");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>
      <NotificationList />
    </div>
  );
}
```

---

## Task 8 — Migrate programs/[id]/classes list page

**Files:**
- Modify: `web/src/app/(dashboard)/programs/[id]/classes/page.tsx`

Server component. Adds back button (to program detail `/programs/${id}`) + mono breadcrumb. Migrates heading h1 and blue-600 CTA to volt Button. Needs `getTranslations("common")` for back label.

- [ ] **Step 1: Replace programs/[id]/classes/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ClassList from "@/components/classes/ClassList";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("classesPageTitle")} - Klasio` };
}

export default async function ClassesPage({ params }: { params: Promise<{ id: string }> }) {
  const t = await getTranslations("programs");
  const tCommon = await getTranslations("common");
  const { id } = await params;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("classesPageBreadcrumb")}</span>
        </nav>
      </div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("classesPageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href={`/programs/${id}/classes/new`}>+ {t("classesAddButton")}</Link>
        </Button>
      </div>
      <ClassList programId={id} />
    </div>
  );
}
```

---

## Task 9 — Migrate classes/page.tsx (complex client component)

**Files:**
- Modify: `web/src/app/(dashboard)/classes/page.tsx`

Client component with expand/collapse table. Changes:
- Import `Input, Select, Button` from `@/components/ui`
- Replace `<input ... className="rounded-md border border-gray-300 ... w-48">` → `<Input ... className="w-48">`
- Replace both `<select ... className="rounded-md border border-gray-300 ...">` → `<Select ... className="w-auto">`
- `text-gray-900` h1 → `text-k-dark`
- `text-gray-700` labels → `text-k-subtle`
- `text-gray-500` loading/empty/cells → `text-k-muted`
- `bg-blue-50` expanded row → `bg-k-bg`
- `text-blue-500` chevron → `text-k-volt`
- `text-gray-400` chevron default → `text-k-muted`
- `bg-gray-50` thead → `bg-k-bg`; `text-gray-500` th → `text-k-muted`
- `bg-white` tbody → `bg-k-surface`; `text-gray-200` border → `border-k-border`
- `hover:bg-gray-50` rows → `hover:bg-k-bg`
- `text-gray-900` cell name → `text-k-dark`; `text-gray-500` cells → `text-k-muted`
- `hover:text-blue-600` links → `hover:text-k-subtle`
- `text-gray-400 italic` unassigned → `text-k-muted italic`
- Pagination `<button ... bg-white px-3 py-2 ... border-gray-300 hover:bg-gray-50>` → `<Button variant="outline">`
- `text-gray-700` pagination summary → `text-k-subtle`
- `rounded-md bg-red-50 ... text-red-700 border-red-200` error → `rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 text-k-danger-text`
- Keep `border-gray-200` on the outer table wrapper → `border-k-border`
- Keep `divide-gray-200` → `divide-k-border`

- [ ] **Step 1: Replace classes/page.tsx**

```tsx
"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ChevronDown, ChevronRight } from "lucide-react";
import { ClassLevel, ClassStatus } from "@/lib/types/programClass";
import { useAllClasses } from "@/hooks/useProgramClasses";
import { useAuth } from "@/hooks/useAuth";
import { primaryRole } from "@/lib/types/auth";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
import ClassTypeBadge from "@/components/classes/ClassTypeBadge";
import ClassStatusBadge from "@/components/classes/ClassStatusBadge";
import ClassRosterPanel from "@/components/attendance/ClassRosterPanel";
import { Button, Input, Select } from "@/components/ui";

export default function AllClassesPage() {
  const t = useTranslations("classes");
  const tPagination = useTranslations("pagination");
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [levelFilter, setLevelFilter] = useState<ClassLevel | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<ClassStatus | undefined>(undefined);
  const [programNameInput, setProgramNameInput] = useState("");
  const [programNameFilter, setProgramNameFilter] = useState<string | undefined>(undefined);
  const [expandedClassId, setExpandedClassId] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const SIZE = 20;

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setProgramNameFilter(programNameInput.trim() || undefined);
      setPage(0);
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [programNameInput]);

  const { classes, totalPages, totalElements, loading, error } = useAllClasses(
    page,
    SIZE,
    levelFilter,
    statusFilter,
    programNameFilter
  );

  function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  function toggleExpand(classId: string) {
    setExpandedClassId((prev) => (prev === classId ? null : classId));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>

      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <label htmlFor="programNameFilter" className="text-sm font-medium text-k-subtle">
              {t("filterProgramLabel")}
            </label>
            <Input
              id="programNameFilter"
              type="text"
              value={programNameInput}
              onChange={(e) => setProgramNameInput(e.target.value)}
              placeholder={t("filterProgramPlaceholder")}
              className="w-48"
            />
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="levelFilter" className="text-sm font-medium text-k-subtle">
              {t("filterLevelLabel")}
            </label>
            <Select
              id="levelFilter"
              value={levelFilter ?? ""}
              onChange={(e) => {
                setLevelFilter(e.target.value === "" ? undefined : (e.target.value as ClassLevel));
                setPage(0);
              }}
              className="w-auto"
            >
              <option value="">{t("filterAll")}</option>
              <option value="BEGINNER">{t("filterBeginnerOption")}</option>
              <option value="INTERMEDIATE">{t("filterIntermediateOption")}</option>
              <option value="ADVANCED">{t("filterAdvancedOption")}</option>
            </Select>
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="statusFilter" className="text-sm font-medium text-k-subtle">
              {t("filterStatusLabel")}
            </label>
            <Select
              id="statusFilter"
              value={statusFilter ?? ""}
              onChange={(e) => {
                setStatusFilter(e.target.value === "" ? undefined : (e.target.value as ClassStatus));
                setPage(0);
              }}
              className="w-auto"
            >
              <option value="">{t("filterAll")}</option>
              <option value="ACTIVE">{t("filterActive")}</option>
              <option value="INACTIVE">{t("filterInactive")}</option>
            </Select>
          </div>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-k-muted">{t("listLoading")}</div>
        )}

        {error && (
          <div
            className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
            role="alert"
          >
            {error}
          </div>
        )}

        {!loading && !error && classes.length === 0 && (
          <div className="text-center py-8 text-sm text-k-muted">{t("listEmpty")}</div>
        )}

        {!loading && !error && classes.length > 0 && (
          <>
            <div className="overflow-x-auto rounded-lg border border-k-border">
              <table className="min-w-full divide-y divide-k-border">
                <thead className="bg-k-bg">
                  <tr>
                    <th className="w-10 px-3 py-3" aria-label="Expand" />
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colName")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colProgram")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colLevel")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colType")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colProfessor")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colMaxStudents")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colStatus")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colCreated")}
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-k-surface divide-y divide-k-border">
                  {classes.map((c) => (
                    <React.Fragment key={c.id}>
                      <tr
                        className={`hover:bg-k-bg cursor-pointer ${expandedClassId === c.id ? "bg-k-bg" : ""}`}
                        onClick={() => toggleExpand(c.id)}
                      >
                        <td className="px-3 py-4 text-center text-k-muted">
                          {expandedClassId === c.id ? (
                            <ChevronDown className="w-4 h-4 mx-auto text-k-volt" />
                          ) : (
                            <ChevronRight className="w-4 h-4 mx-auto" />
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-k-dark">
                          <Link
                            href={`/programs/${c.programId}/classes/${c.id}`}
                            className="hover:text-k-subtle hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {c.name}
                          </Link>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          <Link
                            href={`/programs/${c.programId}/classes`}
                            className="hover:text-k-subtle hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {c.programName ?? c.programId}
                          </Link>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassLevelBadge level={c.level} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassTypeBadge type={c.type} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          {c.professorName ?? <span className="text-k-muted italic">{t("colUnassigned")}</span>}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          {c.maxStudents}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassStatusBadge status={c.status} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          {formatDate(c.createdAt)}
                        </td>
                      </tr>

                      {expandedClassId === c.id && (
                        <tr>
                          <td colSpan={9} className="p-0">
                            <ClassRosterPanel classId={c.id} userRole={user ? primaryRole(user.roles) : undefined} />
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between border-t border-k-border pt-4">
              <p className="text-sm text-k-subtle">
                {tPagination("summary", { current: page + 1, total: totalPages, count: totalElements })}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  {tPagination("previous")}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                >
                  {tPagination("next")}
                </Button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
```

---

## Task 10 — Migrate plans/page.tsx

**Files:**
- Modify: `web/src/app/(dashboard)/plans/page.tsx`

Changes: import `Select` from ui; replace `<select>` with `<Select className="w-auto">`; heading gray-900→k-dark; label gray-700→k-subtle; table: thead bg-gray-50→bg-k-bg, th text-gray-500→text-k-muted, tbody bg-white→bg-k-surface, dividers; plan link text-blue-600→text-k-subtle hover:text-k-dark; loading/empty/error text gray-500→k-muted.

- [ ] **Step 1: Replace plans/page.tsx**

```tsx
"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useAllPlans } from "@/hooks/usePlans";
import { Select } from "@/components/ui";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";

function formatCost(cost: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(cost);
}

export default function PlansPage() {
  const t = useTranslations("programs");
  const [statusFilter, setStatusFilter] = useState<string | undefined>(
    undefined
  );
  const { plans, loading, error } = useAllPlans(statusFilter);

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : value);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("allPlansPageTitle")}</h1>
      </div>

      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <label
            htmlFor="statusFilter"
            className="text-sm font-medium text-k-subtle"
          >
            {t("filterStatusLabel")}
          </label>
          <Select
            id="statusFilter"
            value={statusFilter ?? ""}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="w-auto"
          >
            <option value="">{t("filterAll")}</option>
            <option value="ACTIVE">{t("filterActive")}</option>
            <option value="INACTIVE">{t("filterInactive")}</option>
          </Select>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-k-muted">
            {t("allPlansLoading")}
          </div>
        )}

        {error && (
          <div
            className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
            role="alert"
          >
            {error}
          </div>
        )}

        {!loading && !error && plans.length === 0 && (
          <div className="text-center py-8 text-sm text-k-muted">
            {t("allPlansEmpty")}
          </div>
        )}

        {!loading && !error && plans.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-k-border">
              <thead className="bg-k-bg">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("allPlansColPlanName")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("allPlansColProgram")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColModality")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColCost")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColManager")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColStatus")}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-k-surface divide-y divide-k-border">
                {plans.map((plan) => (
                  <tr key={plan.id} className="hover:bg-k-bg">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-k-dark">
                      <Link
                        href={`/programs/${plan.programId ?? ""}/plans/${plan.id}`}
                        className="text-k-subtle hover:text-k-dark hover:underline"
                      >
                        {plan.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {plan.programName ?? "-"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {plan.modality === "HOURS_BASED"
                        ? t("modalityHoursBased")
                        : plan.modality === "CLASSES_PER_WEEK"
                        ? t("modalityClassesPerWeek")
                        : plan.modality}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {formatCost(plan.cost)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {plan.managerName ?? plan.managerId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <ProgramStatusBadge status={plan.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
```

---

## Task 11 — tsc gate + commit list pages

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors.

- [ ] **Step 2: Commit**

```bash
git add \
  web/src/app/\(dashboard\)/students/page.tsx \
  web/src/app/\(dashboard\)/professors/page.tsx \
  web/src/app/\(dashboard\)/programs/page.tsx \
  web/src/app/\(dashboard\)/tenants/page.tsx \
  web/src/app/\(dashboard\)/admins/page.tsx \
  web/src/app/\(dashboard\)/managers/page.tsx \
  web/src/app/\(dashboard\)/payment-proofs/page.tsx \
  web/src/app/\(dashboard\)/notifications/page.tsx \
  "web/src/app/(dashboard)/programs/[id]/classes/page.tsx" \
  web/src/app/\(dashboard\)/classes/page.tsx \
  web/src/app/\(dashboard\)/plans/page.tsx
git commit -m "refactor(pages): migrate list pages to design tokens"
```

---

## Task 12 — Migrate detail pages: students/[id], professors/[id], tenants/[slug]

**Files:**
- Modify: `web/src/app/(dashboard)/students/[id]/page.tsx`
- Modify: `web/src/app/(dashboard)/professors/[id]/page.tsx`
- Modify: `web/src/app/(dashboard)/tenants/[slug]/page.tsx`

All three are client components. Changes: add `Button` import and `useTranslations` (for students/professors which didn't have it); replace hardcoded breadcrumb nav with back button + mono breadcrumb block; migrate error div to danger tokens; migrate loading text to `text-k-muted`.

- [ ] **Step 1: Replace students/[id]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import StudentDetail from "@/components/students/StudentDetail";
import { useStudentDetail } from "@/hooks/useStudents";
import { Button } from "@/components/ui";

interface StudentDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function StudentDetailPage({ params }: StudentDetailPageProps) {
  const tCommon = useTranslations("common");
  const tStudents = useTranslations("students");
  const { id } = use(params);
  const { student, loading, error, refetch } = useStudentDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/students">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">
            {student ? `${student.firstName} ${student.lastName}` : id}
          </span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading student details...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {student && <StudentDetail student={student} onStatusChanged={refetch} />}
    </div>
  );
}
```

- [ ] **Step 2: Replace professors/[id]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProfessorDetail from "@/components/professors/ProfessorDetail";
import { useProfessorDetail } from "@/hooks/useProfessors";
import { Button } from "@/components/ui";

interface ProfessorDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ProfessorDetailPage({ params }: ProfessorDetailPageProps) {
  const tCommon = useTranslations("common");
  const tProfessors = useTranslations("professors");
  const { id } = use(params);
  const { professor, loading, error, refetch } = useProfessorDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/professors">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/professors" className="hover:text-k-subtle">{tProfessors("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">
            {professor ? `${professor.firstName} ${professor.lastName}` : id}
          </span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading professor details...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {professor && <ProfessorDetail professor={professor} onStatusChanged={refetch} />}
    </div>
  );
}
```

- [ ] **Step 3: Replace tenants/[slug]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import TenantDetail from "@/components/tenants/TenantDetail";
import { useTenantDetail } from "@/hooks/useTenants";
import { Button } from "@/components/ui";

interface TenantSlugPageProps {
  params: Promise<{ slug: string }>;
}

export default function TenantSlugPage({ params }: TenantSlugPageProps) {
  const tCommon = useTranslations("common");
  const tTenants = useTranslations("tenants");
  const { slug } = use(params);
  const { tenant, loading, error, refetch } = useTenantDetail(slug);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/tenants">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/tenants" className="hover:text-k-subtle">{tTenants("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{tenant?.name ?? slug}</span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading tenant details...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {tenant && <TenantDetail tenant={tenant} onDeactivated={refetch} />}
    </div>
  );
}
```

---

## Task 13 — Migrate detail page: programs/[id]

**Files:**
- Modify: `web/src/app/(dashboard)/programs/[id]/page.tsx`

Already has `useTranslations("programs")`. Add `Button` import + back button + mono breadcrumb. Existing `<nav>` replaced with the new two-element block.

- [ ] **Step 1: Replace programs/[id]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProgramDetail from "@/components/programs/ProgramDetail";
import { useProgramDetail } from "@/hooks/usePrograms";
import { Button } from "@/components/ui";

interface ProgramDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ProgramDetailPage({ params }: ProgramDetailPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id } = use(params);
  const { program, loading, error, refetch } = useProgramDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/programs">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{program?.name ?? id}</span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("detailLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {program && <ProgramDetail program={program} onStatusChanged={refetch} />}
    </div>
  );
}
```

---

## Task 14 — Migrate detail pages: programs/[id]/classes/[classId] and programs/[id]/plans/[planId]

**Files:**
- Modify: `web/src/app/(dashboard)/programs/[id]/classes/[classId]/page.tsx`
- Modify: `web/src/app/(dashboard)/programs/[id]/plans/[planId]/page.tsx`

`classes/[classId]` gets back button to `/programs/${id}/classes` + two-segment mono breadcrumb. `plans/[planId]` is the most complex page: back button to `/programs/${id}`, three-segment breadcrumb, migrated dl/dd styles, Card-like header, Button primitives for edit/deactivate/reactivate, feedback state danger/success tokens, schedule entry bg.

- [ ] **Step 1: Replace programs/[id]/classes/[classId]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ClassDetail from "@/components/classes/ClassDetail";
import { useProgramClassDetail } from "@/hooks/useProgramClasses";
import { Button } from "@/components/ui";

interface ClassDetailPageProps {
  params: Promise<{ id: string; classId: string }>;
}

export default function ClassDetailPage({ params }: ClassDetailPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, classId } = use(params);
  const { programClass, loading, error, refetch } = useProgramClassDetail(id, classId);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/classes`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}/classes`} className="hover:text-k-subtle">{t("classDetailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">
            {programClass ? programClass.name : classId}
          </span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("classDetailLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {programClass && (
        <ClassDetail
          programId={id}
          programClass={programClass}
          onChanged={refetch}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 2: Replace programs/[id]/plans/[planId]/page.tsx**

Key changes beyond back/breadcrumb:
- `bg-white shadow rounded-lg overflow-hidden` card → `bg-k-surface border border-k-border rounded-k-lg overflow-hidden`
- `border-b border-gray-200` → `border-b border-k-line`
- `text-gray-900` h2 → `text-k-dark`; `text-gray-500` subtitle → `text-k-muted`
- `bg-white border-gray-300 text-gray-700 hover:bg-gray-50` edit link → `<Button variant="outline" asChild>`
- Feedback: success `bg-green-50 text-green-700 border-green-200` → `bg-k-volt/10 text-k-volt-text border border-k-volt/30`; error → danger tokens
- `text-gray-500` dt → `font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted`; `text-gray-900` dd → `text-sm font-medium text-k-dark`
- `bg-gray-50 rounded-md` schedule entries → `bg-k-bg rounded-k-sm`
- `text-gray-500` section heading → `font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted`
- `bg-gray-50` footer → `bg-k-bg`
- `bg-red-600 hover:bg-red-700` deactivate button → `<Button variant="danger">`
- `bg-green-600 hover:bg-green-700` reactivate button → `<Button variant="volt">`

```tsx
"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useProgramDetail } from "@/hooks/usePrograms";
import { useProgramPlanDetail } from "@/hooks/useProgramPlans";
import { api, ApiError } from "@/lib/api";
import { Button } from "@/components/ui";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";

interface PlanDetailPageProps {
  params: Promise<{ id: string; planId: string }>;
}

export default function PlanDetailPage({ params }: PlanDetailPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, planId } = use(params);
  const { program } = useProgramDetail(id);
  const { plan, loading, error, refetch } = useProgramPlanDetail(id, planId);

  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(`/programs/${id}/plans/${planId}/${action}`);
      const label = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: t("planDetailSuccessFeedback", { action: label }),
      });
      refetch();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : t("planDetailErrorFeedback", { action });
      setFeedback({ type: "error", message });
    } finally {
      setActionLoading(false);
    }
  }

  function formatCost(cost: number): string {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "COP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(cost);
  }

  function formatDate(dateString: string | null): string {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{plan?.name ?? t("planDetailBreadcrumb")}</span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("planDetailLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {feedback && (
        <div
          className={`rounded-k-sm p-4 text-sm border mb-4 ${
            feedback.type === "success"
              ? "bg-k-volt/10 text-k-volt-text border-k-volt/30"
              : "bg-k-danger-bg border-k-danger-text/30 text-k-danger-text"
          }`}
          role="alert"
        >
          {feedback.message}
        </div>
      )}

      {plan && (
        <div className="space-y-6">
          <div className="bg-k-surface border border-k-border rounded-k-lg overflow-hidden">
            <div className="px-6 py-5 border-b border-k-line flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-k-dark">
                  {plan.name}
                </h2>
                <p className="text-sm text-k-muted mt-1">
                  {formatCost(plan.cost)}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <ProgramStatusBadge status={plan.status} />
                {plan.status === "ACTIVE" && (
                  <Button variant="outline" size="sm" asChild>
                    <Link href={`/programs/${id}/plans/${planId}/edit`}>
                      {t("planDetailEditButton")}
                    </Link>
                  </Button>
                )}
              </div>
            </div>

            <div className="px-6 py-5">
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">{t("planDetailModality")}</dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {plan.modality === "HOURS_BASED"
                      ? t("modalityHoursBased")
                      : plan.modality === "CLASSES_PER_WEEK"
                        ? t("modalityClassesPerWeek")
                        : plan.modality}
                  </dd>
                </div>

                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">{t("planDetailManager")}</dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {plan.managerName ?? plan.managerId}
                  </dd>
                </div>

                {plan.hours != null && (
                  <div>
                    <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">{t("planDetailHours")}</dt>
                    <dd className="mt-1 text-sm font-medium text-k-dark">
                      {plan.hours}
                    </dd>
                  </div>
                )}

                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                    {t("planDetailCreatedAt")}
                  </dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {formatDate(plan.createdAt)}
                  </dd>
                </div>

                <div>
                  <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                    {t("planDetailCreatedBy")}
                  </dt>
                  <dd className="mt-1 text-sm font-medium text-k-dark">
                    {plan.createdBy}
                  </dd>
                </div>

                {plan.updatedAt && (
                  <>
                    <div>
                      <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                        {t("planDetailLastUpdated")}
                      </dt>
                      <dd className="mt-1 text-sm font-medium text-k-dark">
                        {formatDate(plan.updatedAt)}
                      </dd>
                    </div>
                    <div>
                      <dt className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
                        {t("planDetailUpdatedBy")}
                      </dt>
                      <dd className="mt-1 text-sm font-medium text-k-dark">
                        {plan.updatedBy}
                      </dd>
                    </div>
                  </>
                )}
              </dl>
            </div>

            {plan.scheduleEntries.length > 0 && (
              <div className="px-6 py-5 border-t border-k-line">
                <h3 className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-3">
                  {t("planDetailScheduleTitle")}
                </h3>
                <div className="space-y-2">
                  {plan.scheduleEntries.map((entry, index) => (
                    <div
                      key={index}
                      className="flex items-center gap-3 text-sm text-k-dark bg-k-bg rounded-k-sm px-3 py-2"
                    >
                      <span className="font-medium w-24">
                        {{
                          MONDAY: t("dayMonday"),
                          TUESDAY: t("dayTuesday"),
                          WEDNESDAY: t("dayWednesday"),
                          THURSDAY: t("dayThursday"),
                          FRIDAY: t("dayFriday"),
                          SATURDAY: t("daySaturday"),
                          SUNDAY: t("daySunday"),
                        }[entry.dayOfWeek] ?? entry.dayOfWeek}
                      </span>
                      <span>
                        {entry.startTime} - {entry.endTime}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="px-6 py-4 border-t border-k-line bg-k-bg">
              {plan.status === "ACTIVE" && (
                <Button
                  variant="danger"
                  onClick={() => handleStatusAction("deactivate")}
                  disabled={actionLoading}
                >
                  {actionLoading ? t("planDetailDeactivatingButton") : t("planDetailDeactivateButton")}
                </Button>
              )}
              {plan.status === "INACTIVE" && (
                <Button
                  variant="volt"
                  onClick={() => handleStatusAction("reactivate")}
                  disabled={actionLoading}
                >
                  {actionLoading ? t("planDetailReactivatingButton") : t("planDetailReactivateButton")}
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
```

---

## Task 15 — Migrate membership detail pages + students/[id]/memberships list

**Files:**
- Modify: `web/src/app/(dashboard)/students/[id]/memberships/[membershipId]/page.tsx`
- Modify: `web/src/app/(dashboard)/student/memberships/[membershipId]/page.tsx`
- Modify: `web/src/app/(dashboard)/students/[id]/memberships/page.tsx`

All three are client components. The first two get back button + mono breadcrumb + danger error state. The third (admin memberships list) gets a back button + breadcrumb, migrates the filter pills and the blue-600 CTA to Button volt.

- [ ] **Step 1: Replace students/[id]/memberships/[membershipId]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMembershipDetail } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import { useAuth } from "@/hooks/useAuth";
import MembershipDetail from "@/components/memberships/MembershipDetail";
import { PaymentProofTimeline } from "@/components/payment-proofs/PaymentProofTimeline";
import { Button } from "@/components/ui";

interface Props {
  params: Promise<{ id: string; membershipId: string }>;
}

export default function MembershipDetailPage({ params }: Props) {
  const { id: studentId, membershipId } = use(params);
  const t = useTranslations("memberships");
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { membership, loading, error, refetch } = useMembershipDetail(membershipId);
  const { student } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;
  const { user } = useAuth();

  const isAdmin = (user?.roles.includes("ADMIN") || user?.roles.includes("SUPERADMIN")) ?? false;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${studentId}/memberships`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}`} className="hover:text-k-subtle">{studentName}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}/memberships`} className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("detailBreadcrumbDetail")}</span>
        </nav>
      </div>

      {loading && (
        <div className="py-8 text-center text-sm text-k-muted">{t("detailLoading")}</div>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {membership && (
        <div className="space-y-6">
          <MembershipDetail
            membership={membership}
            onRefresh={refetch}
            isAdmin={isAdmin}
          />
          <PaymentProofTimeline membershipId={membershipId} membershipStatus={membership.status} />
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Replace student/memberships/[membershipId]/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMembershipDetail } from "@/hooks/useMemberships";
import MembershipDetail from "@/components/memberships/MembershipDetail";
import { PaymentProofPanel } from "@/components/payment-proofs/PaymentProofPanel";
import { PaymentProofTimeline } from "@/components/payment-proofs/PaymentProofTimeline";
import { Button } from "@/components/ui";

interface Props {
  params: Promise<{ membershipId: string }>;
}

export default function StudentMembershipDetailPage({ params }: Props) {
  const { membershipId } = use(params);
  const t = useTranslations("memberships");
  const tPage = useTranslations("studentMembershipsPage");
  const tCommon = useTranslations("common");
  const { membership, loading, error, refetch } = useMembershipDetail(membershipId);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/student/memberships">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/student/memberships" className="hover:text-k-subtle">{tPage("title")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("detailBreadcrumbDetail")}</span>
        </nav>
      </div>

      {loading && (
        <div className="py-8 text-center text-sm text-k-muted">{t("detailLoading")}</div>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {membership && (
        <div className="space-y-6">
          <MembershipDetail
            membership={membership}
            onRefresh={refetch}
            isAdmin={false}
            isManager={false}
          />
          {(membership.status === "PENDING_PAYMENT" ||
            membership.status === "PENDING_PAYMENT_VALIDATION" ||
            membership.status === "EXPIRED") && (
            <PaymentProofPanel membershipId={membershipId} membershipStatus={membership.status} />
          )}
          <PaymentProofTimeline membershipId={membershipId} membershipStatus={membership.status} />
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Replace students/[id]/memberships/page.tsx**

```tsx
"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMemberships, useMembershipActions } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import MembershipList from "@/components/memberships/MembershipList";
import { Button } from "@/components/ui";

interface Props {
  params: Promise<{ id: string }>;
}

export default function StudentMembershipsPage({ params }: Props) {
  const { id: studentId } = use(params);
  const t = useTranslations("memberships");
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { memberships, loading, error, refetch } = useMemberships(studentId);
  const { student } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;
  const { activateMembership, validatePayment, loading: actionLoading, error: actionError } =
    useMembershipActions();

  const [statusFilter, setStatusFilter] = useState<string>("");

  const filtered = statusFilter
    ? memberships.filter((m) => m.status === statusFilter)
    : memberships;

  async function handleActivate(id: string) {
    await activateMembership(id);
    refetch();
  }

  async function handleValidatePayment(id: string) {
    await validatePayment(id, { activateDirectly: true });
    refetch();
  }

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${studentId}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}`} className="hover:text-k-subtle">{studentName}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("detailBreadcrumb")}</span>
        </nav>
      </div>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("adminPageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href={`/students/${studentId}/memberships/new`}>+ {t("adminNewButton")}</Link>
        </Button>
      </div>

      <div className="mb-4 flex gap-2">
        {["", "ACTIVE", "INACTIVE", "EXPIRED", "PENDING_PAYMENT", "PENDING_PAYMENT_VALIDATION", "PENDING_MANAGER_ACTIVATION"].map(
          (s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                statusFilter === s
                  ? "bg-k-dark text-white"
                  : "bg-k-bg text-k-subtle hover:bg-k-border"
              }`}
            >
              {s === "" ? t("adminFilterAll") : s.replace(/_/g, " ")}
            </button>
          )
        )}
      </div>

      {(actionError || error) && (
        <div className="mb-4 rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {actionError ?? error}
        </div>
      )}

      {loading ? (
        <div className="py-8 text-center text-sm text-k-muted">{t("adminLoading")}</div>
      ) : (
        <MembershipList
          memberships={filtered}
          studentId={studentId}
          onActivate={actionLoading ? undefined : handleActivate}
          onValidatePayment={actionLoading ? undefined : handleValidatePayment}
        />
      )}
    </div>
  );
}
```

---

## Task 16 — tsc gate + commit detail pages

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors.

- [ ] **Step 2: Commit**

```bash
git add \
  "web/src/app/(dashboard)/students/[id]/page.tsx" \
  "web/src/app/(dashboard)/professors/[id]/page.tsx" \
  "web/src/app/(dashboard)/tenants/[slug]/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/classes/[classId]/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/plans/[planId]/page.tsx" \
  "web/src/app/(dashboard)/students/[id]/memberships/[membershipId]/page.tsx" \
  "web/src/app/(dashboard)/student/memberships/[membershipId]/page.tsx" \
  "web/src/app/(dashboard)/students/[id]/memberships/page.tsx"
git commit -m "refactor(pages): migrate detail pages to design tokens"
```

---

## Task 17 — Migrate form pages: students/new + students/[id]/edit

**Files:**
- Modify: `web/src/app/(dashboard)/students/new/page.tsx`
- Modify: `web/src/app/(dashboard)/students/[id]/edit/page.tsx`

Pattern for all form pages: add back button + mono breadcrumb (same spec Section 4 pattern); heading `text-2xl font-bold text-gray-900` → `text-[26px] font-extrabold tracking-[-0.02em] text-k-dark`.

`students/new` is a server component. It has no form wrapper (StudentForm renders its own container). No Card wrapper needed — the form component owns its UI.

`students/[id]/edit` is a client component. Has loading/error states — migrate both to k-* tokens.

- [ ] **Step 1: Replace students/new/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import StudentForm from "@/components/students/StudentForm";

export const metadata = {
  title: "Add Student - Klasio",
};

export default async function NewStudentPage() {
  const tStudents = await getTranslations("students");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/students">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{tStudents("addButton")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">
        {tStudents("addButton")}
      </h1>

      <StudentForm />
    </div>
  );
}
```

- [ ] **Step 2: Replace students/[id]/edit/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import StudentForm from "@/components/students/StudentForm";
import { useStudentDetail } from "@/hooks/useStudents";
import { Button } from "@/components/ui";

interface EditStudentPageProps {
  params: Promise<{ id: string }>;
}

export default function EditStudentPage({ params }: EditStudentPageProps) {
  const { id } = use(params);
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { student, loading, error } = useStudentDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${id}`} className="hover:text-k-subtle">
            {student ? `${student.firstName} ${student.lastName}` : id}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">Edit</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">Edit Student</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading student...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {student && <StudentForm student={student} />}
    </div>
  );
}
```

---

## Task 18 — Migrate form pages: professors/new + professors/[id]/edit

**Files:**
- Modify: `web/src/app/(dashboard)/professors/new/page.tsx`
- Modify: `web/src/app/(dashboard)/professors/[id]/edit/page.tsx`

Same form page pattern as Task 17.

- [ ] **Step 1: Replace professors/new/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ProfessorForm from "@/components/professors/ProfessorForm";

export const metadata = {
  title: "Add Professor - Klasio",
};

export default async function NewProfessorPage() {
  const tProfessors = await getTranslations("professors");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/professors">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/professors" className="hover:text-k-subtle">{tProfessors("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">Add Professor</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">
        Add Professor
      </h1>

      <ProfessorForm />
    </div>
  );
}
```

- [ ] **Step 2: Replace professors/[id]/edit/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProfessorForm from "@/components/professors/ProfessorForm";
import { useProfessorDetail } from "@/hooks/useProfessors";
import { Button } from "@/components/ui";

interface EditProfessorPageProps {
  params: Promise<{ id: string }>;
}

export default function EditProfessorPage({ params }: EditProfessorPageProps) {
  const { id } = use(params);
  const tProfessors = useTranslations("professors");
  const tCommon = useTranslations("common");
  const { professor, loading, error } = useProfessorDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/professors/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/professors" className="hover:text-k-subtle">{tProfessors("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/professors/${id}`} className="hover:text-k-subtle">
            {professor ? `${professor.firstName} ${professor.lastName}` : id}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">Edit</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">Edit Professor</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading professor...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {professor && <ProfessorForm professor={professor} />}
    </div>
  );
}
```

---

## Task 19 — Migrate form pages: programs/new + programs/[id]/edit + tenants/new

**Files:**
- Modify: `web/src/app/(dashboard)/programs/new/page.tsx`
- Modify: `web/src/app/(dashboard)/programs/[id]/edit/page.tsx`
- Modify: `web/src/app/(dashboard)/tenants/new/page.tsx`

- [ ] **Step 1: Replace programs/new/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ProgramForm from "@/components/programs/ProgramForm";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("newPageTitle")} - Klasio` };
}

export default async function NewProgramPage() {
  const t = await getTranslations("programs");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/programs">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("newBreadcrumbNew")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">
        {t("newPageTitle")}
      </h1>

      <ProgramForm />
    </div>
  );
}
```

- [ ] **Step 2: Replace programs/[id]/edit/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProgramForm from "@/components/programs/ProgramForm";
import { useProgramDetail } from "@/hooks/usePrograms";
import { Button } from "@/components/ui";

interface EditProgramPageProps {
  params: Promise<{ id: string }>;
}

export default function EditProgramPage({ params }: EditProgramPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id } = use(params);
  const { program, loading, error } = useProgramDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("planEditBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">{t("editPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("editLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {program && <ProgramForm program={program} />}
    </div>
  );
}
```

- [ ] **Step 3: Replace tenants/new/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import TenantForm from "@/components/tenants/TenantForm";

export const metadata = {
  title: "Create New Tenant - Klasio",
};

export default async function NewTenantPage() {
  const tTenants = await getTranslations("tenants");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/tenants">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/tenants" className="hover:text-k-subtle">{tTenants("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">New</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">Create New Tenant</h1>

      <TenantForm />
    </div>
  );
}
```

---

## Task 20 — Migrate class and plan form pages (4 files)

**Files:**
- Modify: `web/src/app/(dashboard)/programs/[id]/classes/new/page.tsx`
- Modify: `web/src/app/(dashboard)/programs/[id]/classes/[classId]/edit/page.tsx`
- Modify: `web/src/app/(dashboard)/programs/[id]/plans/new/page.tsx`
- Modify: `web/src/app/(dashboard)/programs/[id]/plans/[planId]/edit/page.tsx`

`plans/new` and `plans/[planId]/edit` currently wrap their form in `<div className="bg-white shadow rounded-lg p-6">` — replace with `<Card padding="md">`. The other two (classes/new, classes/[classId]/edit) delegate rendering to `ClassForm` and don't have an inline wrapper.

- [ ] **Step 1: Replace programs/[id]/classes/new/page.tsx**

```tsx
import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ClassForm from "@/components/classes/ClassForm";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("classNewPageTitle")} - Klasio` };
}

export default async function NewClassPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const t = await getTranslations("programs");
  const tCommon = await getTranslations("common");
  const { id } = await params;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/classes`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href={`/programs/${id}/classes`} className="hover:text-k-subtle">{t("classDetailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("classNewBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-6">{t("classNewPageTitle")}</h1>

      <ClassForm programId={id} />
    </div>
  );
}
```

- [ ] **Step 2: Replace programs/[id]/classes/[classId]/edit/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui";
import ClassForm from "@/components/classes/ClassForm";
import { useProgramClassDetail } from "@/hooks/useProgramClasses";

interface EditClassPageProps {
  params: Promise<{ id: string; classId: string }>;
}

export default function EditClassPage({ params }: EditClassPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, classId } = use(params);
  const { programClass, loading, error } = useProgramClassDetail(id, classId);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/classes/${classId}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}/classes`} className="hover:text-k-subtle">{t("classDetailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}/classes/${classId}`} className="hover:text-k-subtle">
            {programClass ? programClass.name : classId}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("classEditBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">{t("classEditPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("classEditLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {programClass && (
        <ClassForm programId={id} programClass={programClass} />
      )}
    </div>
  );
}
```

- [ ] **Step 3: Replace programs/[id]/plans/new/page.tsx**

`<div className="bg-white shadow rounded-lg p-6">` → `<Card padding="md">`. Import `Card`.

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button, Card } from "@/components/ui";
import ProgramPlanForm from "@/components/programs/ProgramPlanForm";
import { useProgramDetail } from "@/hooks/usePrograms";

interface NewPlanPageProps {
  params: Promise<{ id: string }>;
}

export default function NewPlanPage({ params }: NewPlanPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id } = use(params);
  const { program, loading, error } = useProgramDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("planNewBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">{t("planNewPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("planNewLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {program && (
        <Card padding="md">
          <ProgramPlanForm programId={id} tenantId={program.tenantId} />
        </Card>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Replace programs/[id]/plans/[planId]/edit/page.tsx**

```tsx
"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button, Card } from "@/components/ui";
import ProgramPlanForm from "@/components/programs/ProgramPlanForm";
import { useProgramDetail } from "@/hooks/usePrograms";
import { useProgramPlanDetail } from "@/hooks/useProgramPlans";

interface EditPlanPageProps {
  params: Promise<{ id: string; planId: string }>;
}

export default function EditPlanPage({ params }: EditPlanPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, planId } = use(params);
  const { program, loading: programLoading } = useProgramDetail(id);
  const { plan, loading: planLoading, error } = useProgramPlanDetail(id, planId);

  const loading = programLoading || planLoading;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/plans/${planId}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}/plans/${planId}`} className="hover:text-k-subtle">
            {plan?.name ?? t("planDetailBreadcrumb")}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("planEditBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">{t("planEditPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("planEditLoadingText")}
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {plan && (
        <Card padding="md">
          <ProgramPlanForm
            programId={id}
            tenantId={program?.tenantId}
            plan={plan}
          />
        </Card>
      )}
    </div>
  );
}
```

---

## Task 21 — Migrate membership new form pages

**Files:**
- Modify: `web/src/app/(dashboard)/students/[id]/memberships/new/page.tsx`

Complex page with inline program selector and conditional form rendering. Changes:
- Add back button (to student's membership list)
- Heading gray-900→k-dark
- Nav breadcrumb gray-500→k-muted/k-subtle
- `bg-yellow-50 border-yellow-200 text-yellow-800` warning → `bg-k-warn-bg border border-k-warn-text/30 text-k-warn-text`
- `rounded-md border border-gray-200 p-6 bg-white` form wrapper → `<Card padding="md">`
- `text-sm font-medium text-gray-700` label → `text-sm font-medium text-k-subtle`
- `rounded-md border border-gray-300 ... focus:ring-blue-500` select → `<Select>`
- `py-4 text-center text-sm text-gray-500` loading plans → `text-k-muted`

- [ ] **Step 1: Replace students/[id]/memberships/new/page.tsx**

```tsx
"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button, Card, Select } from "@/components/ui";
import MembershipForm from "@/components/memberships/MembershipForm";
import { useMembershipActions } from "@/hooks/useMemberships";
import { useStudentDetail } from "@/hooks/useStudents";
import { useProgramPlansByProgram } from "@/hooks/usePrograms";
import { CreateMembershipRequest } from "@/lib/types/membership";

interface Props {
  params: Promise<{ id: string }>;
}

export default function NewMembershipPage({ params }: Props) {
  const { id: studentId } = use(params);
  const router = useRouter();
  const t = useTranslations("memberships");
  const tStudents = useTranslations("students");
  const tCommon = useTranslations("common");
  const { createMembership } = useMembershipActions();
  const { student, loading: studentLoading } = useStudentDetail(studentId);
  const studentName = student ? `${student.firstName} ${student.lastName}` : studentId;

  const activeEnrollments = student?.enrollments?.filter((e) => e.status === "ACTIVE") ?? [];
  const [selectedProgramId, setSelectedProgramId] = useState<string>("");

  const { plans, loading: plansLoading } = useProgramPlansByProgram(
    selectedProgramId || null,
    "HOURS_BASED"
  );

  async function handleSubmit(data: CreateMembershipRequest) {
    await createMembership(data);
    router.push(`/students/${studentId}/memberships`);
  }

  return (
    <div className="max-w-xl">
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/students/${studentId}/memberships`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/students" className="hover:text-k-subtle">{tStudents("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}`} className="hover:text-k-subtle">{studentName}</Link>
          <span className="mx-2">/</span>
          <Link href={`/students/${studentId}/memberships`} className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("newBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-6">{t("newPageTitle")}</h1>

      {studentLoading ? (
        <div className="py-8 text-center text-sm text-k-muted">{t("newLoadingStudent")}</div>
      ) : activeEnrollments.length === 0 ? (
        <div className="rounded-k-sm bg-k-warn-bg border border-k-warn-text/30 p-4 text-sm text-k-warn-text">
          {t("newNoEnrollments")}
        </div>
      ) : (
        <Card padding="md">
          <div className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-k-subtle mb-1">{t("formProgramLabel")}</label>
              <Select
                value={selectedProgramId}
                onChange={(e) => setSelectedProgramId(e.target.value)}
              >
                <option value="">{t("formSelectProgram")}</option>
                {activeEnrollments.map((e) => (
                  <option key={e.programId} value={e.programId}>
                    {e.programName} ({e.level})
                  </option>
                ))}
              </Select>
            </div>

            {selectedProgramId && (
              plansLoading ? (
                <div className="py-4 text-center text-sm text-k-muted">{t("newLoadingPlans")}</div>
              ) : plans.length === 0 ? (
                <div className="rounded-k-sm bg-k-warn-bg border border-k-warn-text/30 p-3 text-sm text-k-warn-text">
                  {t("newNoPlans")}
                </div>
              ) : (
                <MembershipForm
                  studentId={studentId}
                  plans={plans}
                  onSubmit={handleSubmit}
                  onCancel={() => router.push(`/students/${studentId}/memberships`)}
                />
              )
            )}
          </div>
        </Card>
      )}
    </div>
  );
}
```

---

## Task 22 — tsc gate + commit form pages

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors.

- [ ] **Step 2: Commit**

```bash
git add \
  "web/src/app/(dashboard)/students/new/page.tsx" \
  "web/src/app/(dashboard)/students/[id]/edit/page.tsx" \
  "web/src/app/(dashboard)/professors/new/page.tsx" \
  "web/src/app/(dashboard)/professors/[id]/edit/page.tsx" \
  "web/src/app/(dashboard)/programs/new/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/edit/page.tsx" \
  "web/src/app/(dashboard)/tenants/new/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/classes/new/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/classes/[classId]/edit/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/plans/new/page.tsx" \
  "web/src/app/(dashboard)/programs/[id]/plans/[planId]/edit/page.tsx" \
  "web/src/app/(dashboard)/students/[id]/memberships/new/page.tsx"
git commit -m "refactor(pages): migrate form pages to design tokens"
```

---

## Task 23 — Migrate student zone: student/classes

**Files:**
- Modify: `web/src/app/(dashboard)/student/classes/page.tsx`

Complex client component with inline `ClassSessionsPanel`. Changes:
- Remove `const LEVEL_COLORS` map; replace raw span with `<Badge>` from ui
- Heading gray-900→k-dark, gray-500→k-muted subtitle
- Table: `overflow-hidden rounded-lg border border-gray-200 bg-white` → `overflow-hidden rounded-lg border border-k-border bg-k-surface`; thead `bg-gray-50` → `bg-k-bg`; th `text-gray-500` → `text-k-muted`; td `text-gray-900` → `text-k-dark`; td `text-gray-500` → `text-k-muted`; row `hover:bg-gray-50` → `hover:bg-k-bg`; chevron `text-gray-400` → `text-k-muted`; `divide-gray-100` → `divide-k-line`
- Error div: `bg-red-50 border-red-200 text-red-700` → danger tokens
- `ClassSessionsPanel`:
  - Container: `bg-gray-50 px-4 py-3 border-t border-gray-100` → `bg-k-bg px-4 py-3 border-t border-k-line`
  - Heading: `text-xs font-semibold text-gray-500 uppercase tracking-wide` → `font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted`
  - Loading/empty: `text-sm text-gray-400` → `text-sm text-k-muted`
  - Error: danger tokens
  - Inline table: `text-xs text-gray-500` th → `text-k-muted`; `divide-gray-100` → `divide-k-line`; `text-gray-900` date td → `text-k-dark`; `text-gray-500` time td → `text-k-muted`
  - Alert icon: `text-amber-600` → `text-k-warn-text`
  - Register button enabled: `bg-indigo-600 text-white hover:bg-indigo-700` → `bg-k-volt text-k-dark hover:bg-k-volt-hover`
  - Register button disabled: `bg-gray-100 text-gray-400 cursor-not-allowed` → `bg-k-bg text-k-muted cursor-not-allowed`

- [ ] **Step 1: Replace student/classes/page.tsx**

```tsx
"use client";

import React, { useState, useEffect } from "react";
import { AlertTriangle, ChevronDown, ChevronRight } from "lucide-react";
import { useMyClasses } from "@/hooks/useMyClasses";
import { useAvailableSessions } from "@/hooks/useAvailableSessions";
import { useRegisterForSession } from "@/hooks/useRegisterForSession";
import SessionCapacityBar from "@/components/attendance/SessionCapacityBar";
import { Badge } from "@/components/ui";
import { AvailableSession } from "@/lib/types/attendance";
import { ProgramClassSummary } from "@/lib/types/programClass";
import { AttendanceTimeConstants, todayInTenantZone, addDays, formatSessionDate } from "@/lib/attendanceConstants";

function computeIntendedHours(start: string, end: string): number {
  const [sh, sm] = start.split(":").map(Number);
  const [eh, em] = end.split(":").map(Number);
  const durationMinutes = (eh * 60 + em) - (sh * 60 + sm);
  return Math.max(1, Math.floor(durationMinutes / 60));
}

interface ClassSessionsPanelProps {
  programId: string;
  classId: string;
}

function ClassSessionsPanel({ programId, classId }: ClassSessionsPanelProps) {
  const today = todayInTenantZone();
  const twoWeeksOut = addDays(today, 14);

  const { sessions, loading, error, refetch } = useAvailableSessions(programId, {
    from: today,
    to: twoWeeksOut,
  });
  const { register } = useRegisterForSession();

  const [localSessions, setLocalSessions] = useState<AvailableSession[]>([]);
  const [registerError, setRegisterError] = useState<string | null>(null);

  useEffect(() => {
    setLocalSessions(sessions);
  }, [sessions]);

  const classSessions = localSessions.filter(
    (s) => s.classId === classId && s.status !== "CANCELLED"
  );

  async function handleRegister(s: AvailableSession) {
    setRegisterError(null);

    setLocalSessions((prev) =>
      prev.filter((x) => !(x.classId === s.classId && x.sessionDate === s.sessionDate))
    );

    try {
      const hours = computeIntendedHours(s.startTime, s.endTime);
      await register(classId, s.sessionDate, hours);
      refetch();
    } catch (err) {
      setLocalSessions((prev) => {
        const exists = prev.some(
          (x) => x.classId === s.classId && x.sessionDate === s.sessionDate
        );
        if (exists) return prev;
        return [...prev, s].sort((a, b) => a.sessionDate.localeCompare(b.sessionDate));
      });
      setRegisterError(
        err instanceof Error ? err.message : "Failed to register. Please try again."
      );
    }
  }

  return (
    <div className="bg-k-bg px-4 py-3 border-t border-k-line">
      <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
        Upcoming Sessions — next 2 weeks
      </p>

      {loading && (
        <p className="text-sm text-k-muted py-2">Loading sessions…</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-2 text-xs text-k-danger-text mb-2">
          {error}
        </div>
      )}

      {registerError && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-2 text-xs text-k-danger-text mb-2">
          {registerError}
        </div>
      )}

      {!loading && !error && classSessions.length === 0 && (
        <p className="text-sm text-k-muted py-1">
          No upcoming sessions in the next 2 weeks.
        </p>
      )}

      {classSessions.length > 0 && (
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-xs text-k-muted">
              <th className="py-1 pr-4 text-left font-medium">Date</th>
              <th className="py-1 pr-4 text-left font-medium">Time</th>
              <th className="py-1 pr-4 text-left font-medium">Capacity</th>
              <th className="py-1 text-left font-medium"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-k-line">
            {classSessions.map((s) => {
              const isFull = s.currentCapacity >= s.maxStudents;
              const registrationOpen = s.registrationOpen !== false;
              return (
                <tr key={`${s.classId}-${s.sessionDate}`}>
                  <td className="py-2 pr-4 text-k-dark">
                    <div className="flex items-center gap-1.5">
                      <span>{formatSessionDate(s.sessionDate)}</span>
                      {s.status === "ALERTED" && (
                        <span
                          title={s.alertReason ?? "Alert issued for this session"}
                          className="inline-flex text-k-warn-text"
                        >
                          <AlertTriangle className="w-4 h-4" />
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="py-2 pr-4 text-k-muted whitespace-nowrap">
                    {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                  </td>
                  <td className="py-2 pr-4">
                    <SessionCapacityBar
                      current={s.currentCapacity}
                      max={s.maxStudents}
                    />
                  </td>
                  <td className="py-2">
                    <button
                      onClick={() => handleRegister(s)}
                      disabled={isFull || !registrationOpen}
                      title={
                        !registrationOpen
                          ? `Registration closes ${AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES} min before class`
                          : isFull
                          ? "This session is full"
                          : undefined
                      }
                      className={[
                        "rounded px-3 py-1 text-xs font-medium transition-colors",
                        isFull || !registrationOpen
                          ? "bg-k-bg text-k-muted cursor-not-allowed"
                          : "bg-k-volt text-k-dark hover:bg-k-volt-hover",
                      ].join(" ")}
                    >
                      {isFull ? "Full" : !registrationOpen ? "Closed" : "Register"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default function StudentClassesPage() {
  const { classes, loading, error } = useMyClasses();
  const [expandedClassId, setExpandedClassId] = useState<string | null>(null);

  function toggleExpand(c: ProgramClassSummary) {
    setExpandedClassId((prev) => (prev === c.id ? null : c.id));
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">My Classes</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          Classes available to you based on your enrollment level.
        </p>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">Loading…</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && classes.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          No classes found. Make sure you have an active enrollment.
        </p>
      )}

      {classes.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Class
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Program
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Capacity
                </th>
                <th className="w-8" />
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {classes.map((c) => {
                const isExpanded = expandedClassId === c.id;
                return (
                  <React.Fragment key={c.id}>
                    <tr
                      className="hover:bg-k-bg cursor-pointer"
                      onClick={() => toggleExpand(c)}
                    >
                      <td className="px-4 py-3 text-sm font-medium text-k-dark">
                        {c.name}
                      </td>
                      <td className="px-4 py-3 text-sm text-k-muted">
                        {c.programName ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        <Badge
                          variant={
                            c.level === "BEGINNER"
                              ? "beginner"
                              : c.level === "INTERMEDIATE"
                              ? "intermediate"
                              : c.level === "ADVANCED"
                              ? "advanced"
                              : "info"
                          }
                          label={c.level}
                          small
                        />
                      </td>
                      <td className="px-4 py-3 text-sm text-k-muted">
                        {c.maxStudents}
                      </td>
                      <td className="px-2 py-3 text-k-muted">
                        {isExpanded ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan={5} className="p-0">
                          <ClassSessionsPanel
                            programId={c.programId}
                            classId={c.id}
                          />
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
```

---

## Task 24 — Migrate student zone: student/registrations

**Files:**
- Modify: `web/src/app/(dashboard)/student/registrations/page.tsx`

Changes:
- Remove `LEVEL_COLORS` map; replace raw span with `<Badge>`
- Heading gray-900→k-dark, subtitle gray-500→k-muted
- Tab filter pills: active `bg-indigo-600 border-indigo-600` → `bg-k-dark text-white border-k-dark`; inactive `bg-white text-gray-600 border-gray-300 hover:border-indigo-400 hover:text-indigo-600` → `bg-k-surface text-k-subtle border-k-border hover:border-k-subtle hover:text-k-dark`
- Success banner `bg-green-50 border-green-200 text-green-700` → `bg-k-volt/10 border border-k-volt/30 text-k-volt-text`
- Error/cancelError: `bg-red-50 border-red-200 text-red-700` → danger tokens
- `CANCELLED_BY_SYSTEM` warning banner `bg-amber-50 border-amber-200 text-amber-800` → `bg-k-warn-bg border border-k-warn-text/30 text-k-warn-text`
- `SESSION_CANCELLED` warning `bg-red-50 border-red-200 text-red-800` → danger tokens
- Empty state `text-gray-400` → `text-k-muted`; loading `text-gray-500` → `text-k-muted`
- Table: `bg-white` → `bg-k-surface`; `border-gray-200` → `border-k-border`; thead `bg-gray-50` → `bg-k-bg`; th `text-gray-500` → `text-k-muted`; td date `text-gray-900` → `text-k-dark`; td time/hours `text-gray-500` → `text-k-muted`; `hover:bg-gray-50` → `hover:bg-k-bg`; `divide-gray-100` → `divide-k-line`; cancel link `text-red-600 hover:text-red-800` → `text-k-danger-text hover:text-k-dark`; disabled cancel `text-gray-400` → `text-k-muted`
- Confirmation modal: keep `bg-black/40` overlay; inner `bg-white rounded-lg shadow-xl p-6` → `bg-k-surface rounded-k-lg shadow-k-modal p-6`; `text-gray-900` h2 → `text-k-dark`; `text-gray-600` body → `text-k-muted`; Keep button `text-gray-700 bg-white border-gray-300 hover:bg-gray-50` → `<Button variant="outline">`; Cancel confirm `bg-red-600 hover:bg-red-700` → `<Button variant="danger">`

- [ ] **Step 1: Replace student/registrations/page.tsx**

```tsx
"use client";

import { useState } from "react";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import RegistrationStatusBadge from "@/components/attendance/RegistrationStatusBadge";
import { Badge, Button } from "@/components/ui";
import { Registration } from "@/lib/types/attendance";
import { AttendanceTimeConstants, formatSessionDate } from "@/lib/attendanceConstants";

function isCancellable(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  const cutoff = new Date(sessionStart.getTime() - AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES * 60 * 1000);
  return new Date() < cutoff;
}

function isWithinWarningZone(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  return new Date() >= sessionStart;
}

const STATUS_OPTIONS = [
  { value: "REGISTERED",           label: "Registered" },
  { value: "CANCELLED_BY_STUDENT", label: "Cancelled" },
  { value: "CANCELLED_BY_SYSTEM",  label: "Schedule Changed" },
  { value: "SESSION_CANCELLED",    label: "Cancelled by league" },
] as const;

type StatusFilter = typeof STATUS_OPTIONS[number]["value"];

export default function StudentRegistrationsPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("REGISTERED");

  const { registrations, loading, error, refetch } = useMyRegistrations({ status: statusFilter });
  const { cancel, loading: cancelling, error: cancelError, clearError } = useCancelRegistration();

  const [confirmTarget, setConfirmTarget] = useState<Registration | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleCancelClick = (reg: Registration) => {
    clearError();
    setSuccessMessage(null);
    setConfirmTarget(reg);
  };

  const handleConfirmCancel = async () => {
    if (!confirmTarget) return;
    try {
      await cancel(confirmTarget.id);
      setSuccessMessage(
        `Registration for "${confirmTarget.className}" on ${formatSessionDate(confirmTarget.sessionDate)} cancelled.`
      );
      setConfirmTarget(null);
      refetch();
    } catch {
      // error surfaced via cancelError from the hook
    }
  };

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">My Registrations</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          Your upcoming and past class session registrations.
        </p>
      </div>

      <div className="mb-4 flex gap-2">
        {STATUS_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => setStatusFilter(opt.value)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
              statusFilter === opt.value
                ? "bg-k-dark text-white border-k-dark"
                : "bg-k-surface text-k-subtle border-k-border hover:border-k-subtle hover:text-k-dark"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {successMessage && (
        <div className="mb-4 rounded-k-sm bg-k-volt/10 border border-k-volt/30 p-4 text-sm text-k-volt-text">
          {successMessage}
        </div>
      )}

      {cancelError && (
        <div className="mb-4 rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {cancelError}
        </div>
      )}

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">Loading…</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {statusFilter === "CANCELLED_BY_SYSTEM" && (
        <div className="mb-4 rounded-k-sm bg-k-warn-bg border border-k-warn-text/30 p-4 text-sm text-k-warn-text">
          These registrations were cancelled because the class schedule was changed.
          Go to <strong>My Classes</strong> to register for the new sessions.
        </div>
      )}

      {statusFilter === "SESSION_CANCELLED" && (
        <div className="mb-4 rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          These registrations were cancelled by the league. Your spot was released and no hours
          were deducted. You can register for a different session in{" "}
          <strong>My Classes</strong>.
        </div>
      )}

      {!loading && !error && registrations.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          {statusFilter === "REGISTERED"
            ? "You have no upcoming registrations."
            : statusFilter === "CANCELLED_BY_SYSTEM"
            ? "No registrations were cancelled by a schedule change."
            : statusFilter === "SESSION_CANCELLED"
            ? "No sessions were cancelled by the league."
            : "You have no cancelled registrations."}
        </p>
      )}

      {registrations.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Time
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Class
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Hours
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {registrations.map((r) => {
                const cancellable = isCancellable(r);
                const inPast = isWithinWarningZone(r);
                return (
                  <tr key={r.id} className="hover:bg-k-bg">
                    <td className="px-4 py-3 text-sm text-k-dark">
                      {formatSessionDate(r.sessionDate)}
                    </td>
                    <td className="px-4 py-3 text-sm text-k-muted whitespace-nowrap">
                      {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                    </td>
                    <td className="px-4 py-3 text-sm text-k-dark">
                      {r.className}
                      {r.status === "SESSION_CANCELLED" && r.sessionCancellationReason && (
                        <div className="mt-0.5 text-xs italic text-k-danger-text">
                          Reason: {r.sessionCancellationReason}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <Badge
                        variant={
                          r.level === "BEGINNER"
                            ? "beginner"
                            : r.level === "INTERMEDIATE"
                            ? "intermediate"
                            : r.level === "ADVANCED"
                            ? "advanced"
                            : "info"
                        }
                        label={r.level}
                        small
                      />
                    </td>
                    <td className="px-4 py-3 text-sm text-k-muted">
                      {r.intendedHours}h
                    </td>
                    <td className="px-4 py-3">
                      <RegistrationStatusBadge status={r.status} />
                    </td>
                    <td className="px-4 py-3">
                      {r.status === "REGISTERED" && (
                        cancellable ? (
                          <button
                            onClick={() => handleCancelClick(r)}
                            className="text-xs font-medium text-k-danger-text hover:text-k-dark transition-colors"
                          >
                            Cancel
                          </button>
                        ) : (
                          <span
                            title={
                              inPast
                                ? "Session has already started"
                                : `Cancellation window closed (${AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES} min before class)`
                            }
                            className="text-xs text-k-muted cursor-not-allowed"
                          >
                            Cancel
                          </span>
                        )
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {confirmTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-k-surface rounded-k-lg shadow-k-modal p-6 max-w-sm w-full mx-4">
            <h2 className="text-lg font-semibold text-k-dark mb-2">
              Cancel registration?
            </h2>
            <p className="text-sm text-k-muted mb-4">
              Cancel your registration for{" "}
              <span className="font-medium text-k-dark">{confirmTarget.className}</span> on{" "}
              <span className="font-medium text-k-dark">
                {formatSessionDate(confirmTarget.sessionDate)}
              </span>{" "}
              at {confirmTarget.sessionStartTime.slice(0, 5)}? Your spot will be
              released.
            </p>

            {cancelError && (
              <p className="mb-3 text-sm text-k-danger-text">{cancelError}</p>
            )}

            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => { setConfirmTarget(null); clearError(); }}
                disabled={cancelling}
              >
                Keep
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmCancel}
                disabled={cancelling}
              >
                {cancelling ? "Cancelling…" : "Yes, cancel"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
```

---

## Task 25 — Migrate student zone: enrollments, memberships, memberships/new

**Files:**
- Modify: `web/src/app/(dashboard)/student/enrollments/page.tsx`
- Modify: `web/src/app/(dashboard)/student/memberships/page.tsx`
- Modify: `web/src/app/(dashboard)/student/memberships/new/page.tsx`

- [ ] **Step 1: Replace student/enrollments/page.tsx**

Changes: `<select>` → `<Select className="w-auto">`; heading gray-900→k-dark; subtitle gray-500→k-muted; label gray-600→k-muted; table: bg-white→bg-k-surface, border-gray-200→border-k-border, thead bg-gray-50→bg-k-bg, th text-gray-500→text-k-muted, td gray-900→k-dark, hover:bg-gray-50→hover:bg-k-bg, divide-gray-100→divide-k-line; inline enrollment status span `bg-green-100 text-green-800 / bg-gray-100 text-gray-600` → `<Badge variant={e.status === "ACTIVE" ? "active" : "inactive"} label={e.status} small />`; loading gray-500→k-muted; empty gray-400→k-muted; error danger tokens. Import `Badge, Select`.

```tsx
"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import LevelBadge from "@/components/enrollments/LevelBadge";
import { Badge, Select } from "@/components/ui";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentEnrollmentsPage() {
  const t = useTranslations("studentEnrollmentsPage");
  const tCommon = useTranslations("common");

  const STATUS_OPTIONS = [
    { value: "ACTIVE", label: tCommon("active") },
    { value: "INACTIVE", label: tCommon("inactive") },
    { value: "", label: tCommon("all") },
  ];

  const [statusFilter, setStatusFilter] = useState<string>("ACTIVE");
  const { enrollments, loading, error } = useMyEnrollments(statusFilter || undefined);

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
            {t("subtitle")}
          </p>
        </div>
        <div className="flex items-center gap-2 pt-1">
          <label htmlFor="statusFilter" className="text-sm text-k-muted whitespace-nowrap">
            {t("statusLabel")}
          </label>
          <Select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-auto"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">{t("loading")}</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && enrollments.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          {t("empty", { status: statusFilter ? statusFilter.toLowerCase() : "" })}
        </p>
      )}

      {enrollments.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colProgram")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colLevel")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colStatus")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colEnrolled")}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {enrollments.map((e) => (
                <tr key={e.id} className="hover:bg-k-bg">
                  <td className="px-4 py-3 text-sm font-medium text-k-dark">
                    {e.programName}
                  </td>
                  <td className="px-4 py-3">
                    <LevelBadge level={e.level} />
                  </td>
                  <td className="px-4 py-3">
                    <Badge
                      variant={e.status === "ACTIVE" ? "active" : "inactive"}
                      label={e.status}
                      small
                    />
                  </td>
                  <td className="px-4 py-3 text-sm text-k-muted">
                    {formatDate(e.enrollmentDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Replace student/memberships/page.tsx**

Changes: heading gray-900→k-dark; subtitle gray-500→k-muted; `bg-indigo-600 hover:bg-indigo-700` new button → `<Button variant="volt" asChild>`; loading gray-500→k-muted; error danger tokens; empty gray-400→k-muted; membership card `rounded-lg border border-gray-200 bg-white p-5` → `<Card padding="md">`; header text-gray-900→text-k-dark; id font-mono text-gray-400→text-k-muted; `text-indigo-600 hover:text-indigo-800` links → `text-k-subtle hover:text-k-dark`; `text-amber-600 hover:text-amber-800` upload proof → `text-k-warn-text hover:text-k-dark`; `text-emerald-600 hover:text-emerald-800` renew → `text-k-volt-text hover:text-k-dark`.

```tsx
"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMyMemberships } from "@/hooks/useMemberships";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import HourBalance from "@/components/memberships/HourBalance";
import { Button, Card } from "@/components/ui";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentMembershipsPage() {
  const t = useTranslations("studentMembershipsPage");
  const { memberships, loading, error } = useMyMemberships();

  const hasActiveMembership = memberships.some(
    (m) =>
      m.status === "ACTIVE" ||
      m.status === "PENDING_PAYMENT" ||
      m.status === "PENDING_PAYMENT_VALIDATION" ||
      m.status === "PENDING_MANAGER_ACTIVATION"
  );

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
            {t("subtitle")}
          </p>
        </div>
        {!hasActiveMembership && (
          <Button variant="volt" asChild>
            <Link href="/student/memberships/new">{t("newButton")}</Link>
          </Button>
        )}
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">{t("loading")}</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && memberships.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          {t("empty")}
        </p>
      )}

      <div className="space-y-6">
        {memberships.map((m) => (
          <Card key={m.id} padding="md">
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold text-k-dark">{m.planName}</p>
                  <p className="text-xs text-k-muted mt-0.5 font-mono">{m.id}</p>
                </div>
                <MembershipStatusBadge status={m.status} />
              </div>

              <HourBalance available={m.availableHours} purchased={m.purchasedHours} />

              <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-sm">
                <div>
                  <span className="text-k-muted">{t("expires")} </span>
                  <span className="font-medium text-k-dark">
                    {formatDate(m.expirationDate)}
                  </span>
                </div>
                <div>
                  <span className="text-k-muted">{t("paymentLabel")} </span>
                  <span className="font-medium text-k-dark">
                    {m.paymentValidated ? t("paymentValidated") : t("paymentPending")}
                  </span>
                </div>
              </div>

              <div className="pt-1 flex items-center gap-4">
                <Link
                  href={`/student/memberships/${m.id}`}
                  className="text-sm text-k-subtle hover:text-k-dark font-medium"
                >
                  {t("viewDetails")}
                </Link>
                {m.status === "PENDING_PAYMENT" && (
                  <Link
                    href={`/student/memberships/${m.id}`}
                    className="text-sm font-medium text-k-warn-text hover:text-k-dark"
                  >
                    {t("uploadProof")}
                  </Link>
                )}
                {(m.status === "EXPIRED" || m.status === "INACTIVE") && (
                  <Link
                    href={`/student/memberships/new?renew=${m.id}`}
                    className="text-sm font-medium text-k-volt-text hover:text-k-dark"
                  >
                    {t("renew")}
                  </Link>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Replace student/memberships/new/page.tsx**

Changes: nav breadcrumb gray-500→k-muted; heading gray-900→k-dark; subtitle gray-500→k-muted; loading gray-500→k-muted; form wrapper `rounded-lg border border-gray-200 bg-white p-6` → `<Card padding="md">`. Import `Card`.

```tsx
"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Card } from "@/components/ui";
import StudentMembershipCreationForm from "@/components/memberships/StudentMembershipCreationForm";
import { useMembershipActions, useMembershipDetail } from "@/hooks/useMemberships";

function NewMembershipContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const renewId = searchParams.get("renew");
  const t = useTranslations("studentMembershipsPage");
  const tCommon = useTranslations("common");

  const { createSelfMembership, renewMembership } = useMembershipActions();

  const { membership: sourceMembership, loading: sourceLoading } = useMembershipDetail(renewId ?? "00000000-0000-0000-0000-000000000000");

  const isRenewing = !!renewId;
  const initialProgramId = isRenewing ? sourceMembership?.programId : undefined;
  const initialPlanId = isRenewing ? sourceMembership?.planId : undefined;
  const renewBanner = isRenewing && sourceMembership
    ? `Renewing plan "${sourceMembership.planName}" — your existing membership will be reactivated.`
    : undefined;

  async function handleSubmit(planId: string, file: File) {
    if (isRenewing && renewId) {
      await renewMembership(renewId, file);
      router.push(`/student/memberships/${renewId}`);
    } else {
      const membership = await createSelfMembership({ planId }, file);
      router.push(`/student/memberships/${membership.id}`);
    }
  }

  if (isRenewing && sourceLoading) {
    return <p className="py-8 text-center text-sm text-k-muted">{tCommon("loading")}</p>;
  }

  return (
    <div className="max-w-xl">
      <nav className="mb-6 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
        <Link href="/student/memberships" className="hover:text-k-subtle">
          {t("title")}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-k-subtle">{isRenewing ? t("renew") : t("newButton")}</span>
      </nav>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-2">
        {isRenewing ? t("renew") : t("newButton")}
      </h1>
      <p className="font-[var(--font-mono)] text-xs text-k-muted mb-6">
        {isRenewing
          ? "Your renewed membership will start pending payment validation."
          : "Select a plan from your enrolled program, attach your payment proof, and submit."}
      </p>

      <Card padding="md">
        <StudentMembershipCreationForm
          initialProgramId={initialProgramId}
          initialPlanId={initialPlanId}
          renewBanner={renewBanner}
          onSubmit={handleSubmit}
          onCancel={() => router.push("/student/memberships")}
        />
      </Card>
    </div>
  );
}

export default function NewMembershipPage() {
  return (
    <Suspense fallback={<p className="py-8 text-center text-sm text-k-muted">{/* fallback */}</p>}>
      <NewMembershipContent />
    </Suspense>
  );
}
```

---

## Task 26 — tsc gate + commit student zone

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors.

- [ ] **Step 2: Commit**

```bash
git add \
  "web/src/app/(dashboard)/student/classes/page.tsx" \
  "web/src/app/(dashboard)/student/registrations/page.tsx" \
  "web/src/app/(dashboard)/student/enrollments/page.tsx" \
  "web/src/app/(dashboard)/student/memberships/page.tsx" \
  "web/src/app/(dashboard)/student/memberships/new/page.tsx"
git commit -m "refactor(pages): migrate student zone pages to design tokens"
```

---

## Task 27 — Migrate auth pages: login, forgot-password, reset-password

**Files:**
- Rewrite: `web/src/app/(auth)/login/page.tsx`
- Rewrite: `web/src/app/(auth)/forgot-password/page.tsx`
- Rewrite: `web/src/app/(auth)/reset-password/page.tsx`

All get the dark shell (spec Section 5). `reset-password` also has an invalid-token branch using danger tokens + primary Button.

- [ ] **Step 1: Replace login/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import LoginForm from "@/components/auth/LoginForm";

export default async function LoginPage() {
  const t = await getTranslations("loginPage");

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="mt-2 text-sm text-k-muted">{t("subtitle")}</p>
        </div>
        <LoginForm />
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Replace forgot-password/page.tsx**

`forgot-password` has no i18n. Keep strings as-is; only swap shell classes.

```tsx
import ForgotPasswordForm from "@/components/auth/ForgotPasswordForm";

export default function ForgotPasswordPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">Forgot your password?</h1>
          <p className="mt-2 text-sm text-k-muted">Enter your email and we&apos;ll send you a reset link.</p>
        </div>
        <ForgotPasswordForm />
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Replace reset-password/page.tsx**

Invalid-token branch: `bg-red-50 border-red-200 rounded-md` → danger tokens; `bg-indigo-600 text-white ... hover:bg-indigo-700` anchor → `<Button variant="primary" asChild>`. Valid-token branch gets dark shell. Suspense fallback also gets dark shell skeleton.

```tsx
"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui";
import ResetPasswordForm from "@/components/auth/ResetPasswordForm";

function ResetPasswordContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  if (!token) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
        <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
          klasio
        </div>
        <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
          <div className="text-center">
            <h2 className="text-lg font-semibold text-k-danger-text mb-2">Invalid Link</h2>
            <p className="text-sm text-k-subtle mb-4">
              This password reset link is invalid. Please request a new one.
            </p>
            <Button variant="primary" asChild>
              <Link href="/forgot-password">Request New Link</Link>
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">Reset your password</h1>
          <p className="mt-2 text-sm text-k-muted">Enter your new password below.</p>
        </div>
        <ResetPasswordForm token={token} />
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
          <p className="text-k-muted">Loading...</p>
        </div>
      }
    >
      <ResetPasswordContent />
    </Suspense>
  );
}
```

---

## Task 28 — Migrate auth pages: setup-account + register/[tenantSlug]

**Files:**
- Rewrite: `web/src/app/(auth)/setup-account/page.tsx`
- Rewrite: `web/src/app/register/[tenantSlug]/page.tsx`

`setup-account` is a client component with Suspense. `register` is a server component — use `max-w-lg` (wider for registration form per spec). Both get dark shell.

- [ ] **Step 1: Replace setup-account/page.tsx**

```tsx
"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import SetupAccountForm from "@/components/auth/SetupAccountForm";

function SetupAccountContent() {
  const t = useTranslations("auth.setupAccount");
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
          <p className="mt-2 text-sm text-k-muted">{t("pageSubtitle")}</p>
        </div>
        <SetupAccountForm token={token} />
      </div>
    </div>
  );
}

export default function SetupAccountPage() {
  const t = useTranslations("auth.setupAccount");

  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
          <p className="text-k-muted">{t("pageLoading")}</p>
        </div>
      }
    >
      <SetupAccountContent />
    </Suspense>
  );
}
```

- [ ] **Step 2: Replace register/[tenantSlug]/page.tsx**

```tsx
import { getTranslations } from "next-intl/server";
import RegistrationForm from "@/components/auth/RegistrationForm";

interface RegisterPageProps {
  params: Promise<{ tenantSlug: string }>;
}

export default async function RegisterPage({ params }: RegisterPageProps) {
  const { tenantSlug } = await params;
  const t = await getTranslations("registerPage");

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
      <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
        klasio
      </div>
      <div className="w-full max-w-lg bg-k-surface rounded-k-xl shadow-k-modal p-10">
        <div className="text-center mb-6">
          <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="mt-2 text-sm text-k-muted">{t("subtitle", { tenantSlug })}</p>
        </div>
        <RegistrationForm tenantSlug={tenantSlug} />
      </div>
    </div>
  );
}
```

---

## Task 29 — tsc gate + commit auth pages

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors.

- [ ] **Step 2: Commit**

```bash
git add \
  "web/src/app/(auth)/login/page.tsx" \
  "web/src/app/(auth)/forgot-password/page.tsx" \
  "web/src/app/(auth)/reset-password/page.tsx" \
  "web/src/app/(auth)/setup-account/page.tsx" \
  "web/src/app/register/[tenantSlug]/page.tsx"
git commit -m "refactor(pages): migrate auth pages to design tokens"
```

---

## Task 30 — Migrate loading skeletons (5 files)

**Files:**
- Modify: `web/src/app/(dashboard)/programs/loading.tsx`
- Modify: `web/src/app/(dashboard)/programs/[id]/loading.tsx`
- Modify: `web/src/app/(dashboard)/tenants/loading.tsx`
- Modify: `web/src/app/(dashboard)/tenants/[slug]/loading.tsx`
- Modify: `web/src/app/(dashboard)/student/memberships/new/loading.tsx`

Token-only swaps: `bg-gray-200` → `bg-k-line`; `rounded` / `rounded-md` → `rounded-k-sm`; `bg-white shadow` → `bg-k-surface border border-k-border`; `text-gray-500` → `text-k-muted`; `border-gray-200` → `border-k-line`.

- [ ] **Step 1: Replace programs/loading.tsx**

```tsx
export default function ProgramsLoading() {
  return (
    <div className="text-center py-8 text-sm text-k-muted">
      Loading programs...
    </div>
  );
}
```

- [ ] **Step 2: Replace programs/[id]/loading.tsx**

```tsx
export default function ProgramDetailLoading() {
  return (
    <div className="text-center py-8 text-sm text-k-muted">
      Loading program details...
    </div>
  );
}
```

- [ ] **Step 3: Replace tenants/loading.tsx**

```tsx
export default function TenantsLoading() {
  return (
    <div className="animate-pulse space-y-4">
      <div className="flex items-center justify-between mb-8">
        <div className="h-8 w-32 bg-k-line rounded-k-sm" />
        <div className="h-10 w-40 bg-k-line rounded-k-sm" />
      </div>
      <div className="h-10 w-48 bg-k-line rounded-k-sm" />
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-14 bg-k-line rounded-k-sm" />
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Replace tenants/[slug]/loading.tsx**

```tsx
export default function TenantDetailLoading() {
  return (
    <div className="animate-pulse space-y-6">
      <div className="h-4 w-48 bg-k-line rounded-k-sm" />
      <div className="bg-k-surface border border-k-border rounded-k-lg overflow-hidden">
        <div className="px-6 py-5 border-b border-k-line flex items-center gap-4">
          <div className="h-12 w-12 bg-k-line rounded-full" />
          <div className="space-y-2">
            <div className="h-6 w-40 bg-k-line rounded-k-sm" />
            <div className="h-4 w-24 bg-k-line rounded-k-sm" />
          </div>
        </div>
        <div className="px-6 py-5 grid grid-cols-2 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="space-y-2">
              <div className="h-4 w-24 bg-k-line rounded-k-sm" />
              <div className="h-4 w-32 bg-k-line rounded-k-sm" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Replace student/memberships/new/loading.tsx**

```tsx
export default function Loading() {
  return <p className="py-8 text-center text-sm text-k-muted">Loading…</p>;
}
```

---

## Task 31 — tsc gate + commit loading skeletons

- [ ] **Step 1: Run TypeScript check**

```bash
cd web && npx tsc --noEmit 2>&1 | head -50
```

Expected: zero errors.

- [ ] **Step 2: Commit**

```bash
git add \
  "web/src/app/(dashboard)/programs/loading.tsx" \
  "web/src/app/(dashboard)/programs/[id]/loading.tsx" \
  "web/src/app/(dashboard)/tenants/loading.tsx" \
  "web/src/app/(dashboard)/tenants/[slug]/loading.tsx" \
  "web/src/app/(dashboard)/student/memberships/new/loading.tsx"
git commit -m "refactor(pages): migrate loading skeletons to design tokens"
```

---

## Task 32 — Final sweep: grep for residual raw color classes

**Files:** no changes

- [ ] **Step 1: Run grep sweep in src/app**

```bash
grep -rn "bg-gray\|text-gray\|border-gray\|bg-white\|text-blue\|bg-blue\|bg-indigo\|text-indigo\|bg-green\|text-green\|bg-red\|text-red\|bg-yellow\|text-yellow\|bg-amber\|text-amber" \
  web/src/app/\(dashboard\) \
  web/src/app/\(auth\) \
  web/src/app/register \
  2>/dev/null | grep -v "node_modules" | grep -v ".next"
```

Expected: zero lines from `(dashboard)`, `(auth)`, or `register` directories.

- [ ] **Step 2: Interpret results**

If any hits appear:
- Check if the hit is inside a component file (`components/`) accidentally caught by glob — those are out of scope.
- If the hit is in a `page.tsx` or `loading.tsx`, fix it using the token substitution table in the spec (Section 1). Add a note to the PR description for any remaining hits that are deliberately deferred.

---

## Task 33 — Full production build verification

**Files:** no changes

- [ ] **Step 1: Run production build**

```bash
cd web && npm run build 2>&1 | tail -30
```

Expected output ends with:
```
✓ Compiled successfully
Route (app) ...
```

Zero TypeScript errors, zero build errors.

- [ ] **Step 2: If build fails**

Check the error. Common causes:
- Missing import (forgot to add `Button`, `Card`, etc.)
- Using a translation key that doesn't exist → verify the key exists in both `en.json` and `es.json`
- `asChild` usage on a non-Slot-aware component

Fix the issue, re-run `npm run build` until clean.
