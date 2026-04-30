# Compound Component Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate 38 existing compound components to consume the new `ui/` primitives and `k-*` design tokens via surgical edits — zero changes to hooks, state, data fetching, props interfaces, types, i18n keys, or any `app/**` page file.

**Architecture:** Five-phase migration on `feature/full-redesign` with one commit per phase. Step 0 extends the Badge primitive (1 new variant + 2 new optional props) using only existing tokens. Groups A–D then refactor 38 compound files in dependency order: badges → lists → forms → modals. Each group passes `npx tsc --noEmit` before commit; Group D additionally runs `npm run build` and a manual dev-server spot-check.

**Tech Stack:** React 19, Next.js 15.1, TypeScript 5.9, Tailwind 3.4, Jest 29, `next-intl`, `lucide-react`. All primitives already exist under `web/src/components/ui/` and re-export from `web/src/components/ui/index.ts`.

**Spec reference:** `docs/superpowers/specs/2026-04-24-compound-component-migration-design.md`

---

## File Map

| Phase | Action | Path |
|---|---|---|
| Step 0 | Modify | `web/src/components/ui/Badge.tsx` |
| Step 0 | Modify | `web/src/components/ui/__tests__/Badge.test.tsx` |
| Group A | Modify | `web/src/components/students/StudentStatusBadge.tsx` |
| Group A | Modify | `web/src/components/tenants/TenantStatusBadge.tsx` |
| Group A | Modify | `web/src/components/programs/ProgramStatusBadge.tsx` |
| Group A | Modify | `web/src/components/classes/ClassStatusBadge.tsx` |
| Group A | Modify | `web/src/components/professors/ProfessorStatusBadge.tsx` |
| Group A | Modify | `web/src/components/payment-proofs/ProofStatusBadge.tsx` |
| Group A | Modify | `web/src/components/enrollments/LevelBadge.tsx` |
| Group A | Modify | `web/src/components/classes/ClassLevelBadge.tsx` |
| Group A | Modify | `web/src/components/classes/ClassTypeBadge.tsx` |
| Group A | Modify | `web/src/components/memberships/MembershipStatusBadge.tsx` |
| Group A | Modify | `web/src/components/attendance/RegistrationStatusBadge.tsx` |
| Group A | Modify | `web/src/components/attendance/SessionStatusBadge.tsx` |
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

No new files created. No files deleted.

---

## Per-task verification gate

Run from `/Users/gonzalodevarona/Documents/klasio/web` after every file edit (do not commit if it fails):

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
```

Expected: zero output, exit 0. If errors appear, fix the just-edited file before moving on.

---

## Phase 0 — Badge primitive extension

### Task 1: Extend Badge primitive with `info` variant, `icon` prop, and `title` prop

**Files:**
- Modify: `web/src/components/ui/Badge.tsx`
- Modify: `web/src/components/ui/__tests__/Badge.test.tsx`

- [ ] **Step 1: Replace `Badge.tsx` with the extended implementation**

```tsx
import React from "react";
import { cn } from "@/lib/utils";

export type BadgeVariant =
  | "active" | "expiring" | "inactive"
  | "pending" | "approved" | "rejected"
  | "beginner" | "intermediate" | "advanced"
  | "info";

export interface BadgeProps {
  variant: BadgeVariant;
  label: string;
  small?: boolean;
  className?: string;
  icon?: React.ReactNode;
  title?: string;
}

const VARIANT_CLASSES: Record<BadgeVariant, string> = {
  active:       "bg-k-volt text-k-volt-text",
  expiring:     "bg-k-warn-bg text-k-warn-text",
  inactive:     "bg-k-bg text-k-subtle border border-k-border",
  pending:      "bg-k-warn-bg text-k-warn-text",
  approved:     "bg-k-volt text-k-volt-text",
  rejected:     "bg-k-danger-bg text-k-danger-text",
  beginner:     "bg-k-info-bg text-k-info-text",
  intermediate: "bg-k-warn-bg text-k-warn-text",
  advanced:     "bg-k-volt text-k-volt-text",
  info:         "bg-k-info-bg text-k-info-text",
};

export function Badge({ variant, label, small, className, icon, title }: BadgeProps) {
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

Changes from original: added `"info"` to `BadgeVariant`; added `icon?: React.ReactNode` and `title?: string` to `BadgeProps`; added `info` row to `VARIANT_CLASSES`; added `gap-1` to base classes; added `title` attribute to `<span>`; rendered `{icon}` after `{label}`.

- [ ] **Step 2: Replace `Badge.test.tsx` with extended test suite**

```tsx
import React from "react";
import { render, screen } from "@testing-library/react";
import { Badge, type BadgeVariant } from "../Badge";

describe("Badge", () => {
  it("renders the label as text", () => {
    render(<Badge variant="active" label="Active" />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("applies base pill classes", () => {
    render(<Badge variant="active" label="Active" />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("rounded-full");
    expect(el).toHaveClass("font-semibold");
    expect(el).toHaveClass("inline-flex");
    expect(el).toHaveClass("items-center");
    expect(el).toHaveClass("gap-1");
  });

  it("applies default size classes", () => {
    render(<Badge variant="active" label="Active" />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("text-[11px]");
    expect(el).toHaveClass("px-2.5");
    expect(el).toHaveClass("py-0.5");
  });

  it("applies small size classes when small=true", () => {
    render(<Badge variant="active" label="Active" small />);
    const el = screen.getByText("Active");
    expect(el).toHaveClass("text-[10px]");
    expect(el).toHaveClass("px-2");
    expect(el).toHaveClass("py-px");
  });

  const cases: Array<{
    variant: BadgeVariant;
    expected: string[];
  }> = [
    { variant: "active",       expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "expiring",     expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "inactive",     expected: ["bg-k-bg", "text-k-subtle", "border", "border-k-border"] },
    { variant: "pending",      expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "approved",     expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "rejected",     expected: ["bg-k-danger-bg", "text-k-danger-text"] },
    { variant: "beginner",     expected: ["bg-k-info-bg", "text-k-info-text"] },
    { variant: "intermediate", expected: ["bg-k-warn-bg", "text-k-warn-text"] },
    { variant: "advanced",     expected: ["bg-k-volt", "text-k-volt-text"] },
    { variant: "info",         expected: ["bg-k-info-bg", "text-k-info-text"] },
  ];

  cases.forEach(({ variant, expected }) => {
    it(`variant="${variant}" applies correct color classes`, () => {
      render(<Badge variant={variant} label="X" />);
      const el = screen.getByText("X");
      expected.forEach((cls) => expect(el).toHaveClass(cls));
    });
  });

  it("merges caller className", () => {
    render(<Badge variant="active" label="X" className="custom-class" />);
    expect(screen.getByText("X")).toHaveClass("custom-class");
  });

  it("renders an icon node passed via the icon prop", () => {
    render(
      <Badge
        variant="active"
        label="WithIcon"
        icon={<svg data-testid="badge-icon" />}
      />,
    );
    expect(screen.getByTestId("badge-icon")).toBeInTheDocument();
  });

  it("forwards the title prop to the underlying span", () => {
    render(<Badge variant="active" label="X" title="tooltip text" />);
    expect(screen.getByText("X")).toHaveAttribute("title", "tooltip text");
  });
});
```

Changes from original: added `"gap-1"` assertion in base-classes test; added `info` row to the `cases` array; added two new tests (`renders an icon node passed via the icon prop`, `forwards the title prop to the underlying span`).

- [ ] **Step 3: Verify TypeScript and tests pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npx tsc --noEmit
npm test -- src/components/ui/__tests__/Badge.test.tsx
```

Expected: tsc exits 0 with no output. Jest reports all 14 tests passing (4 existing structural + 10 variant cases + 1 className + 2 new icon/title).

- [ ] **Step 4: Commit Phase 0**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add web/src/components/ui/Badge.tsx web/src/components/ui/__tests__/Badge.test.tsx
git commit -m "feat(ui): extend Badge primitive with info variant and icon slot"
```

---

## Phase A — Badge wrapper migration (12 files)

Each Group A task replaces a wrapper's inline `<span>` with `<Badge>` from `@/components/ui` and replaces its `STATUS_STYLES` (or `LEVEL_STYLES`/`TYPE_STYLES`) `Record<…, string>` map with a `STATUS_VARIANT` (or analogous) `Record<…, BadgeVariant>` map. The wrapper's component name, default-export status, and props interface are preserved.

### Task 2: Migrate `StudentStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/students/StudentStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { StudentStatus } from "@/lib/types/student";

interface StudentStatusBadgeProps {
  status: StudentStatus;
}

const STATUS_VARIANT: Record<StudentStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function StudentStatusBadge({ status }: StudentStatusBadgeProps) {
  const t = useTranslations("badges.studentStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 3: Migrate `TenantStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/tenants/TenantStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
import { Badge, type BadgeVariant } from "@/components/ui";
import { TenantStatus } from "@/lib/types/tenant";

interface TenantStatusBadgeProps {
  status: TenantStatus;
}

const STATUS_VARIANT: Record<TenantStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function TenantStatusBadge({ status }: TenantStatusBadgeProps) {
  return <Badge variant={STATUS_VARIANT[status]} label={status} />;
}
```

Note: this file has no `useTranslations` call in the original — `label={status}` is intentional, matching legacy behavior.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 4: Migrate `ProgramStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/programs/ProgramStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ProgramStatus } from "@/lib/types/program";

interface ProgramStatusBadgeProps {
  status: ProgramStatus;
}

const STATUS_VARIANT: Record<ProgramStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function ProgramStatusBadge({ status }: ProgramStatusBadgeProps) {
  const t = useTranslations("badges.programStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 5: Migrate `ClassStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/classes/ClassStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ClassStatus } from "@/lib/types/programClass";

interface ClassStatusBadgeProps {
  status: ClassStatus;
}

const STATUS_VARIANT: Record<ClassStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function ClassStatusBadge({ status }: ClassStatusBadgeProps) {
  const t = useTranslations("badges.classStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 6: Migrate `ProfessorStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/professors/ProfessorStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ProfessorStatus } from "@/lib/types/professor";

interface ProfessorStatusBadgeProps {
  status: ProfessorStatus;
}

const STATUS_VARIANT: Record<ProfessorStatus, BadgeVariant> = {
  INVITED:     "pending",
  ACTIVE:      "active",
  DEACTIVATED: "rejected",
};

export default function ProfessorStatusBadge({ status }: ProfessorStatusBadgeProps) {
  const t = useTranslations("badges.professorStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 7: Migrate `ProofStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/payment-proofs/ProofStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ProofStatus } from "@/lib/types/paymentProof";

const STATUS_VARIANT: Record<ProofStatus, BadgeVariant> = {
  PENDING:    "pending",
  APPROVED:   "approved",
  REJECTED:   "rejected",
  SUPERSEDED: "inactive",
};

interface Props {
  status: ProofStatus;
}

export function ProofStatusBadge({ status }: Props) {
  const t = useTranslations("badges.proofStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

Note: original uses **named** export (`export function ProofStatusBadge`) — preserve that. Other badges use default export.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 8: Migrate `LevelBadge.tsx` (enrollments)

**Files:**
- Modify: `web/src/components/enrollments/LevelBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { Level } from "@/lib/types/enrollment";

interface LevelBadgeProps {
  level: Level;
}

const LEVEL_VARIANT: Record<Level, BadgeVariant> = {
  BEGINNER:     "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED:     "advanced",
};

export default function LevelBadge({ level }: LevelBadgeProps) {
  const t = useTranslations("badges.enrollmentLevel");
  return <Badge variant={LEVEL_VARIANT[level]} label={t(level)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 9: Migrate `ClassLevelBadge.tsx`

**Files:**
- Modify: `web/src/components/classes/ClassLevelBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ClassLevel } from "@/lib/types/programClass";

interface ClassLevelBadgeProps {
  level: ClassLevel;
}

const LEVEL_VARIANT: Record<ClassLevel, BadgeVariant> = {
  BEGINNER:     "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED:     "advanced",
};

export default function ClassLevelBadge({ level }: ClassLevelBadgeProps) {
  const t = useTranslations("badges.classLevel");
  return <Badge variant={LEVEL_VARIANT[level]} label={t(level)} />;
}
```

Note: semantic shift accepted — ADVANCED was red in legacy, now uses `advanced` variant which maps to volt/green. Documented in spec.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 10: Migrate `ClassTypeBadge.tsx`

**Files:**
- Modify: `web/src/components/classes/ClassTypeBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ClassType } from "@/lib/types/programClass";

interface ClassTypeBadgeProps {
  type: ClassType;
}

const TYPE_VARIANT: Record<ClassType, BadgeVariant> = {
  RECURRING: "info",
  // TODO: no purple token in design system — collapses with default inactive grey
  ONE_TIME:  "inactive",
};

export default function ClassTypeBadge({ type }: ClassTypeBadgeProps) {
  const t = useTranslations("badges.classType");
  return <Badge variant={TYPE_VARIANT[type]} label={t(type)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 11: Migrate `MembershipStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/memberships/MembershipStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { MembershipStatus } from "@/lib/types/membership";

interface MembershipStatusBadgeProps {
  status: MembershipStatus;
}

const STATUS_VARIANT: Record<MembershipStatus, BadgeVariant> = {
  EXPIRED:                    "rejected",
  INACTIVE:                   "inactive",
  PENDING_PAYMENT:            "rejected",
  PENDING_PAYMENT_VALIDATION: "pending",
  PENDING_MANAGER_ACTIVATION: "pending",
  ACTIVE:                     "active",
};

export default function MembershipStatusBadge({ status }: MembershipStatusBadgeProps) {
  const t = useTranslations("badges.membershipStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
```

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 12: Migrate `RegistrationStatusBadge.tsx`

**Files:**
- Modify: `web/src/components/attendance/RegistrationStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { RegistrationStatus } from "@/lib/types/attendance";

interface RegistrationStatusBadgeProps {
  status: RegistrationStatus;
}

const STATUS_VARIANT: Record<RegistrationStatus, BadgeVariant> = {
  REGISTERED:           "active",
  CANCELLED_BY_STUDENT: "inactive",
  CANCELLED_BY_SYSTEM:  "inactive",
  SESSION_CANCELLED:    "rejected",
  PRESENT:              "info",
  PRESENT_NO_HOURS:     "pending",
  ABSENT:               "rejected",
};

export default function RegistrationStatusBadge({ status }: RegistrationStatusBadgeProps) {
  const t = useTranslations("badges.registrationStatus");
  const variant = STATUS_VARIANT[status] ?? "inactive";
  return <Badge variant={variant} label={t(status)} />;
}
```

Note: the `?? "inactive"` fallback preserves the legacy fallback behavior for unknown statuses.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 13: Migrate `SessionStatusBadge.tsx` (special case — branches with icons + tooltip)

**Files:**
- Modify: `web/src/components/attendance/SessionStatusBadge.tsx`

- [ ] **Step 1: Replace the file with**

```tsx
"use client";

import { useTranslations } from "next-intl";
import { Flag, XCircle } from "lucide-react";
import { Badge } from "@/components/ui";

interface SessionStatusBadgeProps {
  status: string;
  reason?: string | null;
}

export default function SessionStatusBadge({ status, reason }: SessionStatusBadgeProps) {
  const t = useTranslations("badges.sessionStatus");

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
}
```

Note: the legacy `XCircle` rendered before the label; the new pattern renders it after. Acceptable cosmetic shift — documented in spec.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 14: Commit Phase A

- [ ] **Step 1: Stage all 12 modified badge files and commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add \
  web/src/components/students/StudentStatusBadge.tsx \
  web/src/components/tenants/TenantStatusBadge.tsx \
  web/src/components/programs/ProgramStatusBadge.tsx \
  web/src/components/classes/ClassStatusBadge.tsx \
  web/src/components/professors/ProfessorStatusBadge.tsx \
  web/src/components/payment-proofs/ProofStatusBadge.tsx \
  web/src/components/enrollments/LevelBadge.tsx \
  web/src/components/classes/ClassLevelBadge.tsx \
  web/src/components/classes/ClassTypeBadge.tsx \
  web/src/components/memberships/MembershipStatusBadge.tsx \
  web/src/components/attendance/RegistrationStatusBadge.tsx \
  web/src/components/attendance/SessionStatusBadge.tsx
git commit -m "refactor(ui): migrate badge components to Badge primitive"
```

---

## Phase B — List/table view migration (10 files)

Each task in Phase B opens a list component and applies the same set of transformations:

**Group B substitution rules** (apply to every Phase B file):

1. **Add the import** at the top of the file (next to other component imports):
   ```tsx
   import { Table, Thead, Th, Tr, Td, Input, Select, Button } from "@/components/ui";
   ```
   Only include the symbols actually used by the file.

2. **Table markup**:
   - Replace `<div className="overflow-x-auto …">` + `<table className="…">` with `<Table>`. The `Table` primitive owns the overflow wrapper, the rounded border, and the `<table>` element.
   - Replace `<thead className="…">` with `<Thead>`. Keep the inner `<tr>` as a native `<tr>`.
   - Replace each `<th className="…uppercase…">{label}</th>` with `<Th>{label}</Th>`. If the original was right-aligned, use `<Th right>{label}</Th>`.
   - Keep `<tbody>` as a native element. The `Table` primitive does not wrap it.
   - Replace each body `<tr className="… hover:bg-gray-50 cursor-pointer" onClick={…}>` with `<Tr onClick={…}>`. The `Tr` primitive adds hover/cursor automatically when `onClick` is present.
   - Replace each `<td className="…">{cell}</td>` with `<Td>{cell}</Td>`. Use the boolean props instead of inline classes:
     - `<Td mono>` for `font-mono` cells
     - `<Td muted>` for `text-gray-500`/`text-gray-400` cells
     - `<Td bold>` for `font-medium`/`font-semibold` cells
     - `<Td right>` for right-aligned action columns

3. **Filter inputs/selects**:
   - Replace standalone search `<input className="…" placeholder=… value=… onChange=…>` with `<Input placeholder=… value=… onChange=… />`. No `label` prop unless the original had a visible `<label>` paired with the input.
   - Replace status/filter `<select className="…" value=… onChange=…>…</select>` with `<Select value=… onChange=…>…</Select>`. Children `<option>` elements stay native.
   - If a filter `<label>` element exists outside the input/select (e.g. `<label htmlFor="statusFilter">…</label>`), drop the `<label>` and the `htmlFor`/`id` pair — the `Input`/`Select` primitive can render its own `label` prop instead. If the visible-label text is meaningful for users, pass it as `<Input label={t("…")}>` / `<Select label={t("…")}>`.

4. **Action buttons** inside the list component (not in `app/**` page files):
   - Row actions (Edit/Delete/View): `<Button variant="ghost" size="sm" onClick={…}>{label}</Button>`.
   - "Search" / "Clear" filter buttons: `<Button variant="outline" size="sm" type="button" onClick={…}>{label}</Button>`.
   - Pagination Previous/Next buttons: `<Button variant="outline" size="sm" type="button" onClick={…} disabled={…}>{label}</Button>`. The `Button` primitive handles `disabled:opacity-40 disabled:cursor-not-allowed` automatically.
   - Primary CTAs ("Review", "New X"): `<Button variant="primary" size="sm">{label}</Button>`.

5. **Out of scope, leave unchanged:**
   - Loading skeletons, empty-state copy, error banners that are pure `<div>` markup (their internal `<button>`s, if any, do swap).
   - Inline `<span className="bg-…">` membership/status indicators in cells — `<span>` is not in the substitution list.
   - `<colgroup>`, sticky-header wrappers, custom column widths — preserve.
   - `<Link>` and `<a>` elements — preserve.
   - `useEffect`, hooks, state, API calls — preserve.

After applying these rules, run the verification gate.

### Task 15: Migrate `StudentList.tsx`

**Files:**
- Modify: `web/src/components/students/StudentList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/students/StudentList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above. Specifically:**

  - Add `import { Table, Thead, Th, Tr, Td, Input, Select, Button } from "@/components/ui";` after the existing component imports.
  - Replace the standalone `<label htmlFor="statusFilter">` + `<select id="statusFilter">` block with a single `<Select label={t("filterStatusLabel")} value={statusFilter ?? ""} onChange={(e) => handleStatusChange(e.target.value)}>` containing the existing `<option>` children. Remove `id`, `htmlFor`, and the `<label>` element.
  - Replace the search `<input>` + Search `<button>` + Clear `<button>` block (lines ~89–113) with:
    ```tsx
    <Input
      type="text"
      value={searchInput}
      onChange={(e) => setSearchInput(e.target.value)}
      onKeyDown={handleSearchKeyDown}
      placeholder={t("filterSearchPlaceholder")}
    />
    <Button variant="outline" size="sm" type="button" onClick={handleSearch}>
      {tCommon("search")}
    </Button>
    {search && (
      <Button variant="outline" size="sm" type="button" onClick={handleClearSearch}>
        {tCommon("clear")}
      </Button>
    )}
    ```
  - Wrap the table block: replace `<div className="overflow-x-auto"><table className="min-w-full divide-y divide-gray-200">` and the matching `</table></div>` with `<Table>` … `</Table>`.
  - Replace `<thead className="bg-gray-50">` with `<Thead>`. The inner `<tr>` stays native.
  - Replace each `<th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{label}</th>` with `<Th>{label}</Th>`.
  - Replace `<tbody className="bg-white divide-y divide-gray-200">` with native `<tbody>` (drop the className entirely — the `Tr` primitive owns row borders and hover).
  - Replace each body row `<tr key={student.id} className="hover:bg-gray-50">` with `<Tr key={student.id}>`. (The original has no `onClick`; do not invent one. Hover style is added by the primitive only when `onClick` is passed — for a non-interactive row, `Tr` renders without hover, matching legacy non-interactive behavior except hover-style. Acceptable.)
  - Replace each `<td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{cell}</td>` with `<Td bold>{cell}</Td>` (for the Name column with the `<Link>`).
  - Replace `text-gray-500 font-mono` td → `<Td mono muted>`.
  - Replace `text-gray-500` td → `<Td muted>`.
  - Replace plain `text-gray-900` td → `<Td>`.
  - For the "Membership" cell that contains an inline `<span className="bg-green-100 …">` or `<span className="bg-gray-100 …">`: leave the inline `<span>` markup as-is (out of scope per Phase B rule 5). Wrap only with the new `<Td>{…}</Td>`.
  - Pagination Previous/Next buttons (lines ~203–219): replace each with `<Button variant="outline" size="sm" type="button" onClick={…} disabled={…}>{label}</Button>`.
  - Error banner `<div role="alert" className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200">` (lines 60–68) — leave as-is. (Error banner is `<div>`, not in substitution list; className is gray-* free already.)

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output. If TypeScript complains about an `onChange` mismatch, verify the Input/Select primitives accept the same `ChangeEvent` types as native elements (they do — both extend the native HTML attributes).

---

### Task 16: Migrate `ProfessorList.tsx`

**Files:**
- Modify: `web/src/components/professors/ProfessorList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/professors/ProfessorList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above to all `<table>`, `<thead>`, `<th>`, `<tr>`, `<td>`, filter `<input>`/`<select>`, and action `<button>` elements found in the file.**

Specifically:
- Add `import { Table, Thead, Th, Tr, Td, Input, Select, Button } from "@/components/ui";` (only include symbols used).
- Apply the table-markup transformations (rules B.2).
- Apply filter-input/select transformations (rules B.3).
- Apply action-button transformations (rules B.4).
- Leave loading, empty-state, and error-banner `<div>` blocks as-is unless they contain internal `<button>` elements.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 17: Migrate `ProgramList.tsx`

**Files:**
- Modify: `web/src/components/programs/ProgramList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/programs/ProgramList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above (the rules section preceding Task 15).**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 18: Migrate `ClassList.tsx`

**Files:**
- Modify: `web/src/components/classes/ClassList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/classes/ClassList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 19: Migrate `AdminList.tsx`

**Files:**
- Modify: `web/src/components/admins/AdminList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/admins/AdminList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 20: Migrate `ManagerList.tsx`

**Files:**
- Modify: `web/src/components/managers/ManagerList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/managers/ManagerList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 21: Migrate `TenantList.tsx`

**Files:**
- Modify: `web/src/components/tenants/TenantList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/tenants/TenantList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 22: Migrate `MembershipList.tsx`

**Files:**
- Modify: `web/src/components/memberships/MembershipList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/memberships/MembershipList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 23: Migrate `EnrollmentList.tsx`

**Files:**
- Modify: `web/src/components/enrollments/EnrollmentList.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/enrollments/EnrollmentList.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 24: Migrate `ProofQueue.tsx`

**Files:**
- Modify: `web/src/components/payment-proofs/ProofQueue.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/payment-proofs/ProofQueue.tsx
```

- [ ] **Step 2: Apply Phase B substitution rules above.**

Specifically:
- Add `import { Table, Thead, Th, Tr, Td, Button } from "@/components/ui";`.
- Replace `<div className="overflow-x-auto rounded-lg border border-gray-200">` + `<table className="min-w-full divide-y divide-gray-200">` with `<Table>` … `</Table>`.
- Replace `<thead className="bg-gray-50">` with `<Thead>`.
- Replace each `<th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">` with `<Th>`. The empty header cell `<th className="px-4 py-3" />` becomes `<Th />`.
- Replace `<tbody className="divide-y divide-gray-100 bg-white">` with native `<tbody>`.
- Replace `<tr key={…} className="hover:bg-gray-50">` with `<Tr key={…}>`.
- Replace `<td className="px-4 py-3 text-sm text-gray-900">` → `<Td>`; `text-gray-700` → `<Td>` (no muted prop — `text-gray-700` is regular body); `text-gray-500` → `<Td muted>`; `text-xs text-gray-500` → `<Td muted>`; right-aligned action `<td className="px-4 py-3 text-right">` → `<Td right>`.
- Replace the "Review" button `<button className="rounded-md bg-indigo-600 …">` with `<Button variant="primary" size="sm" onClick={() => setSelected(item)}>{t("reviewBtn")}</Button>`.
- Leave error banner `<div className="rounded-md bg-red-50 …">` and loading/empty `<p>` lines as-is.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 25: Commit Phase B

- [ ] **Step 1: Stage all 10 modified list files and commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add \
  web/src/components/students/StudentList.tsx \
  web/src/components/professors/ProfessorList.tsx \
  web/src/components/programs/ProgramList.tsx \
  web/src/components/classes/ClassList.tsx \
  web/src/components/admins/AdminList.tsx \
  web/src/components/managers/ManagerList.tsx \
  web/src/components/tenants/TenantList.tsx \
  web/src/components/memberships/MembershipList.tsx \
  web/src/components/enrollments/EnrollmentList.tsx \
  web/src/components/payment-proofs/ProofQueue.tsx
git commit -m "refactor(ui): migrate list components to ui primitives"
```

---

## Phase C — Form migration (9 files)

**Group C substitution rules** (apply to every Phase C file):

1. **Add the import** at the top of the file:
   ```tsx
   import { Input, Select, Button } from "@/components/ui";
   ```
   Only include the symbols actually used.

2. **Text/email/password inputs with paired `<label>`**:
   Replace this triplet:
   ```tsx
   <div>
     <label htmlFor="<id>" className="…">{labelText}</label>
     <input
       id="<id>"
       type="<type>"
       value={…}
       onChange={…}
       required (other native attrs)
       className="…"
       placeholder="…"
     />
     {error && <p className="text-sm text-red-600">{error}</p>}
   </div>
   ```
   With:
   ```tsx
   <Input
     label={labelText}
     type="<type>"
     value={…}
     onChange={…}
     required
     placeholder="…"
     error={error}
   />
   ```
   Drop the wrapping `<div>`, the `<label>` element, the `htmlFor`/`id` pair, the inline `className`, and the error `<p>`. The `Input` primitive owns label, error, and base styling.

3. **Selects**:
   Replace:
   ```tsx
   <select className="…" value={…} onChange={…} required>…options…</select>
   ```
   With:
   ```tsx
   <Select label={labelText} value={…} onChange={…} required>…options…</Select>
   ```
   Children `<option>` elements stay native. If the select had no visible `<label>` (e.g. inline filter), omit the `label` prop.

4. **Submit / cancel / destructive buttons**:
   - `<button type="submit" className="…bg-indigo-600…">` → `<Button variant="volt" type="submit">{label}</Button>`.
   - `<button type="button" className="…bg-white border…">` (cancel/secondary) → `<Button variant="outline" type="button" onClick={…}>{label}</Button>`.
   - Destructive `<button>` (Delete/Reject) → `<Button variant="danger" type="button" onClick={…}>{label}</Button>`.
   - The `Button` primitive forwards `...rest` to the underlying `<button>`, so `disabled`, `aria-label`, etc. pass through.

5. **Out of scope, leave unchanged:**
   - `<input type="date">`, `<input type="file">`, `<input type="checkbox">`, `<input type="radio">`, multi-select widgets — keep as raw elements; swap their inline gray-* classes to `k-*` tokens (`bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm focus:border-k-volt focus:outline-none`). Add `// TODO: no primitive for type="<type>"` as a comment above the element.
   - `<textarea>` — keep raw, swap classes to `k-*` tokens. Same comment.
   - Real-time password policy / validation panels (`PasswordPolicyChecker` consumer) — leave logic and rendering untouched; only the underlying `<input type="password">` swaps to `<Input type="password">` **provided** the field has no inline trailing affordance (see next bullet).
   - Show/hide password toggle button: this pattern relies on an absolutely-positioned `<button>` overlaid on the input, anchored to a `relative` wrapper around the `<input>`. The `Input` primitive's internal layout (label above, input inside its own `relative` div, error below) breaks that anchoring, and the primitive has no trailing-icon slot in the current version. **When a password field has a show/hide toggle, leave the entire field raw**: keep `<div className="relative">` + `<input type={…}>` + `<button>` as-is, but swap the `<input>`'s gray-* classes to `k-*` tokens (`bg-k-surface border border-k-border rounded-k-sm px-3 py-2 pr-10 text-sm focus:border-k-volt focus:outline-none w-full`). Add a comment above the wrapping `<div>`: `// TODO: migrate to <Input> when primitive supports a trailing-icon slot.` This rule applies to `LoginForm`, `ResetPasswordForm`, and `SetupAccountForm`.
   - Error banner `<div role="alert" className="bg-red-50 …">` — leave as-is. Submit-level errors render outside `Input`'s field-level error.
   - Form-level structure (`<form onSubmit={…}>`, `<div className="space-y-…">`, layout grids) — preserve.

After applying these rules, run the verification gate.

### Task 26: Migrate `StudentForm.tsx`

**Files:**
- Modify: `web/src/components/students/StudentForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/students/StudentForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules above to every `<input>`, `<select>`, and submit/cancel `<button>` in the file.**

Watch for: `<input type="date">` (birth date) and any tutor-data fields for minors — keep raw, swap classes to `k-*` tokens, add the `// TODO` comment.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 27: Migrate `ProfessorForm.tsx`

**Files:**
- Modify: `web/src/components/professors/ProfessorForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/professors/ProfessorForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 28: Migrate `ProgramForm.tsx`

**Files:**
- Modify: `web/src/components/programs/ProgramForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/programs/ProgramForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 29: Migrate `ClassForm.tsx`

**Files:**
- Modify: `web/src/components/classes/ClassForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/classes/ClassForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules above.**

Watch for: `<input type="time">` for class start/end times, day-of-week selectors. Treat `type="time"` like `type="date"` — keep raw, swap classes to `k-*` tokens, add `// TODO` comment.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 30: Migrate `MembershipForm.tsx`

**Files:**
- Modify: `web/src/components/memberships/MembershipForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/memberships/MembershipForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 31: Migrate `LoginForm.tsx`

**Files:**
- Modify: `web/src/components/auth/LoginForm.tsx`

- [ ] **Step 1: Apply Phase C substitution rules. Specifically:**

  - Add `import { Input, Button } from "@/components/ui";` after the existing imports.
  - Replace the email `<div>…<label>…<input id="email" type="email" …>` block with:
    ```tsx
    <Input
      label={t("emailLabel")}
      type="email"
      value={email}
      onChange={(e) => setEmail(e.target.value)}
      required
      placeholder={t("emailPlaceholder")}
    />
    ```
  - The password block has a show/hide toggle absolutely positioned over the input. Per Phase C rule 5, leave the entire password field raw — keep the `<div>` + `<label>` + `<div className="relative mt-1">` + `<input id="password" …>` + show/hide `<button>` markup. Only swap the `<input>`'s className from the legacy `block w-full px-3 py-2 pr-10 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm` to `bg-k-surface border border-k-border rounded-k-sm px-3 py-2 pr-10 text-sm focus:border-k-volt focus:outline-none w-full`. Add a comment above `<div className="relative mt-1">`: `// TODO: migrate to <Input> when primitive supports a trailing-icon slot.`
  - Replace the submit `<button type="submit" className="…bg-indigo-600…">` with:
    ```tsx
    <Button variant="volt" type="submit" disabled={loading} className="w-full">
      {loading ? t("submitting") : t("submit")}
    </Button>
    ```
  - Leave the error banner `<div role="alert" className="bg-red-50 …">` as-is.
  - Leave the "Forgot password" `<a>` link as-is.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 32: Migrate `ForgotPasswordForm.tsx`

**Files:**
- Modify: `web/src/components/auth/ForgotPasswordForm.tsx`

- [ ] **Step 1: Apply Phase C substitution rules. Specifically:**

  - Add `import { Input, Button } from "@/components/ui";`.
  - Replace the email field `<div>…<label htmlFor="email">…<input id="email" type="email" …>` with:
    ```tsx
    <Input
      label={t("emailLabel")}
      type="email"
      value={email}
      onChange={(e) => setEmail(e.target.value)}
      required
      placeholder={t("emailPlaceholder")}
    />
    ```
  - Replace the submit `<button type="submit" className="…bg-indigo-600…">` with:
    ```tsx
    <Button variant="volt" type="submit" disabled={loading} className="w-full">
      {loading ? t("submitting") : t("submit")}
    </Button>
    ```
  - Leave the post-submit success `<div className="bg-green-50 …">` and "Back to login" `<a>` blocks as-is.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 33: Migrate `ResetPasswordForm.tsx`

**Files:**
- Modify: `web/src/components/auth/ResetPasswordForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/auth/ResetPasswordForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules.**

Watch for: `PasswordPolicyChecker` import + render — leave its surrounding markup intact, only swap the `<input type="password">` it consumes. The checker reads the input value via state (`password`); swapping to `<Input>` does not affect the binding.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 34: Migrate `SetupAccountForm.tsx`

**Files:**
- Modify: `web/src/components/auth/SetupAccountForm.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/auth/SetupAccountForm.tsx
```

- [ ] **Step 2: Apply Phase C substitution rules.**

Same `PasswordPolicyChecker` caveat as Task 33.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 35: Commit Phase C

- [ ] **Step 1: Stage all 9 modified form files and commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add \
  web/src/components/students/StudentForm.tsx \
  web/src/components/professors/ProfessorForm.tsx \
  web/src/components/programs/ProgramForm.tsx \
  web/src/components/classes/ClassForm.tsx \
  web/src/components/memberships/MembershipForm.tsx \
  web/src/components/auth/LoginForm.tsx \
  web/src/components/auth/ForgotPasswordForm.tsx \
  web/src/components/auth/ResetPasswordForm.tsx \
  web/src/components/auth/SetupAccountForm.tsx
git commit -m "refactor(ui): migrate forms to ui primitives"
```

---

## Phase D — Modal migration (7 files)

**Group D substitution rules** (apply to every Phase D file):

1. **Add the import** at the top of the file:
   ```tsx
   import { Modal } from "@/components/ui";
   ```

2. **Replace modal chrome only.** The `Modal` primitive owns: overlay, click-outside-to-close, panel container, header (title + close button), Escape-key handler, scroll containment, ARIA dialog attributes.

   Replace:
   ```tsx
   <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
     <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />
     <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-<size> max-h-[90vh] overflow-y-auto">
       <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 sticky top-0 bg-white z-10">
         <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
         <button onClick={onClose} className="…" aria-label="Close">
           <X className="h-5 w-5" />
         </button>
       </div>
       {/* body */}
     </div>
   </div>
   ```

   With:
   ```tsx
   <Modal open onClose={onClose} title={title} size="<sizeProp>">
     {/* body — unchanged */}
   </Modal>
   ```

   The body — form fields, submit/cancel button rows, validation, etc. — passes through `children` and is **not modified by Phase D**, even if its inline `<input>`/`<select>`/`<button>` markup matches Phase C patterns. Inline-form migration inside modals is a follow-up effort.

3. **Size mapping**:

   | Existing panel `max-w-…` | `Modal` `size` |
   |---|---|
   | `max-w-sm` | `"sm"` |
   | `max-w-md` (or no width) | `"md"` |
   | `max-w-lg` | `"lg"` |
   | `max-w-xl` or wider | `"xl"` |

4. **Cleanup after substitution**:
   - Remove the `import { X } from "lucide-react";` if `X` is no longer used (the close-button SVG comes from `Modal` primitive). If `X` is used elsewhere in the file, keep the import.
   - Remove the local `useEffect` that registers the Escape-key listener — the `Modal` primitive owns that.
   - Remove the local `onClose`-on-overlay-click logic — `Modal` owns it.
   - The modal component's `open` state lives outside (in the parent). Phase D files render the modal unconditionally as `<Modal open ...>` because in the existing code each file is rendered conditionally by the parent (`{showModal && <Modal …>}`). The `Modal` primitive's `open` prop must be `true` when the component is rendered. If the file conditionally returns `null` (e.g. `if (!isOpen) return null;`), keep that early return and pass `open` as the matching boolean.

5. **`"use client"` directive** stays. The `Modal` primitive itself is `"use client"`, so the consumer must be too.

After applying these rules, run the verification gate.

### Task 36: Migrate `CreateAdminModal.tsx`

**Files:**
- Modify: `web/src/components/admins/CreateAdminModal.tsx`

- [ ] **Step 1: Apply Phase D substitution rules. Specifically:**

  - Add `import { Modal } from "@/components/ui";`.
  - Remove `import { X } from "lucide-react";`.
  - Remove the `useEffect` that registers the `Escape` key listener (lines 50–54 in current file).
  - Replace the entire return JSX (lines 81–204) with:
    ```tsx
    return (
      <Modal open onClose={onClose} title={t("formTitle")} size="md">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* body content from line 94 onwards (error banner, fields, footer) — unchanged */}
        </form>
      </Modal>
    );
    ```
    The form `<form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">` becomes `<form onSubmit={handleSubmit} className="space-y-4">` — drop `px-6 py-5` since the Modal primitive's panel already has `px-7 py-6` padding.
  - Inline form fields (`<input>`/`<select>`/cancel/submit `<button>`) inside the body **stay as-is** — Phase D does not migrate them.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output. If TS complains about unused `useEffect` import, remove it from the import statement (keep `useState`).

---

### Task 37: Migrate `EditAdminModal.tsx`

**Files:**
- Modify: `web/src/components/admins/EditAdminModal.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/admins/EditAdminModal.tsx
```

- [ ] **Step 2: Apply Phase D substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 38: Migrate `CreateManagerModal.tsx`

**Files:**
- Modify: `web/src/components/managers/CreateManagerModal.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/managers/CreateManagerModal.tsx
```

- [ ] **Step 2: Apply Phase D substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 39: Migrate `EditManagerModal.tsx`

**Files:**
- Modify: `web/src/components/managers/EditManagerModal.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/managers/EditManagerModal.tsx
```

- [ ] **Step 2: Apply Phase D substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 40: Migrate `CreateProfessorModal.tsx`

**Files:**
- Modify: `web/src/components/professors/CreateProfessorModal.tsx`

- [ ] **Step 1: Apply Phase D substitution rules. Specifically:**

  - Add `import { Modal } from "@/components/ui";`.
  - Remove `import { X } from "lucide-react";`.
  - Remove the `useEffect` Escape-key listener (lines 56–60).
  - Replace the entire return JSX (lines 105–208) with:
    ```tsx
    return (
      <Modal open onClose={onClose} title={t("formCreateTitle")} size="md">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* body content from line 118 onwards — unchanged */}
        </form>
      </Modal>
    );
    ```
    Drop `px-6 py-5` from the form's className.
  - Inline form fields stay as-is.

- [ ] **Step 2: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 41: Migrate `EditProfessorModal.tsx`

**Files:**
- Modify: `web/src/components/professors/EditProfessorModal.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/professors/EditProfessorModal.tsx
```

- [ ] **Step 2: Apply Phase D substitution rules above.**

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 42: Migrate `ProofReviewModal.tsx`

**Files:**
- Modify: `web/src/components/payment-proofs/ProofReviewModal.tsx`

- [ ] **Step 1: Read the current file**

```bash
cat /Users/gonzalodevarona/Documents/klasio/web/src/components/payment-proofs/ProofReviewModal.tsx
```

- [ ] **Step 2: Apply Phase D substitution rules above.**

This modal is wider than other modals (proof image preview). Map its `max-w-…` value to `size="lg"` or `size="xl"` per the size table.

- [ ] **Step 3: Run `npx tsc --noEmit` from `web/`**

Expected: zero output.

---

### Task 43: Commit Phase D + final build + dev-server spot-check

- [ ] **Step 1: Stage all 7 modified modal files and commit**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git add \
  web/src/components/admins/CreateAdminModal.tsx \
  web/src/components/admins/EditAdminModal.tsx \
  web/src/components/managers/CreateManagerModal.tsx \
  web/src/components/managers/EditManagerModal.tsx \
  web/src/components/professors/CreateProfessorModal.tsx \
  web/src/components/professors/EditProfessorModal.tsx \
  web/src/components/payment-proofs/ProofReviewModal.tsx
git commit -m "refactor(ui): migrate modals to Modal primitive"
```

- [ ] **Step 2: Run final production build**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm run build
```

Expected: `Compiled successfully`. Zero type errors. Zero ESLint errors (the project's existing lint rules apply).

- [ ] **Step 3: Verify zero `app/**` files were touched**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git diff --name-only HEAD~5 HEAD -- web/src/app/
```

Expected: zero output. If anything appears, revert it before merging.

- [ ] **Step 4: Verify all 5 commits are present in order**

```bash
git log --oneline HEAD~5..HEAD
```

Expected (top to bottom = newest to oldest):
```
<sha> refactor(ui): migrate modals to Modal primitive
<sha> refactor(ui): migrate forms to ui primitives
<sha> refactor(ui): migrate list components to ui primitives
<sha> refactor(ui): migrate badge components to Badge primitive
<sha> feat(ui): extend Badge primitive with info variant and icon slot
```

- [ ] **Step 5: Manual dev-server spot-check**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm run dev
```

Open `http://localhost:3000` in a browser and visually verify each of the following pages renders without broken layouts, missing styles, or visible errors:

- `/students` — list with mixed ACTIVE/INACTIVE statuses (Phase A badges + Phase B table)
- `/programs` — list (Phase B table)
- `/classes` — list with mixed levels (Phase A level badges + Phase B table)
- `/memberships` (or `/students/<id>/memberships`) — list with mixed status badges (Phase A `MembershipStatusBadge` showing collapsed pending shades)
- `/payment-proofs` — proof queue (Phase B `ProofQueue`)
- `/login` — `LoginForm` (Phase C primitives, password show/hide still works)
- `/students/new` — `StudentForm` (Phase C primitives)
- Any list page with a "Create X" CTA — open the modal (e.g. `/admins` → "New Admin" button) — verify Modal chrome renders, Escape closes, click-outside closes, inner form still submits.

Spot-check criteria:
- No raw class names rendered as text
- No invisible buttons / inputs (would indicate missing background)
- Submit buttons render in volt-lime, not indigo
- Table headers render in DM Mono uppercase
- Badge colors match the variant table (collapsed-pending and ALERTED-with-icon will look different from legacy — that's expected)

Time budget for spot-check: ~10 minutes. Record findings as a comment in the eventual PR description.

- [ ] **Step 6: Stop dev server**

`Ctrl+C` in the dev-server terminal.

---

## Self-Review Checklist (post-execution)

Once all 43 tasks are complete:

- [ ] All 5 commits land on `feature/full-redesign` in correct order.
- [ ] `npm run build` exited 0 in Task 43 Step 2.
- [ ] `git diff --name-only HEAD~5 HEAD -- web/src/app/` returned no output.
- [ ] Manual spot-check completed; findings recorded.
- [ ] No public component name, default-vs-named export, or props interface changed in any of the 38 files (verifiable by grepping consumer call sites — none should have type errors after the run).
- [ ] All 12 badge wrappers retain `useTranslations` (except `TenantStatusBadge` which never had it).
- [ ] `Badge.test.tsx` reports 14 passing tests (3 new tests added in Step 0).

If any item fails, return to the failing phase and remediate before merging.
