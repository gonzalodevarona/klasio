# UI Primitives — Klasio Web Design System (Step 2)

**Date:** 2026-04-24
**Scope:** New `src/components/ui/` directory with 10 reusable primitives. Zero changes to existing components.
**Depends on:** `2026-04-24-design-system-migration-design.md` (Step 1 — tokens already in `tailwind.config.ts`).

---

## Goal

Introduce a set of headless-style presentational primitives that every future feature component imports, replacing ad-hoc inline styles scattered across the codebase. Primitives encode the visual language of the design system (color tokens, radii, shadows, typography) and expose a minimal, consistent prop API. They are **text-agnostic**: no user-facing strings are hardcoded inside primitives — callers pass translated labels via `next-intl` at the feature-layer boundary.

---

## Non-Goals

- Refactoring existing components to consume the primitives (deferred to Step 3).
- Domain-specific wrappers (e.g. `MembershipStatusBadge`) — those already exist and keep their current location under feature directories.
- A full Storybook or visual regression pipeline — tests are behavioral (RTL) only in Step 2.

---

## Files Created

| File | Responsibility |
|---|---|
| `web/src/lib/utils.ts` | `cn()` helper wrapping `clsx` + `tailwind-merge` |
| `web/src/components/ui/Badge.tsx` | Status/tag pill |
| `web/src/components/ui/Button.tsx` | Button with 5 variants, 3 sizes, Radix Slot `asChild` |
| `web/src/components/ui/Input.tsx` | Text input with label/error/hint/leftIcon |
| `web/src/components/ui/Select.tsx` | `<select>` sibling of Input |
| `web/src/components/ui/Card.tsx` | Surface container (light/dark) |
| `web/src/components/ui/StatCard.tsx` | Metric display card |
| `web/src/components/ui/Table.tsx` | Compound `Table`/`Thead`/`Th`/`Tr`/`Td` |
| `web/src/components/ui/Pagination.tsx` | Page navigation controls |
| `web/src/components/ui/Modal.tsx` | Overlay dialog |
| `web/src/components/ui/HoursBar.tsx` | Membership hour-remaining meter |
| `web/src/components/ui/index.ts` | Barrel export |

All components have a co-located `<Component>.test.tsx`. No existing files are modified except `package.json` (new deps).

---

## Dependencies Added

```
@radix-ui/react-slot   ^1.x   — Button asChild polymorphism
clsx                   ^2.x   — conditional class composition
tailwind-merge         ^2.x   — dedupe conflicting Tailwind classes in cn()
```

No other deps change. Tests use the existing Jest + `@testing-library/react` + `jest-dom` stack.

---

## Design Decisions (resolved during brainstorming)

| # | Decision | Rationale |
|---|---|---|
| 1 | **Primitives are text-agnostic.** No default labels, no internal `useTranslations` calls. Callers pass translated strings via props. | Matches existing pattern (`MembershipStatusBadge` calls `t()` then passes `label` — in the new world it would pass to primitive `<Badge>`). Primitive stays dumb; i18n lives at feature layer. Respects feedback memory: UI strings must be English (or locale-resolved via next-intl), never hardcoded Spanish. |
| 2 | **Button `asChild` via `@radix-ui/react-slot`.** | Industry-standard pattern (shadcn/ui). Tiny dep. Reusable for future Dialog/Dropdown primitives. |
| 3 | **Modal uses `size` variants, not a numeric `width` prop.** Sizes: `sm` (400px), `md` (480px default), `lg` (600px), `xl` (800px). Unusual widths go through `className` escape hatch. | Spec's `width: number` prop contradicted "no inline styles" constraint (dynamic `w-[${n}px]` breaks Tailwind JIT). Variants + literal `className` keep everything JIT-visible. |
| 4 | **HoursBar fill = `(total - used) / total`.** 100%→k-volt, 66%→#8AE800, 33%→#FFC107, else k-border. | Color progression only makes sense on remaining hours (green = healthy, grey = depleted). Matches existing `HourBalance` mental model. `≥100%` naturally reachable when an admin adds bonus hours. |
| 5 | **Badge variant list = 9** (`active`, `expiring`, `inactive`, `pending`, `approved`, `rejected`, `beginner`, `intermediate`, `advanced`). Dropped `premium`, `new`, `single`, `recurring` (no domain mapping; YAGNI). | Keep only variants that map to current domain: membership status (3), payment proof status (3), student level (3). |
| 6 | **Pagination is 0-indexed** and **has no default text**. Caller passes `labelPrev`, `labelNext`, `labelFormat(page, totalPages, total) => string`. | 0-indexed matches existing `TenantList.tsx:130`. No defaults = zero locale leakage. |
| 7 | **Button `asChild` + `disabled` combo: not implemented.** `disabled` is silently ignored when `asChild={true}`. | Anchor elements have no native `disabled` attribute; caller should gate rendering instead. Out of scope for Step 2. |
| 8 | **Card border uses `border-[1.5px]`.** Tailwind has `border` (1px) and `border-2` (2px); 1.5px requires arbitrary value. | Spec called for `border-1.5`; Tailwind has no standard utility between 1px and 2px. |
| 9 | **Input `leftIcon` adds `pl-9` to input and absolute-positions the icon at `left-3`.** Input wrapper becomes `relative`. | Standard icon-input pattern. Offset accounts for icon width + gap. |
| 10 | **StatCard `subColor` is a Tailwind class string** (e.g. `"text-k-danger-text"`), not a raw CSS color. Applied via `cn()`. | Keeps "no inline styles" constraint. Caller stays in the token vocabulary. |
| 11 | **Button adds a 3rd `icon` size** (`h-8 w-8 p-0`, 32×32 square). | Required by Modal close button and will recur in future toolbars/row actions. |
| 12 | **Button `danger` variant adds `border` (1px).** | Spec specified `border k-danger-text/30` colour without width; 1px is the sane default and `/30` opacity modifier requires an active border class. |
| 13 | **Button `ghost` variant keeps `bg-k-bg`** (not transparent). | Spec explicit. Naming is nonstandard but intentional — it's a low-emphasis button, not a completely transparent one. |
| 14 | **`'use client'` directive only on `Modal`** (Escape-key listener via `useEffect`). All other primitives are pure presentational and inherit the caller's client boundary. | Minimize client-boundary surface area; keep RSC compatibility where possible. |

---

## Component APIs

### `Badge`

```ts
type BadgeVariant =
  | 'active' | 'expiring' | 'inactive'
  | 'pending' | 'approved' | 'rejected'
  | 'beginner' | 'intermediate' | 'advanced';

interface BadgeProps {
  variant: BadgeVariant;
  label: string;           // required — caller supplies translated text
  small?: boolean;         // text-[10px] px-2 py-px instead of text-[11px] px-2.5 py-0.5
  className?: string;
}
```

**Variant → classes:**

| variant | bg | text | border |
|---|---|---|---|
| `active` | `bg-k-volt` | `text-k-volt-text` | — |
| `expiring` | `bg-k-warn-bg` | `text-k-warn-text` | — |
| `inactive` | `bg-k-bg` | `text-k-subtle` | `border border-k-border` |
| `pending` | `bg-k-warn-bg` | `text-k-warn-text` | — |
| `approved` | `bg-k-volt` | `text-k-volt-text` | — |
| `rejected` | `bg-k-danger-bg` | `text-k-danger-text` | — |
| `beginner` | `bg-k-info-bg` | `text-k-info-text` | — |
| `intermediate` | `bg-k-warn-bg` | `text-k-warn-text` | — |
| `advanced` | `bg-k-volt` | `text-k-volt-text` | — |

Base: `rounded-full font-semibold inline-flex items-center`. Default size: `text-[11px] px-2.5 py-0.5`. Small: `text-[10px] px-2 py-px`.

### `Button`

```ts
type ButtonVariant = 'primary' | 'volt' | 'outline' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'icon';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant: ButtonVariant;
  size?: ButtonSize;       // 'md' default
  asChild?: boolean;       // wrap single child via @radix-ui/react-slot
}
```

**Variant → classes:**

| variant | classes |
|---|---|
| `primary` | `bg-k-dark text-white hover:bg-[#1A1A1A]` |
| `volt` | `bg-k-volt text-k-dark hover:bg-[#B8EE3A]` |
| `outline` | `bg-transparent text-k-dark border border-k-border hover:bg-k-surface` |
| `ghost` | `bg-k-bg text-k-subtle hover:bg-k-border` |
| `danger` | `bg-k-danger-bg text-k-danger-text border border-k-danger-text/30` |

**Size → classes:**

| size | classes |
|---|---|
| `sm` | `text-xs px-3 py-1.5 rounded-k-sm` |
| `md` | `text-sm font-semibold px-4 py-2 rounded-k-sm` |
| `icon` | `h-8 w-8 p-0 rounded-k-sm inline-flex items-center justify-center` |

Base: `inline-flex items-center justify-center transition duration-k disabled:opacity-40 disabled:cursor-not-allowed`.

When `asChild={true}`, renders a Radix `<Slot>` that clones the single child and merges className + props. `disabled` is silently ignored in `asChild` mode (decision 7).

### `Input`

```ts
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
}
```

Structure:
```
<div>
  {label && <label class="text-xs font-medium text-k-subtle mb-1 font-[var(--font-mono)] uppercase tracking-wide">{label}</label>}
  <div class="relative">
    {leftIcon && <span class="absolute left-3 top-1/2 -translate-y-1/2">{leftIcon}</span>}
    <input class="bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm font-[var(--font-main)] w-full focus:border-k-volt focus:outline-none {error && 'border-k-danger-text'} {leftIcon && 'pl-9'}" />
  </div>
  {error
    ? <p class="text-xs text-k-danger-text mt-1">{error}</p>
    : hint && <p class="text-xs text-k-muted mt-1">{hint}</p>}
</div>
```

Forwards ref to the underlying `<input>` via `React.forwardRef`.

### `Select`

Same shape as `Input` but the control element is `<select>` with `children` (option elements). Same label/error/hint. No `leftIcon` support (selects have their own caret). Forwards ref to the underlying `<select>`.

### `Card`

```ts
interface CardProps {
  padding?: 'sm' | 'md' | 'lg';   // 'md' default
  dark?: boolean;                  // swap to dark surface
  className?: string;
  children: React.ReactNode;
}
```

Classes: base `rounded-k-lg`. Light: `bg-k-surface border-[1.5px] border-k-border`. Dark: `bg-k-dark`. Padding: `p-4` / `p-6` / `p-8`.

### `StatCard`

```ts
interface StatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  subColor?: string;    // Tailwind class e.g. "text-k-volt"
  dark?: boolean;
}
```

Composes `Card` with `padding="md"` + `dark` prop. Interior: `flex flex-col gap-1.5`.
- Label: `font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em]` — `text-k-muted` light / `text-[#666]` dark.
- Value: `text-[40px] font-extrabold tracking-[-0.03em] leading-none` — `text-k-dark` light / `text-k-volt` dark.
- Sub: `text-xs font-medium` — default `text-k-muted` light / `text-k-volt` dark; overridable via `subColor` prop.

### `Table` (compound)

Exports: `Table`, `Thead`, `Th`, `Tr`, `Td`.

| export | props | classes |
|---|---|---|
| `Table` | `className?, children` | `overflow-x-auto rounded-k-md border border-k-border w-full` (as `<div><table class="w-full">`) |
| `Thead` | `children` | `bg-k-bg border-b border-k-border` |
| `Th` | `right?: boolean, children, className?` | `font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted px-4 py-2.5` + `text-right` when `right` |
| `Tr` | `onClick?, active?: boolean, children, className?` | `border-b border-k-line` + `hover:bg-k-surface cursor-pointer` when `onClick` + `bg-[#F9FFEA]` when `active` |
| `Td` | `mono?, muted?, bold?, right?, children, className?` | `px-4 py-3 text-sm whitespace-nowrap` + `font-[var(--font-mono)]` / `text-k-muted` / `font-semibold` / `text-right` per flag |

### `Pagination`

```ts
interface PaginationProps {
  page: number;          // 0-indexed
  totalPages: number;
  total: number;
  onPrev: () => void;
  onNext: () => void;
  labelPrev: string;     // required — caller translates
  labelNext: string;     // required
  labelFormat: (page: number, totalPages: number, total: number) => string;   // required
}
```

Layout: `flex justify-between items-center pt-4`. Left slot: `<span class="font-[var(--font-mono)] text-xs text-k-muted">{labelFormat(page, totalPages, total)}</span>`. Right slot: two `<Button variant="outline" size="sm">` — `onPrev` disabled when `page === 0`, `onNext` disabled when `page >= totalPages - 1`.

### `Modal`

```ts
type ModalSize = 'sm' | 'md' | 'lg' | 'xl';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;           // caller passes translated
  size?: ModalSize;        // 'md' default
  className?: string;      // escape hatch for unusual widths
  children: React.ReactNode;
}
```

Returns `null` when `open === false`. When open: renders overlay `fixed inset-0 z-50 bg-k-dark/55 backdrop-blur-sm flex items-center justify-center`. Clicking overlay calls `onClose`. Panel: `bg-k-surface rounded-k-xl shadow-k-modal max-h-[85vh] overflow-y-auto w-full`, size class applied (`max-w-[400px]` / `max-w-[480px]` / `max-w-[600px]` / `max-w-[800px]`), plus `className` appended.

Header: `flex justify-between items-center px-7 py-6 border-b border-k-border`, title `text-base font-bold`, close is `<Button variant="ghost" size="icon">` with X icon (inline SVG).
Body: `px-7 py-6`.

**Behavior:** Escape key listener (via `useEffect` on `window`) calls `onClose`. This is the only `'use client'` primitive.

### `HoursBar`

```ts
interface HoursBarProps {
  used: number;
  total: number;
}
```

Compute `remaining = max(total - used, 0)`, `pct = total > 0 ? (remaining / total) * 100 : 0`.

Fill color:
- `pct >= 100` → `bg-k-volt`
- `pct >= 66`  → `bg-[#8AE800]`
- `pct >= 33`  → `bg-[#FFC107]`
- else         → `bg-k-border`

Layout: `flex items-center gap-2`. Bar outer: `w-20 h-1 bg-k-line rounded-full overflow-hidden`. Fill: `h-full {colorClass}` with inline style `width: ${min(pct, 100)}%` (only style exception — width must be dynamic). Label: `font-[var(--font-mono)] text-[11px] text-k-subtle`, text `{used}/{total}h`.

> Note: the fill `width` is the single authorized inline-style exception in the primitive set — a dynamic numeric percentage cannot be expressed as a static Tailwind class.

### `cn()` utility

```ts
// web/src/lib/utils.ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
```

### Barrel — `index.ts`

```ts
export { Badge } from './Badge';
export { Button } from './Button';
export { Input } from './Input';
export { Select } from './Select';
export { Card } from './Card';
export { StatCard } from './StatCard';
export { Table, Thead, Th, Tr, Td } from './Table';
export { Pagination } from './Pagination';
export { Modal } from './Modal';
export { HoursBar } from './HoursBar';
```

Types are exported alongside components.

---

## Testing Strategy (TDD)

Per project convention (CLAUDE.md: TDD non-negotiable), tests are written before implementation. One `.test.tsx` per component, co-located. Coverage focuses on prop-to-class mapping, a11y, and interactions — not snapshots.

| Component | Test cases (minimum) |
|---|---|
| `cn` | merges classes; dedupes conflicting Tailwind utilities |
| `Badge` | each of 9 variants renders expected bg/text classes; `small` prop applies small classes; `label` rendered as text |
| `Button` | each of 5 variants renders expected classes; `sm`/`md`/`icon` sizes; `asChild` renders child element (not `<button>`) with merged className; `disabled` sets `disabled` attr + opacity class; `onClick` fires; `type` forwarded |
| `Input` | base classes; `error` state border class; `label`/`hint`/`error` render; `leftIcon` adds `pl-9` and icon is positioned; forwards ref |
| `Select` | analogous to Input (with `<select>`); option children render |
| `Card` | default vs `dark`; each `padding` variant; `className` merges |
| `StatCard` | label/value/sub render; `dark` swaps color classes; `subColor` prop overrides default sub class |
| `Table` | wrapper classes; `Th right` adds `text-right`; `Tr onClick` adds cursor + fires; `Tr active` bg class; each `Td` modifier flag maps correctly |
| `Pagination` | `labelFormat` result shown; `labelPrev`/`labelNext` render on buttons; prev disabled at `page=0`; next disabled at `page=totalPages-1`; `onPrev`/`onNext` fire |
| `Modal` | `open=false` → nothing in DOM; `open=true` → panel + overlay render; Escape key calls `onClose`; overlay click calls `onClose`; each `size` applies correct `max-w-*` class; `className` appended to panel |
| `HoursBar` | fill width = `(total-used)/total * 100` clamped 0–100; color per threshold; label text exact |

Target: ~80 tests across 11 files. Stack: existing Jest + RTL + jest-dom. No new test deps.

---

## Verification

After all commits:

1. `cd web && npx tsc --noEmit` → zero errors
2. `cd web && npm test` → all green, ~80 new tests pass
3. `cd web && npm run build` → Next.js build succeeds
4. `cd web && npm run lint` → no new warnings

Per-commit gate: `npx tsc --noEmit` + `npm test -- <component>` must pass before the commit.

---

## Commit Plan

Branch: current `feature/full-redesign`. Conventional Commits. No Co-Authored-By footer.

| # | Commit |
|---|---|
| 1 | `chore(design-system): install clsx tailwind-merge radix-slot and add cn utility` |
| 2 | `feat(ui): add Badge primitive` |
| 3 | `feat(ui): add Button primitive with asChild support` |
| 4 | `feat(ui): add Input and Select primitives` |
| 5 | `feat(ui): add Card and StatCard primitives` |
| 6 | `feat(ui): add Table compound primitive` |
| 7 | `feat(ui): add Pagination primitive` |
| 8 | `feat(ui): add Modal primitive` |
| 9 | `feat(ui): add HoursBar primitive` |
| 10 | `chore(ui): add ui primitives barrel export` |

Each commit: write tests (red) → implement (green) → verify gates → commit.

---

## Out of Scope (Step 3+)

- Migrating existing components in `src/components/{memberships,students,programs,...}` to consume the primitives.
- Replacing domain-specific badges/buttons with wrappers over the new primitives.
- Additional primitives that don't appear in this spec (e.g. Dropdown, Tooltip, Toast) — add when first consumer needs them.
- Focus-trap inside `Modal` (current scope: Escape + overlay click are sufficient; trap can be added when an accessibility audit flags it).
- Storybook or visual regression tests.
