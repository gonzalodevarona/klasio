# Sidebar Header Restyle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat sidebar header brand block with a 3-row vertical layout (KLogo / tenant name / role pill) — pure CSS/style changes, zero logic changes.

**Architecture:** Add two Tailwind tokens, add i18n role label keys, resize TenantBrand, then update the `Brand` component in Sidebar.tsx and the mobile drawer header to use the new 3-row layout.

**Tech Stack:** Next.js 15, TypeScript 5.9, Tailwind CSS 3.4, next-intl, React Testing Library / Jest 29

---

## File Map

| File | Change |
|---|---|
| `web/tailwind.config.ts` | Add `k-volt-muted` color + `k-mono` fontFamily |
| `web/messages/en.json` | Add `layout.roleLabel.*` keys |
| `web/messages/es.json` | Add `layout.roleLabel.*` keys |
| `web/src/components/layout/TenantBrand.tsx` | Resize skeleton + img + text |
| `web/src/components/layout/__tests__/TenantBrand.test.tsx` | Assert new skeleton classes |
| `web/src/components/layout/Sidebar.tsx` | Replace Brand JSX + mobile drawer header |

---

### Task 1: Add Tailwind design tokens

**Files:**
- Modify: `web/tailwind.config.ts`

- [ ] **Step 1: Add `k-volt-muted` color token**

In `web/tailwind.config.ts`, inside `theme.extend.colors`, add after the `"k-volt-hover"` line:

```ts
"k-volt-muted": "rgba(202,255,77,0.1)",
```

Result (colors block, abbreviated):
```ts
colors: {
  // ... existing tokens ...
  "k-volt": "#CAFF4D",
  "k-volt-hover": "#B8EE3A",
  "k-volt-muted": "rgba(202,255,77,0.1)",   // ← new
  // ...
},
```

- [ ] **Step 2: Add `k-mono` fontFamily token**

In `web/tailwind.config.ts`, inside `theme.extend`, add a new `fontFamily` block after the `colors` block:

```ts
fontFamily: {
  "k-mono": ["var(--font-mono)"],
},
```

Full `theme.extend` after both additions:
```ts
theme: {
  extend: {
    colors: {
      "k-bg": "#F4F4F2",
      "k-surface": "#FAFAF8",
      "k-dark": "#0A0A0A",
      "k-ink": "#2A2A28",
      "k-muted": "#9A9A98",
      "k-subtle": "#4A4A48",
      "k-border": "#DDDDD8",
      "k-line": "#EBEBEA",
      "k-volt": "#CAFF4D",
      "k-volt-text": "#2A4A00",
      "k-warn-bg": "#FFF0C2",
      "k-warn-text": "#8A5A00",
      "k-info-bg": "#E8F4FF",
      "k-info-text": "#0066BB",
      "k-danger-bg": "#FFE8E8",
      "k-danger-text": "#CC2200",
      "k-sidebar-active": "#1A1A1A",
      "k-volt-hover": "#B8EE3A",
      "k-volt-muted": "rgba(202,255,77,0.1)",
    },
    fontFamily: {
      "k-mono": ["var(--font-mono)"],
    },
    borderRadius: {
      "k-sm": "8px",
      "k-md": "12px",
      "k-lg": "16px",
      "k-xl": "20px",
    },
    boxShadow: {
      "k-card": "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)",
      "k-modal": "0 24px 80px rgba(0,0,0,0.25)",
      "k-dropdown": "0 8px 24px rgba(0,0,0,0.12)",
    },
    transitionDuration: {
      k: "150ms",
    },
  },
},
```

- [ ] **Step 3: Verify TypeScript compiles**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 4: Commit**

```bash
git add web/tailwind.config.ts
git commit -m "style(sidebar): add k-volt-muted and k-mono tailwind tokens"
```

---

### Task 2: Add i18n role label keys

**Files:**
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Add role labels to `en.json`**

In `web/messages/en.json`, find the `"layout"` namespace and locate the last key:
```json
"navAttendance": "Attendance"
```
Add a comma after it and insert:
```json
"navAttendance": "Attendance",
"roleLabel": {
  "SUPERADMIN": "Superadmin",
  "ADMIN": "Admin",
  "MANAGER": "Manager",
  "PROFESSOR": "Professor",
  "STUDENT": "Student"
}
```

- [ ] **Step 2: Add role labels to `es.json`**

In `web/messages/es.json`, find the `"layout"` namespace and locate:
```json
"navAttendance": "Asistencia"
```
Add a comma and insert:
```json
"navAttendance": "Asistencia",
"roleLabel": {
  "SUPERADMIN": "Superadmin",
  "ADMIN": "Admin",
  "MANAGER": "Manager",
  "PROFESSOR": "Profesor",
  "STUDENT": "Estudiante"
}
```

- [ ] **Step 3: Verify TypeScript compiles**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 4: Commit**

```bash
git add web/messages/en.json web/messages/es.json
git commit -m "feat(sidebar): add i18n role label keys to layout namespace"
```

---

### Task 3: Resize TenantBrand

**Files:**
- Modify: `web/src/components/layout/TenantBrand.tsx`
- Modify: `web/src/components/layout/__tests__/TenantBrand.test.tsx`

- [ ] **Step 1: Update the skeleton test to assert new sizing**

In `web/src/components/layout/__tests__/TenantBrand.test.tsx`, replace the first test:

```tsx
it("renders skeleton when loading", () => {
  const { container } = render(
    <TenantBrand tenantName={null} tenantLogoUrl={null} loading />
  );
  const skeleton = container.querySelector(".animate-pulse");
  expect(skeleton).not.toBeNull();
  expect(skeleton).toHaveClass("h-4", "w-28");
  expect(screen.queryByRole("img")).toBeNull();
});
```

- [ ] **Step 2: Run test to verify it fails (skeleton still has old classes)**

```bash
cd web && npx jest TenantBrand --no-coverage
```
Expected: FAIL — `expect(skeleton).toHaveClass("h-4", "w-28")` fails because skeleton currently has `h-6 w-32`.

- [ ] **Step 3: Apply sizing changes to TenantBrand.tsx**

Replace the full contents of `web/src/components/layout/TenantBrand.tsx`:

```tsx
import { cn } from "@/lib/utils";

interface TenantBrandProps {
  tenantName: string | null;
  tenantLogoUrl: string | null;
  loading: boolean;
  className?: string;
}

export default function TenantBrand({
  tenantName,
  tenantLogoUrl,
  loading,
  className,
}: TenantBrandProps) {
  if (loading) {
    return (
      <div
        className={cn("h-4 w-28 bg-k-sidebar-active rounded animate-pulse", className)}
        aria-hidden="true"
      />
    );
  }

  return (
    <div className={cn("flex items-center gap-2 select-none min-w-0", className)}>
      {tenantLogoUrl && (
        <img
          src={tenantLogoUrl}
          alt={tenantName ?? ""}
          width={18}
          height={18}
          className="shrink-0 rounded-sm object-contain"
        />
      )}
      <span className="text-[13px] font-semibold text-white leading-none truncate">
        {tenantName}
      </span>
    </div>
  );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd web && npx jest TenantBrand --no-coverage
```
Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/layout/TenantBrand.tsx \
        web/src/components/layout/__tests__/TenantBrand.test.tsx
git commit -m "style(sidebar): resize TenantBrand logo, text and skeleton"
```

---

### Task 4: Restyle Brand component and mobile drawer in Sidebar.tsx

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Add `ROLE_LABEL_KEYS` constant after imports**

In `web/src/components/layout/Sidebar.tsx`, after the import block (after line 35, before `type IconComponent`), add:

```ts
const ROLE_LABEL_KEYS = {
  SUPERADMIN: "roleLabel.SUPERADMIN",
  ADMIN:      "roleLabel.ADMIN",
  MANAGER:    "roleLabel.MANAGER",
  PROFESSOR:  "roleLabel.PROFESSOR",
  STUDENT:    "roleLabel.STUDENT",
} as const satisfies Record<Role, string>;
```

- [ ] **Step 2: Replace the Brand component**

Find and replace the entire `Brand` function (currently lines 180–216 of Sidebar.tsx). Replace it with:

```tsx
function Brand({
  role,
  tenantName,
  tenantLogoUrl,
  tenantFetchFailed,
  collapsed,
}: {
  role: Role | undefined;
  tenantName: string | null;
  tenantLogoUrl: string | null;
  tenantFetchFailed: boolean;
  collapsed: boolean;
}) {
  const t = useTranslations("layout");
  if (collapsed) return null;

  const showTenantBrand =
    role !== undefined && role !== "SUPERADMIN" && !tenantFetchFailed;
  const tenantLoading = showTenantBrand && tenantName === null;

  return (
    <div className="overflow-hidden min-w-0 flex flex-col gap-0">
      <KLogo size={28} />
      {showTenantBrand && (
        <div className="mt-3">
          <TenantBrand
            tenantName={tenantName}
            tenantLogoUrl={tenantLogoUrl}
            loading={tenantLoading}
          />
        </div>
      )}
      {role && (
        <span className="mt-2 self-start bg-k-volt-muted text-k-volt font-k-mono text-[9px] uppercase tracking-[0.12em] px-2 py-[3px] rounded-[4px]">
          {t(ROLE_LABEL_KEYS[role])}
        </span>
      )}
    </div>
  );
}
```

Note: `useTranslations` is already imported at the top of the file (`import { useTranslations } from "next-intl"`).

- [ ] **Step 3: Replace the mobile drawer header brand block**

In the mobile drawer section (`{mobileOpen && ...}`), find the existing `<div className="min-w-0 flex-1">` block inside the drawer header:

```tsx
<div className="min-w-0 flex-1">
  {tenantBrandActive ? (
    <TenantBrand
      tenantName={tenantName}
      tenantLogoUrl={tenantLogoUrl}
      loading={tenantBrandLoading}
    />
  ) : (
    <KLogo />
  )}
  <hr className="border-k-sidebar-active my-2" />
  {primaryUserRole && (
    <p className="text-[11px] text-k-subtle truncate">{primaryUserRole}</p>
  )}
</div>
```

Replace it with:

```tsx
<div className="overflow-hidden min-w-0 flex-1 flex flex-col gap-0">
  <KLogo size={28} />
  {tenantBrandActive && (
    <div className="mt-3">
      <TenantBrand
        tenantName={tenantName}
        tenantLogoUrl={tenantLogoUrl}
        loading={tenantBrandLoading}
      />
    </div>
  )}
  {primaryUserRole && (
    <span className="mt-2 self-start bg-k-volt-muted text-k-volt font-k-mono text-[9px] uppercase tracking-[0.12em] px-2 py-[3px] rounded-[4px]">
      {t(ROLE_LABEL_KEYS[primaryUserRole])}
    </span>
  )}
</div>
```

- [ ] **Step 4: Verify TypeScript — zero errors**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

If `tsc` complains about `t(ROLE_LABEL_KEYS[role])` being a non-literal key, cast the argument:
```tsx
{t(ROLE_LABEL_KEYS[role] as Parameters<typeof t>[0])}
```
Apply the same cast in the mobile drawer `{t(ROLE_LABEL_KEYS[primaryUserRole] as Parameters<typeof t>[0])}`.

- [ ] **Step 5: Run full test suite**

```bash
cd web && npx jest --no-coverage
```
Expected: all previously passing tests still pass. The three TenantBrand tests pass. No new failures.

- [ ] **Step 6: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "style(sidebar): 3-row brand header with role pill"
```
