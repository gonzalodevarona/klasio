# Page-Level Design Token Migration — Design

**Date:** 2026-04-25
**Branch:** `feature/full-redesign`
**Step:** 5 of 5 in the design system migration (tokens → primitives → compound → layout → pages)

## Overview

Steps 1–4 of the design system migration are complete: tokens are defined in `tailwind.config.ts`, UI primitives (`Button`, `Card`, `StatCard`, `Badge`, `Input`, `Select`, `Modal`, `Table`, `Pagination`, `HoursBar`) are built and tested, compound components are migrated, and the dashboard layout/sidebar/footer use design tokens.

This step connects the migration at the page level. Every page file in `src/app` that still uses raw Tailwind grays, blues, or `bg-white` is migrated to the `k-*` palette and uses the relevant primitive (`Button`, `Card`, `StatCard`, `Badge`) where the spec calls for it.

## Goals

1. Visually unify all pages with the already-migrated layout and components — no remaining gray-* or raw color classes in `src/app`.
2. Use `<StatCard>`, `<Card>`, `<Button>`, `<Badge>` primitives instead of inline `<div className="bg-white rounded-lg ...">` / `<button className="bg-blue-600 ...">` patterns.
3. Preserve all data-fetching logic, server/client component boundaries, and i18n.
4. Add i18n keys for any new user-facing strings (placeholder stat labels, breadcrumb labels).

## Non-Goals

- Component-level rework inside `components/students/`, `components/programs/`, etc. — that was Step 3.
- DRY-up of auth shell into a shared `(auth)/layout.tsx`. Stays per-page for now (5 auth pages duplicate the dark-bg + card + logo shell intentionally).
- Shared `<PageHeader>`, `<DetailShell>`, `<ListShell>` extraction. Not requested; pages remain thin wrappers.
- Real stat values for placeholder `<StatCard>` instances on Admin/Manager/Professor/Superadmin dashboards — deferred until aggregation APIs exist (RF-30, RF-31, etc.).

## Section 1 — Token Substitution Rules

Every gray/blue/white class in `src/app` maps mechanically to a `k-*` equivalent.

| Old | New |
|---|---|
| `text-gray-900` | `text-k-dark` |
| `text-gray-700` / `text-gray-800` | `text-k-subtle` |
| `text-gray-500` / `text-gray-600` | `text-k-muted` |
| `text-gray-400` | `text-k-muted` |
| `bg-gray-50` | `bg-k-bg` |
| `bg-gray-100` | `bg-k-surface` |
| `bg-white` | `bg-k-surface` |
| `border-gray-200` / `border-gray-300` | `border-k-border` |
| `hover:bg-gray-50` | `hover:bg-k-bg` |
| `hover:bg-gray-200` | `hover:bg-k-border` |
| `text-indigo-600` / `text-indigo-800` | `text-k-subtle` |
| `bg-blue-600` (CTA buttons) | `<Button variant="volt">` |
| `bg-red-50` / `text-red-700` (errors) | `bg-k-danger-bg` / `text-k-danger-text` |
| `rounded-md` / `rounded-lg` (structural) | `rounded-k-sm` / `rounded-k-lg` |

Exception: classes inside `<Badge>`, `<MembershipStatusBadge>`, and other Step-3-migrated components are not touched.

## Section 2 — Dashboard Pages

5 dashboards migrated.

### Admin / Manager / Professor / Superadmin (placeholder dashboards)

These four are currently empty placeholders (`useAuth` + sign-out button + welcome paragraph). Sign-out is **removed** — sidebar already owns logout.

After migration, each becomes a server component:

```tsx
import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";

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

Stat card labels per role (stub values `"—"` until real aggregation API ships):

| Role | StatCards |
|---|---|
| Admin | Students, Active memberships, Pending proofs, Programs |
| Manager | Classes this week, Students in program, Pending activations, Hours logged |
| Professor | Classes today, Students present, Sessions this month, Hours taught |
| Superadmin | Tenants, Total students, Active memberships, Monthly revenue |

Manager dashboard additionally retains `<DelegatedMembershipList />` below the StatCard grid, with a section heading (`text-base font-semibold text-k-dark mb-4`) reading "Memberships awaiting activation".

### Student Dashboard

Real data. No StatCard grid (data is relational, not aggregate counts). Migration:

- Heading block: `text-2xl font-bold text-gray-900` → `text-[26px] font-extrabold tracking-[-0.02em] text-k-dark`; subtitle → `font-[var(--font-mono)] text-xs text-k-muted mt-1`
- Each section container `rounded-lg border border-gray-200 bg-white p-5` → `<Card padding="md">`
- Section headings `text-sm font-semibold text-gray-700 uppercase tracking-wide mb-3` → `text-base font-semibold text-k-dark mb-4`
- "View all" links: `text-xs text-indigo-600 hover:text-indigo-800` → `text-xs text-k-subtle hover:text-k-dark font-medium`
- Loading / empty: `text-gray-400` → `text-k-muted`
- Inline enrollment status pill (raw `bg-green-100 text-green-700` etc.) → `<Badge variant="active|inactive">`
- Inline level badge (raw blue/yellow/red conditionals) → `<Badge variant="beginner|intermediate|advanced">`
- Quick links (`<Link className="rounded-lg border bg-white...">`) → `<Button variant="outline" asChild><Link>...</Link></Button>`

## Section 3 — List Pages

10 list pages: students, professors, programs, classes, payment-proofs, notifications, admins, managers, plans, tenants.

### Top bar

```tsx
<div className="flex items-center justify-between mb-6">
  <div>
    <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
    <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{recordCount} records</p>
  </div>
  <Button variant="volt" asChild>
    <Link href="/students/new">+ {t("addButton")}</Link>
  </Button>
</div>
```

### Record count

Surfaced only where the list component already exposes it at the page boundary. If the count would require a duplicate fetch at the page level, omit the count line — leave for a follow-up where the list component lifts the count up.

### "New X" button

`<Button variant="volt" asChild>` wrapping the existing `<Link>`. Keeps Next.js client-side routing intact. Existing i18n keys (`addButton`, `createButton`, `newButton`) reused unchanged.

### Filter bar

Container becomes `flex gap-2.5 flex-wrap mb-5`. Filter `<select>` / `<input>` already migrated in Step 3 (use `<Select>` / `<Input>` primitives).

### Page padding

- `managers/page.tsx`: strip duplicate `p-6 max-w-7xl mx-auto`
- `notifications/page.tsx`: strip `<main className="p-6">`, replace with `<div>`
- Layout owns padding (`pt-20 px-6 pb-6 lg:p-9`).

### Pages without "New X" button

`notifications/page.tsx`, `payment-proofs/page.tsx`: heading + subtitle only, no Button on the right. Don't force a CTA.

## Section 4 — Detail Pages

8 detail pages: `students/[id]`, `professors/[id]`, `programs/[id]`, `programs/[id]/classes/[classId]`, `programs/[id]/plans/[planId]`, `students/[id]/memberships/[membershipId]`, `student/memberships/[membershipId]`, `tenants/[slug]`.

### Top nav block

Back button + breadcrumb (both, per the user's "do both" decision):

```tsx
<div className="mb-6">
  <Button variant="ghost" size="sm" asChild>
    <Link href="/students">← {t("backToList")}</Link>
  </Button>
  <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
    <Link href="/students" className="hover:text-k-subtle">{t("listLabel")}</Link>
    <span className="mx-2">/</span>
    <span className="text-k-subtle">{currentName}</span>
  </nav>
</div>
```

Back button is the primary affordance; mono breadcrumb provides hierarchical context without competing.

### Detail header card

Most detail components (`StudentDetail`, `ProfessorDetail`, `ProgramDetail`, etc.) own their internal header — the page does not wrap them in a `<Card dark>` at the page level. The `<Card dark>` header pattern lives inside the component (already migrated in Step 3). Page only owns the back button + breadcrumb.

### Loading state

```tsx
<div className="text-center py-8 text-sm text-k-muted">{t("loadingText")}</div>
```

### Error state

```tsx
<div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text" role="alert">
  {error}
</div>
```

### i18n

Add `backToList` and `listLabel` keys per resource (`students`, `professors`, `programs`, etc.) to both `en.json` and `es.json`.

## Section 5 — Auth Pages

5 pages: `login`, `forgot-password`, `reset-password`, `setup-account`, `register/[tenantSlug]`.

Per-page shell duplication accepted (each page writes its own wrapper). DRY-up via shared `(auth)/layout.tsx` deferred.

### Page shell

```tsx
<div className="min-h-screen flex flex-col items-center justify-center bg-k-dark px-4 py-12">
  <div className="mb-8 text-white font-extrabold text-2xl tracking-[-0.04em] text-center">
    klasio
  </div>
  <div className="w-full max-w-md bg-k-surface rounded-k-xl shadow-k-modal p-10">
    <div className="text-center mb-6">
      <h1 className="text-[22px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
      <p className="mt-2 text-sm text-k-muted">{t("subtitle")}</p>
    </div>
    <FormComponent />
  </div>
</div>
```

### Klasio wordmark

Inline text (no SVG asset). Existing `KLogo` component is sidebar-specific — auth gets its own inline rendering.

### reset-password invalid-token branch

Same dark page shell, card content swapped:

```tsx
<div className="text-center">
  <h2 className="text-lg font-semibold text-k-danger-text mb-2">{t("invalidTitle")}</h2>
  <p className="text-sm text-k-subtle mb-4">{t("invalidBody")}</p>
  <Button variant="primary" asChild>
    <Link href="/forgot-password">{t("requestNew")}</Link>
  </Button>
</div>
```

### Suspense fallbacks

`setup-account` and `reset-password` wrap their content in `<Suspense>` for `useSearchParams`. Fallback uses the same dark shell with content `<p className="text-k-muted">{t("loading")}</p>`.

### Form components

Already migrated in Step 3. Pages own only the shell + heading + logo.

## Section 6 — Scope and Execution

### File inventory

~51 page files in `src/app` contain gray/blue/white classes. The 7 groups below sum to 50; the remaining file(s) surface during the final sweep (step 8) and are absorbed into the closest matching group. Grouped:

| Group | Count |
|---|---|
| Dashboards | 5 |
| List pages | 10 |
| Detail pages | 8 |
| Form pages (new/edit) | 13 |
| Student zone subpages | 4 |
| Auth pages | 5 |
| Loading skeletons | 5 |

### Group details

- **Form pages** mirror detail-page chrome (back button + breadcrumb at the top using the same pattern from Section 4). The form component itself is wrapped in a `<Card padding="md">` instead of a raw `<div className="bg-white rounded-lg ...">`. Form input primitives (`<Input>`, `<Select>`) were migrated in Step 3.
- **Loading skeletons** are token-only swaps (`bg-gray-200` → `bg-k-line`, `bg-white` → `bg-k-surface`). No structural changes.
- **Student zone subpages** (`student/classes`, `student/enrollments`, `student/memberships`, `student/registrations`) follow the list-page pattern.

### Execution order

Sequential, page-by-page, with `tsc --noEmit` and a commit after each group:

1. Dashboards (5)
2. List pages (10)
3. Detail pages (8)
4. Form pages (14)
5. Student zone subpages (4)
6. Auth pages (5)
7. Loading skeletons (5)

After all groups:

8. Final sweep: `grep -rn "bg-gray\|text-gray\|border-gray\|bg-white" src/app` — must return zero hits in `src/app/(dashboard)`, `src/app/(auth)`, `src/app/register`. Any remaining hit becomes an explicit follow-up note in the PR description.
9. `npm run build` — zero errors.

### Commits

Conventional-commit format, one per group:

- `refactor(pages): migrate dashboard pages to design tokens`
- `refactor(pages): migrate list pages to design tokens`
- `refactor(pages): migrate detail pages to design tokens`
- … etc.

## Constraints

- No data-fetching changes.
- No API call changes.
- No server/client component boundary changes — except: removing the inline sign-out button from the four placeholder dashboards eliminates the need for `useAuth`, allowing them to drop `"use client"` and become server components.
- All user-visible strings stay in `useTranslations` / `getTranslations`.
- New user-facing text (stat labels, breadcrumb labels, back-button labels) gets i18n keys in both `en.json` and `es.json`.

## Out of Scope

- Component internals inside `src/components/`.
- Auth shell DRY-up.
- Shared page-shell component extraction.
- Real stat values on placeholder dashboards (deferred until aggregation APIs).
- Removing unused `_var` placeholders (e.g. `useAuth`'s `user` variable left over after sign-out removal — drop on sight per project rules, not a separate task).
