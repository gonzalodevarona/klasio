# Compound Component Migration to Design System — Design Spec

**Date:** 2026-04-24
**Branch:** `feature/full-redesign`
**Predecessors:**
- `docs/superpowers/plans/2026-04-24-design-system-migration.md` (Step 1: tokens)
- `docs/superpowers/plans/2026-04-24-ui-primitives.md` (Step 2: primitives)

This spec defines Step 3 of the redesign rollout: migrating existing compound components to consume the new `ui/` primitives and Klasio design tokens, without changing business logic.

---

## Goal

Refactor 38 existing compound components to use the new `ui/` primitives and `k-*` design tokens, with surgical edits only. No changes to hooks, data fetching, state, props interfaces, types, i18n keys, or any `app/**` page file.

## Non-Goals

- Renaming or removing any existing component
- Changing public props of any compound component
- Touching `app/**` page files
- Adding new features or refactoring business logic
- Restyling beyond token swaps and primitive substitution
- Migrating remaining compound components not listed in the file map (deferred to follow-up)

---

## Architecture

Five-phase migration on `feature/full-redesign`. One commit per phase.

```
Step 0   Badge primitive extension                     1 file + 1 test
Group A  Badge wrapper migration                       12 files
Group B  List/table view migration                     10 files
Group C  Form migration                                 9 files
Group D  Modal migration                                7 files
```

**Tech stack:** React 19, Next.js 15.1, TypeScript 5.9, Tailwind 3.4, Jest 29, `next-intl`. All primitives already exist under `web/src/components/ui/` and re-exported from `web/src/components/ui/index.ts`.

**Hard constraints:**
- Zero changes to hooks, state, data fetching, props interfaces, types, i18n keys
- No `app/**` files modified
- Public component names and exports unchanged
- All 12 badge wrapper components retained — `<Badge>` primitive used internally only
- Existing `"use client"` directives preserved

**Verification gate (per phase):**
```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
```
Final phase additionally runs `npm run build` and a manual dev-server spot-check.

---

## File Map

| Phase | Action | Path |
|---|---|---|
| Step 0 | Modify | `web/src/components/ui/Badge.tsx` |
| Step 0 | Modify | `web/src/components/ui/__tests__/Badge.test.tsx` |
| Group A | Modify | `web/src/components/attendance/RegistrationStatusBadge.tsx` |
| Group A | Modify | `web/src/components/attendance/SessionStatusBadge.tsx` |
| Group A | Modify | `web/src/components/classes/ClassLevelBadge.tsx` |
| Group A | Modify | `web/src/components/classes/ClassStatusBadge.tsx` |
| Group A | Modify | `web/src/components/classes/ClassTypeBadge.tsx` |
| Group A | Modify | `web/src/components/enrollments/LevelBadge.tsx` |
| Group A | Modify | `web/src/components/memberships/MembershipStatusBadge.tsx` |
| Group A | Modify | `web/src/components/payment-proofs/ProofStatusBadge.tsx` |
| Group A | Modify | `web/src/components/professors/ProfessorStatusBadge.tsx` |
| Group A | Modify | `web/src/components/programs/ProgramStatusBadge.tsx` |
| Group A | Modify | `web/src/components/students/StudentStatusBadge.tsx` |
| Group A | Modify | `web/src/components/tenants/TenantStatusBadge.tsx` |
| Group B | Modify | `web/src/components/students/StudentList.tsx` |
| Group B | Modify | `web/src/components/professors/ProfessorList.tsx` |
| Group B | Modify | `web/src/components/programs/ProgramList.tsx` |
| Group B | Modify | `web/src/components/classes/ClassList.tsx` |
| Group B | Modify | `web/src/components/admins/AdminList.tsx` |
| Group B | Modify | `web/src/components/managers/ManagerList.tsx` |
| Group B | Modify | `web/src/components/tenants/TenantList.tsx` |
| Group B | Modify | `web/src/components/memberships/MembershipList.tsx` |
| Group B | Modify | `web/src/components/enrollments/EnrollmentList.tsx` |
| Group B | Modify | `web/src/components/payment-proofs/ProofQueue.tsx` |
| Group C | Modify | `web/src/components/students/StudentForm.tsx` |
| Group C | Modify | `web/src/components/professors/ProfessorForm.tsx` |
| Group C | Modify | `web/src/components/programs/ProgramForm.tsx` |
| Group C | Modify | `web/src/components/classes/ClassForm.tsx` |
| Group C | Modify | `web/src/components/memberships/MembershipForm.tsx` |
| Group C | Modify | `web/src/components/auth/LoginForm.tsx` |
| Group C | Modify | `web/src/components/auth/ForgotPasswordForm.tsx` |
| Group C | Modify | `web/src/components/auth/ResetPasswordForm.tsx` |
| Group C | Modify | `web/src/components/auth/SetupAccountForm.tsx` |
| Group D | Modify | `web/src/components/admins/CreateAdminModal.tsx` |
| Group D | Modify | `web/src/components/admins/EditAdminModal.tsx` |
| Group D | Modify | `web/src/components/managers/CreateManagerModal.tsx` |
| Group D | Modify | `web/src/components/managers/EditManagerModal.tsx` |
| Group D | Modify | `web/src/components/professors/CreateProfessorModal.tsx` |
| Group D | Modify | `web/src/components/professors/EditProfessorModal.tsx` |
| Group D | Modify | `web/src/components/payment-proofs/ProofReviewModal.tsx` |

No new files are created. No files are deleted.

---

## Step 0 — Badge primitive extension

The Badge primitive ships with 9 variants (`active, expiring, inactive, pending, approved, rejected, beginner, intermediate, advanced`). Existing compound badges include 7 visual states with no clean match. Step 0 closes the gap with one new variant + two new optional props that consume existing design tokens — no `tailwind.config.ts` change.

### Changes to `Badge.tsx`

**New variant:**
```ts
info: "bg-k-info-bg text-k-info-text",
```

**New optional props on `BadgeProps`:**
```ts
icon?: React.ReactNode;   // rendered after label, inside the same span
title?: string;           // forwarded to the <span> for native tooltip
```

**Updated implementation skeleton:**
```tsx
export function Badge({ variant, label, icon, small, className, title }: BadgeProps) {
  return (
    <span
      title={title}
      className={cn(
        "rounded-full font-semibold inline-flex items-center gap-1",
        small ? "text-[10px] px-2 py-px" : "text-[11px] px-2.5 py-0.5",
        VARIANT_CLASSES[variant],
        className,
      )}
    >
      {label}
      {icon}
    </span>
  );
}
```

`gap-1` is added to the span so an icon adjacent to the label has consistent spacing. When `icon` is undefined, the gap class is harmless (no second child rendered).

### Changes to `Badge.test.tsx`

Three new tests:
1. Renders `bg-k-info-bg text-k-info-text` for `variant="info"`.
2. Renders an icon node passed via `icon` prop.
3. Forwards `title` prop to the underlying `<span>`.

### Variant gap resolution table

| Domain status | Variant chosen | Note |
|---|---|---|
| PENDING_PAYMENT | `rejected` | Red conveys urgency / needs action |
| PENDING_PAYMENT_VALIDATION | `pending` | Collapses with PENDING_MANAGER_ACTIVATION |
| PRESENT | `info` | New blue variant |
| PRESENT_NO_HOURS | `pending` | Collapse accepted |
| ALERTED | `inactive` + `icon` | Amber `Flag` icon provides differentiation |
| RECURRING | `info` | New blue variant |
| ONE_TIME | `inactive` | `// TODO: no purple token in design system` comment |

### Verification

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
npm test -- src/components/ui/__tests__/Badge.test.tsx
```

Both must pass.

### Commit

```
feat(ui): extend Badge primitive with info variant and icon slot
```

---

## Group A — Badge wrapper migration

12 files. Each wrapper retains its component name, default export, and props interface. Internally the wrapper replaces its inline `<span>` with `<Badge>` from `@/components/ui` and replaces its `STATUS_STYLES` color map with a `STATUS_VARIANT` map of type `Record<DomainStatus, BadgeVariant>`.

### Standard pattern

Before:
```tsx
const STATUS_STYLES: Record<FooStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-red-100 text-red-800",
};

export default function FooStatusBadge({ status }: Props) {
  const t = useTranslations("badges.fooStatus");
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}>
      {t(status)}
    </span>
  );
}
```

After:
```tsx
import { Badge, BadgeVariant } from "@/components/ui";

const STATUS_VARIANT: Record<FooStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function FooStatusBadge({ status }: Props) {
  const t = useTranslations("badges.fooStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

### Per-file variant mapping

| File | Mapping |
|---|---|
| `StudentStatusBadge.tsx` | ACTIVE→`active`, INACTIVE→`inactive` |
| `TenantStatusBadge.tsx` | ACTIVE→`active`, INACTIVE→`inactive` (no `useTranslations`; `label={status}` direct) |
| `ProgramStatusBadge.tsx` | ACTIVE→`active`, INACTIVE→`inactive` |
| `ClassStatusBadge.tsx` | ACTIVE→`active`, INACTIVE→`inactive` |
| `ProfessorStatusBadge.tsx` | INVITED→`pending`, ACTIVE→`active`, DEACTIVATED→`rejected` |
| `ProofStatusBadge.tsx` | PENDING→`pending`, APPROVED→`approved`, REJECTED→`rejected`, SUPERSEDED→`inactive` |
| `LevelBadge.tsx` (enrollments) | BEGINNER→`beginner`, INTERMEDIATE→`intermediate`, ADVANCED→`advanced` |
| `ClassLevelBadge.tsx` | BEGINNER→`beginner`, INTERMEDIATE→`intermediate`, ADVANCED→`advanced` (semantic shift: was red, now volt) |
| `ClassTypeBadge.tsx` | RECURRING→`info`, ONE_TIME→`inactive` + `// TODO: no purple token` |
| `MembershipStatusBadge.tsx` | EXPIRED→`rejected`, INACTIVE→`inactive`, PENDING_PAYMENT→`rejected`, PENDING_PAYMENT_VALIDATION→`pending`, PENDING_MANAGER_ACTIVATION→`pending`, ACTIVE→`active` |
| `RegistrationStatusBadge.tsx` | REGISTERED→`active`, CANCELLED_BY_STUDENT→`inactive`, CANCELLED_BY_SYSTEM→`inactive`, SESSION_CANCELLED→`rejected`, PRESENT→`info`, PRESENT_NO_HOURS→`pending`, ABSENT→`rejected` |
| `SessionStatusBadge.tsx` | Special case — see below |

### `SessionStatusBadge.tsx` special case

Existing component branches on `status` and renders three different `<span>` shapes (one with `XCircle`, one with `Flag`, one plain). Migration preserves the branch structure but each branch returns a `<Badge>`:

```tsx
import { Flag, XCircle } from "lucide-react";
import { Badge } from "@/components/ui";

if (status === "CANCELLED") {
  return (
    <Badge
      variant="rejected"
      label={t("CANCELLED")}
      title={reason ?? undefined}
      icon={<XCircle className="w-3.5 h-3.5" />}
    />
  );
}

if (status === "ALERTED") {
  return (
    <Badge
      variant="inactive"
      label={t("ALERTED")}
      title={reason ?? undefined}
      icon={<Flag className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />}
    />
  );
}

return <Badge variant="inactive" label={t("SCHEDULED")} />;
```

The existing `<XCircle>` rendered before the label; under the new pattern it renders after. This is an acceptable cosmetic shift — the icon still pairs with the label. If reviewers flag it, the future fix is a `iconPosition?: "leading" | "trailing"` prop on `Badge`, deferred.

### Verification

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
```

### Commit

```
refactor(ui): migrate badge components to Badge primitive
```

---

## Group B — List/table view migration

10 files. Replace native `<table>` markup with `Table/Thead/Th/Tr/Td` primitives. Replace filter `<input>`/`<select>` with `Input`/`Select`. Replace action `<button>` elements with `Button`. Pagination logic is untouched (the `Pagination` primitive may already be wired; if a list uses raw pagination markup, leave it for a follow-up — out of scope here unless a list contains a manual `<button>` row that fits the `Button` swap rule).

### Standard pattern

Before:
```tsx
<table className="min-w-full divide-y divide-gray-200">
  <thead className="bg-gray-50">
    <tr>
      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
    </tr>
  </thead>
  <tbody>
    <tr className="hover:bg-gray-50 cursor-pointer" onClick={onRowClick}>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{item.name}</td>
    </tr>
  </tbody>
</table>

<input
  className="border border-gray-300 rounded-md px-3 py-2 text-sm"
  placeholder="Search..."
  value={search}
  onChange={e => setSearch(e.target.value)}
/>

<button className="text-sm text-gray-600 hover:text-gray-900" onClick={onEdit}>Edit</button>
```

After:
```tsx
import { Table, Thead, Th, Tr, Td, Input, Select, Button } from "@/components/ui";

<Table>
  <Thead>
    <tr>
      <Th>Name</Th>
    </tr>
  </Thead>
  <tbody>
    <Tr onClick={onRowClick}>
      <Td>{item.name}</Td>
    </Tr>
  </tbody>
</Table>

<Input placeholder="Search..." value={search} onChange={e => setSearch(e.target.value)} />

<Button variant="ghost" size="sm" onClick={onEdit}>Edit</Button>
```

### Substitution rules

- `<table>` (or its wrapper `<div className="overflow-x-auto">`) → `<Table>` (the primitive owns the overflow wrapper, the border, and the rounded corners).
- `<thead>` → `<Thead>`. The `<tr>` directly inside `<Thead>` stays as a native `<tr>`.
- `<th>` → `<Th>`. Use `right` prop instead of `text-right` Tailwind class.
- `<tbody>` stays native — there is no `Tbody` primitive (`Table` only renders the outer wrapper + `<table>`).
- Body `<tr>` → `<Tr>`. Pass `onClick` directly; `Tr` adds `cursor-pointer hover:bg-k-surface` automatically when `onClick` is present.
- `<td>` → `<Td>`. Use `mono`, `muted`, `bold`, `right` boolean props instead of inline classes.
- Filter inputs without a label → `<Input placeholder=... value=... onChange=...>` (no `label` prop).
- Filter selects → `<Select value=... onChange=...><option>...</option></Select>`.
- Row action buttons (Edit, Delete, View) → `<Button variant="ghost" size="sm" onClick={...}>`.
- Primary CTAs ("New Student", "Add Class") at the top of a list page → `<Button variant="primary" size="sm">` if it appears inside the list component itself; if it lives in a page file (`app/**`), do not touch it (out of scope).
- Status/level badges inside cells: replace inline class spans with the corresponding compound badge component (already migrated in Group A) — do not inline `<Badge>` directly. This preserves call-site stability.

### Out of scope per file

- Empty-state layout, loading skeletons, error banners — leave as-is unless they contain a `<button>` action that fits the swap rule.
- Custom column widths via `colgroup` — preserve as-is.
- Sticky headers — `Thead` does not currently support `sticky`; preserve any existing sticky wrapper around `<Table>`.

### Verification

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
```

### Commit

```
refactor(ui): migrate list components to ui primitives
```

---

## Group C — Form migration

9 files. Replace `<input>`, `<select>`, and `<button type="submit">` markup with `Input`, `Select`, and `Button` primitives. Form state, validation, and submit handlers are untouched.

### Standard pattern

Before:
```tsx
<div>
  <label htmlFor="name" className="block text-sm font-medium text-gray-700">Name</label>
  <input
    id="name"
    type="text"
    value={form.name}
    onChange={e => setForm({ ...form, name: e.target.value })}
    className="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 text-sm"
  />
  {errors.name && <p className="text-sm text-red-600">{errors.name}</p>}
</div>

<select
  className="border border-gray-300 rounded-md px-3 py-2 text-sm w-full"
  value={form.programId}
  onChange={e => setForm({ ...form, programId: e.target.value })}
>
  <option value="">Select...</option>
</select>

<button
  type="submit"
  className="bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium"
>
  Save
</button>
```

After:
```tsx
import { Input, Select, Button } from "@/components/ui";

<Input
  label="Name"
  type="text"
  value={form.name}
  onChange={e => setForm({ ...form, name: e.target.value })}
  error={errors.name}
/>

<Select
  label="Program"
  value={form.programId}
  onChange={e => setForm({ ...form, programId: e.target.value })}
>
  <option value="">Select...</option>
</Select>

<Button variant="volt" type="submit">Save</Button>
```

### Substitution rules

- `<label>` + `<input>` + error `<p>` collapse into a single `<Input label={} error={}>`. The `<Input>` primitive renders its own label, error, and hint.
- Existing `htmlFor`/`id` pairs are removed — `Input` generates its own id via `useId()`. If a `for=` value is used elsewhere (e.g. in an `aria-describedby`), pass the existing `id` explicitly via the `id` prop — `Input` honors it.
- `<select>` → `<Select label={...}>`. Children `<option>` elements stay as native.
- Submit buttons → `<Button variant="volt" type="submit">`. The `volt` variant is the Klasio primary CTA color.
- Cancel / secondary buttons → `<Button variant="outline" type="button">`.
- Destructive buttons (Delete, Reject) → `<Button variant="danger" type="button">`.
- `type="submit"` is preserved on `Button` — the primitive forwards `...rest` to the underlying `<button>`.
- Multi-select / `<input type="date">` / `<input type="file">` — keep as raw elements with `className` swapped to `k-*` tokens only (`bg-k-surface border-k-border rounded-k-sm`). Add `// TODO: no primitive for type=<x>` comment.
- `<textarea>` — keep as raw element with `k-*` token classes; no primitive exists for textarea.
- Password fields — `<Input type="password" ...>`.
- Real-time password policy checker (in `ResetPasswordForm`, `SetupAccountForm`) — leave its rendering logic untouched; only the underlying input swaps to `<Input>`.

### Verification

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
```

### Commit

```
refactor(ui): migrate forms to ui primitives
```

---

## Group D — Modal migration

7 files. Replace ad-hoc modal chrome (overlay div + panel div + header + close button) with the `Modal` primitive. Inner content — form fields, submit buttons, confirm text — passes through `children` unchanged.

### Standard pattern

Before:
```tsx
{isOpen && (
  <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
    <div
      className="bg-white rounded-lg shadow-xl w-full max-w-md p-6"
      onClick={e => e.stopPropagation()}
    >
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold text-gray-900">Create Admin</h2>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
      </div>
      {/* inner content */}
    </div>
  </div>
)}
```

After:
```tsx
import { Modal } from "@/components/ui";

<Modal open={isOpen} onClose={onClose} title="Create Admin" size="md">
  {/* inner content — untouched */}
</Modal>
```

### Substitution rules

- The Modal primitive owns: overlay, click-outside-to-close, panel container, header with title + close button, Escape-key handler, scroll containment, ARIA dialog attributes.
- The inner content passes through `children`. Form fields, submit/cancel button rows, confirmation text — none of it changes.
- `size` prop selection:

  | Existing panel max-w | Modal `size` |
  |---|---|
  | `max-w-sm` or `max-w-md` (or no width) | `"md"` |
  | `max-w-lg` | `"lg"` |
  | `max-w-xl` or wider | `"xl"` |
  | Tight modals (≤ 400px) | `"sm"` |

- Existing `"use client"` directives on modal files are preserved.
- If the consumer uses a different prop name (`isOpen`, `visible`, etc.), keep that prop on the consumer component and map it at the call site: `<Modal open={isOpen} ... />`.
- If a modal currently has no close button (rare — confirmation dialogs that close only on action) — Modal still renders one. This is a deliberate accessibility improvement, not a regression.
- Inline submit/cancel button rows inside the modal body should also be migrated to `<Button>` if they were not already migrated as part of Group C (these forms live inside modals). If the modal body imports a separate `Form` component already migrated in Group C, no extra change is needed.

### Verification

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
npm run build
```

`npm run build` runs once at the end of Group D as the final-phase gate.

### Manual dev-server spot-check

After Group D commits, run `npm run dev` and visually verify, at minimum:

- One list page from each major domain (Students, Programs, Classes, Memberships, Payment Proofs)
- One form page (LoginForm, StudentForm)
- One modal flow (CreateProfessorModal)
- One badge-heavy view (Memberships list with mixed statuses)

Spot-check criteria: no broken layouts, no missing styles, no `<span>` rendering raw class names. The check is informal — record findings as a comment in the eventual PR description.

### Commit

```
refactor(ui): migrate modals to Modal primitive
```

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| Badge variant collapse (3 pending shades → 1) is visually confusing to QA | Medium | Documented in spec; future variant additions are cheap. SessionStatusBadge differentiates with icon. |
| `<Input>` primitive renders its own `<label>` and changes spacing | Medium | The Input primitive uses `text-xs uppercase mono` labels — visually different from existing `text-sm font-medium`. This is the intended Klasio design language. Acceptable. |
| Submit `<Button variant="volt">` is bright lime — clashes with existing dark themes | Low | Volt is the Klasio primary CTA color per design system; this is the intended outcome. |
| `Modal` primitive size mapping wrong for a specific modal | Low | tsc + manual spot-check; size is easy to adjust post-merge. |
| `Tr onClick` adds hover style that didn't exist on a non-interactive row | Low | Only added when `onClick` is present, matching previous behavior. |
| `Th` mono font + uppercase + tracking changes header look | Medium | Intentional Klasio design language. Pre-approved via primitive contract. |
| A list page uses pagination markup the migration plan doesn't cover | Low | Out of scope — leave alone, flag for follow-up. |
| Form has exotic input (`type="date"`, file upload) | Medium | Rule is explicit: keep as raw, swap classes to `k-*` tokens, add TODO. |

---

## Out of Scope

- All `app/**` page files (per user constraint)
- `Pagination` primitive adoption inside list components (separate effort)
- Replacing `<textarea>` (no primitive)
- Replacing exotic input types (date, file)
- Adding new Badge variants beyond `info` (e.g. purple) — deferred unless required
- Restyling notifications (`NotificationBell`, `/notifications`) — not in file map
- Restyling sidebar, top nav — not in file map
- Migrating dashboard components — not in file map

---

## Acceptance Criteria

- All 5 commits present on `feature/full-redesign` in order: Step 0, Group A, Group B, Group C, Group D.
- `npx tsc --noEmit` exits 0 after each commit.
- `npm run build` exits 0 after Group D.
- `npm test -- src/components/ui/__tests__/Badge.test.tsx` passes after Step 0 with the 3 new tests.
- No file under `web/src/app/**` has changes (verifiable via `git diff --stat origin/feature/full-redesign... -- web/src/app/`).
- No public component name, default-export status, or props interface changed (verifiable via grep / type errors at consumer call sites).
- Manual dev-server spot-check completed; findings noted.

---

## Rollback Strategy

Each phase is one commit. To roll back any phase: `git revert <commit-sha>`. Phases are independent of each other in the sense that reverting Group D leaves Steps 0/A/B/C intact; reverting Group A invalidates downstream groups only where badges appear inside lists/modals (TypeScript will compile, but visuals revert to legacy).

The Step 0 commit must not be reverted in isolation while later groups remain — Group A consumes the `info` variant and `icon` prop. Always revert in reverse order if multiple phases need to be undone.
