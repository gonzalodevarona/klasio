# Layout Shell Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `Sidebar.tsx` and dashboard `layout.tsx` to the Klasio dark-sidebar / light-canvas design while preserving every existing behavior (auth gate, role nav, mobile drawer, notification bell, i18n, collapse, kbd handlers, scroll lock).

**Architecture:** Step 4 of the design system migration (steps 1–3 already merged). Pure visual + minor structural refactor. Tailwind named tokens replace hardcoded grays. New `KLogo` wordmark component centralizes the brand mark. `UserFooter` and `MobileUserFooter` are deduplicated into a single component with a `forceExpanded` prop. No backend, no routes, no new features.

**Tech Stack:** Next.js 15 App Router, React 19, TypeScript 5.9, Tailwind 3.4, next-intl. All design tokens are pre-defined in `web/tailwind.config.ts`.

**Spec:** `docs/superpowers/specs/2026-04-25-layout-shell-redesign-design.md`

**Verification model:** This is UI styling — no Jest tests added. Each task verifies via `npx tsc --noEmit` and a manual visual smoke matrix from spec §11. A failing-test-first TDD cycle does not apply to presentational changes.

---

## File Structure

| Path | Action | Responsibility |
|---|---|---|
| `web/src/components/layout/KLogo.tsx` | **CREATE** | Wordmark component, single span, `className` passthrough |
| `web/src/components/layout/Sidebar.tsx` | MODIFY | Token swap, KLogo import, active accent bar, widths, footer dedupe |
| `web/src/app/(dashboard)/layout.tsx` | MODIFY | Wrapper bg, main padding, SidebarSkeleton tokens + width |
| `web/messages/en.json` | MODIFY | Drop `layout.brandSubtitle` key |
| `web/messages/es.json` | MODIFY | Drop `layout.brandSubtitle` key |

## Task Order Rationale

1. KLogo first — it has no dependencies; later tasks import it.
2. Brand block + i18n key removal as one atomic commit — must change together (Sidebar stops calling `t("brandSubtitle")` exactly when the key disappears).
3. Mobile header (topbar + drawer) — both small, same file region.
4. Nav items — accent bar, icon colors, helper extraction.
5. Sidebar container (widths, bg, inline skeleton) — cosmetic shell.
6. Footer dedupe — last Sidebar change, smallest blast radius if reverted.
7. Dashboard layout wrapper + main.
8. Dashboard SidebarSkeleton.
9. Final verification gate.

---

## Task 1: Create `KLogo` component

**Files:**
- Create: `web/src/components/layout/KLogo.tsx`

- [ ] **Step 1.1: Create the file**

```tsx
interface KLogoProps {
  className?: string;
}

export default function KLogo({ className }: KLogoProps) {
  return (
    <span
      className={[
        "text-[18px] font-extrabold text-white tracking-[-0.03em] leading-none select-none",
        className ?? "",
      ].join(" ")}
    >
      klasio
    </span>
  );
}
```

- [ ] **Step 1.2: Type-check**

Run from `web/`:
```bash
npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 1.3: Commit**

```bash
git add web/src/components/layout/KLogo.tsx
git commit -m "feat(layout): add KLogo wordmark component"
```

---

## Task 2: Sidebar desktop brand block + drop `brandSubtitle` i18n key (atomic)

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx` (lines 168–191 `Brand` component, line 436 call site)
- Modify: `web/messages/en.json` (line 185)
- Modify: `web/messages/es.json` (line 185)

This is one commit because the `Brand` component must stop calling `t("brandSubtitle")` exactly when the key is removed from JSON.

- [ ] **Step 2.1: Add KLogo import to Sidebar.tsx**

In `web/src/components/layout/Sidebar.tsx`, add this import alongside existing imports (after line 10):

```tsx
import KLogo from "@/components/layout/KLogo";
```

- [ ] **Step 2.2: Replace the `Brand` component (lines 168–191)**

Replace the entire existing `Brand` function with:

```tsx
// Brand block shown in the sidebar header (desktop expanded only).
function Brand({
  tenantName,
  collapsed,
}: {
  tenantName: string | null;
  collapsed: boolean;
}) {
  if (collapsed) return null;
  return (
    <div className="overflow-hidden">
      <KLogo />
      {tenantName && (
        <p className="text-xs text-k-subtle whitespace-nowrap mt-0.5 truncate">
          {tenantName}
        </p>
      )}
    </div>
  );
}
```

Note: `brand` and `brandSubtitle` props are gone. KLogo handles the wordmark; subtitle is replaced by tenant name.

- [ ] **Step 2.3: Update the call site (around line 436)**

Find:

```tsx
<Brand tenantName={tenantName} collapsed={collapsed} brand={t("brand")} brandSubtitle={t("brandSubtitle")} />
```

Replace with:

```tsx
<Brand tenantName={tenantName} collapsed={collapsed} />
```

- [ ] **Step 2.4: Drop `brandSubtitle` from `web/messages/en.json`**

Find (line 185):

```json
    "brand": "Klasio",
    "brandSubtitle": "League Management",
```

Replace with:

```json
    "brand": "Klasio",
```

- [ ] **Step 2.5: Drop `brandSubtitle` from `web/messages/es.json`**

Find (line 185):

```json
    "brand": "Klasio",
    "brandSubtitle": "Gestión de Liga",
```

Replace with:

```json
    "brand": "Klasio",
```

- [ ] **Step 2.6: Verify no stale references**

Run from repo root:

```bash
grep -rn "brandSubtitle" web/src web/messages
```

Expected: zero matches. (Sidebar.tsx still has one reference at line 378 inside the mobile drawer — that's removed in Task 3. Until Task 3 runs, Step 2.6 will find that line. Note it and continue; verify zero again after Task 3.)

- [ ] **Step 2.7: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 2.8: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx web/src/components/layout/KLogo.tsx \
        web/messages/en.json web/messages/es.json
git commit -m "refactor(layout): use KLogo in sidebar brand block, drop brandSubtitle i18n key"
```

(The Brand block uses KLogo, so KLogo + Sidebar Brand changes belong in the same commit. This is the first commit that actually wires KLogo into the app.)

---

## Task 3: Sidebar mobile topbar + mobile drawer header (KLogo, tokens, drop subtitle line)

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`
  - Mobile topbar around lines 345–362
  - Mobile drawer header around lines 374–392

- [ ] **Step 3.1: Update mobile topbar (≈ lines 345–362)**

Find:

```tsx
<header className="lg:hidden fixed top-0 inset-x-0 z-40 flex items-center h-14 px-4 gap-3 bg-gray-900 border-b border-gray-700">
  <button
    onClick={() => setMobileOpen(true)}
    aria-label={t("openNav")}
    className="p-1 text-gray-300 hover:text-white rounded transition-colors"
  >
    <Menu className="h-6 w-6" />
  </button>
  <div className="min-w-0 flex-1">
    <span className="text-lg font-bold text-white">{t("brand")}</span>
    {tenantName && (
      <span className="ml-2 text-xs text-indigo-400 truncate hidden sm:inline">
        {tenantName}
      </span>
    )}
  </div>
  <NotificationBell />
</header>
```

Replace with:

```tsx
<header className="lg:hidden fixed top-0 inset-x-0 z-40 flex items-center h-14 px-4 gap-3 bg-k-dark border-b border-k-sidebar-active">
  <button
    onClick={() => setMobileOpen(true)}
    aria-label={t("openNav")}
    className="p-1 text-k-subtle hover:text-white rounded transition-colors"
  >
    <Menu className="h-6 w-6" />
  </button>
  <div className="min-w-0 flex-1">
    <KLogo />
    {tenantName && (
      <span className="ml-2 text-xs text-k-subtle truncate hidden sm:inline">
        {tenantName}
      </span>
    )}
  </div>
  <NotificationBell />
</header>
```

Changes: `bg-gray-900`→`bg-k-dark`, `border-gray-700`→`border-k-sidebar-active`, hamburger `text-gray-300`→`text-k-subtle`, brand span → `<KLogo />`, tenant `text-indigo-400`→`text-k-subtle`.

- [ ] **Step 3.2: Update mobile drawer header (≈ lines 374–392)**

Find:

```tsx
<aside className="relative flex flex-col w-64 h-full bg-gray-900 shadow-2xl">
  {/* Drawer header */}
  <div className="flex items-center justify-between px-4 py-3 shrink-0 border-b border-gray-700">
    <div className="min-w-0">
      <h1 className="text-lg font-bold text-white">{t("brand")}</h1>
      <p className="text-xs text-gray-400">{t("brandSubtitle")}</p>
      {tenantName && (
        <p className="text-xs font-medium text-indigo-400 truncate mt-0.5">
          {tenantName}
        </p>
      )}
    </div>
    <button
      onClick={() => setMobileOpen(false)}
      aria-label={t("closeNav")}
      className="p-1 text-gray-400 hover:text-white rounded transition-colors shrink-0 ml-2"
    >
      <X className="h-5 w-5" />
    </button>
  </div>
```

Replace with:

```tsx
<aside className="relative flex flex-col w-64 h-full bg-k-dark shadow-2xl">
  {/* Drawer header */}
  <div className="flex items-center justify-between px-4 py-3 shrink-0 border-b border-k-sidebar-active">
    <div className="min-w-0">
      <KLogo />
      {tenantName && (
        <p className="text-xs text-k-subtle truncate mt-0.5">
          {tenantName}
        </p>
      )}
    </div>
    <button
      onClick={() => setMobileOpen(false)}
      aria-label={t("closeNav")}
      className="p-1 text-k-subtle hover:text-white rounded transition-colors shrink-0 ml-2"
    >
      <X className="h-5 w-5" />
    </button>
  </div>
```

Changes: drawer `bg-gray-900`→`bg-k-dark`, divider `border-gray-700`→`border-k-sidebar-active`, brand `<h1>` + subtitle `<p>` → `<KLogo />` (subtitle line dropped), tenant `text-indigo-400 font-medium`→`text-k-subtle`, close button `text-gray-400`→`text-k-subtle`.

- [ ] **Step 3.3: Verify no remaining `brandSubtitle` references**

```bash
grep -rn "brandSubtitle" web/src web/messages
```

Expected: zero matches.

- [ ] **Step 3.4: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 3.5: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "refactor(layout): migrate sidebar mobile header to design tokens"
```

---

## Task 4: Sidebar nav items — helper, active accent bar, icon colors, badge

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`
  - `NotificationBadge` (lines 43–50)
  - `NavLinks` (lines 111–165)

- [ ] **Step 4.1: Update `NotificationBadge` (lines 43–50)**

Find:

```tsx
function NotificationBadge({ count, badgeMax }: { count: number; badgeMax: string }) {
  const label = count > 10 ? badgeMax : String(count);
  return (
    <span className="ml-auto flex items-center justify-center min-w-[1.25rem] h-5 px-1 rounded-full bg-red-500 text-white text-[10px] font-bold leading-none shrink-0">
      {label}
    </span>
  );
}
```

Replace with:

```tsx
function NotificationBadge({ count, badgeMax }: { count: number; badgeMax: string }) {
  const label = count > 10 ? badgeMax : String(count);
  return (
    <span className="ml-auto flex items-center justify-center min-w-[1.25rem] h-5 px-1 rounded-full bg-k-danger-text text-white text-[10px] font-bold leading-none shrink-0">
      {label}
    </span>
  );
}
```

Change: `bg-red-500`→`bg-k-danger-text`.

- [ ] **Step 4.2: Add the `navItemClasses` helper above `NavLinks`**

Above the `NavLinks` declaration (≈ line 111), add:

```tsx
function navItemClasses(active: boolean, collapsed: boolean) {
  return [
    "relative flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
    active
      ? "bg-k-sidebar-active text-white"
      : "text-k-muted hover:bg-k-sidebar-active hover:text-white",
    collapsed ? "justify-center" : "",
  ].join(" ");
}
```

- [ ] **Step 4.3: Replace `NavLinks` body to use the helper, accent bar, and token-colored icon**

Find the `NavLinks` function (lines 111–165). Replace its body's `return (...)` with:

```tsx
return (
  <ul className="space-y-1">
    {items.map(({ label, href, icon: Icon }) => {
      const isActive =
        pathname === href || pathname.startsWith(href + "/");
      const showBadge =
        href === "/payment-proofs" &&
        !collapsed &&
        pendingProofsCount != null &&
        pendingProofsCount > 0;
      return (
        <li key={href} className="relative">
          <Link
            href={href}
            onClick={onLinkClick}
            title={collapsed ? label : undefined}
            className={navItemClasses(isActive, collapsed)}
          >
            {isActive && (
              <span
                aria-hidden="true"
                className="absolute left-0 top-[20%] bottom-[20%] w-[3px] bg-k-volt rounded-r-full"
              />
            )}
            <div className="relative shrink-0">
              <Icon className={`h-5 w-5 ${isActive ? "text-k-volt" : "text-k-subtle"}`} />
              {/* Collapsed mode: dot indicator on the icon itself */}
              {collapsed && pendingProofsCount != null && pendingProofsCount > 0 && href === "/payment-proofs" && (
                <span className="absolute -top-1 -right-1 flex h-2 w-2 rounded-full bg-k-danger-text" />
              )}
            </div>
            {!collapsed && <span className="truncate">{label}</span>}
            {showBadge && <NotificationBadge count={pendingProofsCount!} badgeMax={badgeMax} />}
          </Link>
        </li>
      );
    })}
  </ul>
);
```

Changes:
- `<li>` is now `relative` (so the accent bar can anchor to the row)
- `Link` className uses `navItemClasses(isActive, collapsed)` instead of inline ternary
- New 3px lime accent bar `<span>` rendered when active
- Icon coloring: `text-k-volt` if active, `text-k-subtle` otherwise
- Collapsed dot indicator: `bg-red-500`→`bg-k-danger-text`

- [ ] **Step 4.4: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 4.5: Manual visual smoke (optional checkpoint)**

```bash
cd web && npm run dev
```

Open `http://localhost:3000/students` (logged in as any role). Verify:
- Active route has a lime 3px accent bar on its left edge
- Active icon is lime; inactive icons are subtle gray (#4A4A48)
- Hovering an inactive item shows `bg-k-sidebar-active` (#1A1A1A) and white text
- Pending payment proofs badge (if visible) is dark red (#CC2200), not bright red

Stop the dev server when done.

- [ ] **Step 4.6: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "refactor(layout): migrate sidebar nav items to design tokens with active accent bar"
```

---

## Task 5: Sidebar widths, container colors, inline auth-loading skeleton

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`
  - Inline auth-loading skeleton (lines 325–337)
  - Desktop sidebar `<aside>` container (around line 422–451)

- [ ] **Step 5.1: Update inline auth-loading skeleton (≈ lines 325–337)**

Find:

```tsx
if (loading) {
  return (
    <>
      <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-gray-900 border-b border-gray-700 animate-pulse" />
      <aside className="hidden lg:flex w-64 bg-gray-900 h-screen sticky top-0 flex-col shrink-0">
        <div className="p-6 space-y-2">
          <div className="h-6 w-24 bg-gray-700 rounded animate-pulse" />
          <div className="h-4 w-32 bg-gray-700 rounded animate-pulse" />
        </div>
      </aside>
    </>
  );
}
```

Replace with:

```tsx
if (loading) {
  return (
    <>
      <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-k-dark border-b border-k-sidebar-active animate-pulse" />
      <aside className="hidden lg:flex w-[220px] bg-k-dark h-screen sticky top-0 flex-col shrink-0">
        <div className="p-6 space-y-2">
          <div className="h-6 w-24 bg-k-sidebar-active rounded animate-pulse" />
          <div className="h-4 w-32 bg-k-sidebar-active rounded animate-pulse" />
        </div>
      </aside>
    </>
  );
}
```

Changes: `bg-gray-900`→`bg-k-dark`, `bg-gray-700`→`bg-k-sidebar-active`, `border-gray-700`→`border-k-sidebar-active`, `w-64`→`w-[220px]`.

- [ ] **Step 5.2: Update the desktop sidebar `<aside>` (≈ lines 422–451)**

Find:

```tsx
<aside
  className={[
    "hidden lg:flex flex-col bg-gray-900 text-white h-screen sticky top-0 shrink-0",
    "transition-[width] duration-300 ease-in-out overflow-hidden",
    collapsed ? "w-16" : "w-64",
  ].join(" ")}
>
  {/* Sidebar header */}
  <div
    className={[
      "flex items-center shrink-0 border-b border-gray-700 px-3 py-4",
      collapsed ? "justify-center" : "justify-between",
    ].join(" ")}
  >
    <Brand tenantName={tenantName} collapsed={collapsed} />
    <div className="flex items-center gap-1 shrink-0">
      {!collapsed && <NotificationBell />}
      <button
        onClick={() => setCollapsed((c) => !c)}
        aria-label={collapsed ? t("expandSidebar") : t("collapseSidebar")}
        className="p-1 text-gray-400 hover:text-white rounded transition-colors"
      >
        {collapsed ? (
          <Menu className="h-5 w-5" />
        ) : (
          <ChevronLeft className="h-5 w-5" />
        )}
      </button>
    </div>
  </div>
```

Replace with:

```tsx
<aside
  className={[
    "hidden lg:flex flex-col bg-k-dark text-white h-screen sticky top-0 shrink-0",
    "transition-[width] duration-300 ease-in-out overflow-hidden",
    collapsed ? "w-16" : "w-[220px]",
  ].join(" ")}
>
  {/* Sidebar header */}
  <div
    className={[
      "flex items-center shrink-0 border-b border-k-sidebar-active px-3 py-4",
      collapsed ? "justify-center" : "justify-between",
    ].join(" ")}
  >
    <Brand tenantName={tenantName} collapsed={collapsed} />
    <div className="flex items-center gap-1 shrink-0">
      {!collapsed && <NotificationBell />}
      <button
        onClick={() => setCollapsed((c) => !c)}
        aria-label={collapsed ? t("expandSidebar") : t("collapseSidebar")}
        className="p-1 text-k-subtle hover:text-white rounded transition-colors"
      >
        {collapsed ? (
          <Menu className="h-5 w-5" />
        ) : (
          <ChevronLeft className="h-5 w-5" />
        )}
      </button>
    </div>
  </div>
```

Changes: `bg-gray-900`→`bg-k-dark`, expanded width `w-64`→`w-[220px]`, header divider `border-gray-700`→`border-k-sidebar-active`, chevron button `text-gray-400`→`text-k-subtle`.

- [ ] **Step 5.3: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 5.4: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "refactor(layout): migrate sidebar container, widths, and skeleton to design tokens"
```

---

## Task 6: Sidebar `UserFooter` dedupe + footer color tokens, delete `MobileUserFooter`

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`
  - `UserFooter` (lines 194–242)
  - Delete `MobileUserFooter` (lines 245–283)
  - Drawer footer call site (around line 408)

- [ ] **Step 6.1: Replace `UserFooter` (lines 194–242)**

Replace the entire existing `UserFooter` function with:

```tsx
// User identity block shown at the bottom of the sidebar.
// `forceExpanded` lets the mobile drawer reuse this component without honoring `collapsed`.
function UserFooter({
  role,
  displayName,
  identityDocumentType,
  identityNumber,
  collapsed,
  forceExpanded,
  onLogout,
  signOut,
}: {
  role: Role;
  displayName: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
  collapsed: boolean;
  forceExpanded?: boolean;
  onLogout: () => void;
  signOut: string;
}) {
  const expanded = forceExpanded || !collapsed;
  return (
    <div className="px-2 py-4 border-t border-k-sidebar-active shrink-0">
      {expanded && (
        <div className="px-3 mb-2 space-y-0.5">
          {displayName && (
            <p className="text-xs font-medium text-white truncate">
              {displayName}
            </p>
          )}
          <p className="text-xs text-k-subtle truncate">{role}</p>
          {identityDocumentType && identityNumber && (
            <p className="text-xs text-k-subtle truncate">
              {identityDocumentType} {identityNumber}
            </p>
          )}
        </div>
      )}
      <button
        onClick={onLogout}
        title={!expanded ? signOut : undefined}
        className={[
          "flex items-center gap-3 w-full px-3 py-2 rounded-md text-sm text-k-subtle",
          "hover:text-white hover:bg-k-sidebar-active transition-colors",
          !expanded ? "justify-center" : "",
        ].join(" ")}
      >
        <LogOut className="h-5 w-5 shrink-0" />
        {expanded && <span>{signOut}</span>}
      </button>
    </div>
  );
}
```

Changes:
- New optional `forceExpanded?: boolean` prop
- Internal `expanded = forceExpanded || !collapsed` decides whether the identity block renders and whether the sign-out button is centered/labelled
- `border-gray-700`→`border-k-sidebar-active`
- Role/doc `text-gray-400` and `text-gray-500`→`text-k-subtle`
- Sign-out idle `text-gray-300`→`text-k-subtle`
- Sign-out hover `hover:bg-gray-800`→`hover:bg-k-sidebar-active`

- [ ] **Step 6.2: Delete `MobileUserFooter` (lines 245–283)**

Remove the entire `MobileUserFooter` function declaration. It is no longer used.

- [ ] **Step 6.3: Update the drawer footer call site (≈ line 408)**

Find:

```tsx
{/* Drawer footer */}
{user && (
  <MobileUserFooter
    role={primaryUserRole!}
    displayName={displayName}
    identityDocumentType={identityDocumentType}
    identityNumber={identityNumber}
    onLogout={logout}
    signOut={t("signOut")}
  />
)}
```

Replace with:

```tsx
{/* Drawer footer */}
{user && (
  <UserFooter
    role={primaryUserRole!}
    displayName={displayName}
    identityDocumentType={identityDocumentType}
    identityNumber={identityNumber}
    collapsed={false}
    forceExpanded
    onLogout={logout}
    signOut={t("signOut")}
  />
)}
```

- [ ] **Step 6.4: Verify desktop call site is unchanged**

The desktop sidebar call site (around the bottom of the desktop `<aside>`) should remain:

```tsx
{user && (
  <UserFooter
    role={primaryUserRole!}
    displayName={displayName}
    identityDocumentType={identityDocumentType}
    identityNumber={identityNumber}
    collapsed={collapsed}
    onLogout={logout}
    signOut={t("signOut")}
  />
)}
```

(No `forceExpanded` — it defaults to `undefined`, so `expanded = !collapsed`, preserving the existing behavior.)

- [ ] **Step 6.5: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors. (If it complains about an unused import for `MobileUserFooter`, there shouldn't be one — `MobileUserFooter` was module-internal only.)

- [ ] **Step 6.6: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "refactor(layout): dedupe UserFooter, drop MobileUserFooter, migrate footer to design tokens"
```

---

## Task 7: Dashboard layout wrapper bg + main padding

**Files:**
- Modify: `web/src/app/(dashboard)/layout.tsx` (lines 26–38)

- [ ] **Step 7.1: Replace the wrapper + main**

Find:

```tsx
return (
  <div className="flex min-h-screen bg-gray-50">
    <Suspense fallback={<SidebarSkeleton />}>
      <Sidebar />
    </Suspense>
    {/*
      pt-[4.5rem]: on mobile, clears the fixed topbar (3.5rem = 56px) + 1rem breathing room.
      lg:pt-8:     on desktop, the sidebar is sticky in flow so no topbar — use standard padding.
    */}
    <main className="flex-1 overflow-y-auto px-4 pb-4 pt-[4.5rem] lg:p-8">
      {children}
    </main>
  </div>
);
```

Replace with:

```tsx
return (
  <div className="flex min-h-screen bg-k-bg">
    <Suspense fallback={<SidebarSkeleton />}>
      <Sidebar />
    </Suspense>
    {/*
      pt-20: on mobile, clears the fixed topbar (h-14 = 56px) with 24px breathing room (matches p-6 elsewhere).
      lg:p-9: on desktop, the sidebar is sticky in flow so no topbar — uniform 36px padding.
    */}
    <main className="flex-1 overflow-y-auto pt-20 px-6 pb-6 lg:p-9">
      {children}
    </main>
  </div>
);
```

Changes:
- Wrapper `bg-gray-50`→`bg-k-bg`
- Main mobile padding `px-4 pb-4 pt-[4.5rem]`→`pt-20 px-6 pb-6` (24px sides/bottom; 80px top to clear 56px topbar with 24px breathing)
- Main desktop `lg:p-8`→`lg:p-9` (36px)
- Comment updated to match new values

- [ ] **Step 7.2: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 7.3: Commit**

```bash
git add web/src/app/\(dashboard\)/layout.tsx
git commit -m "refactor(layout): migrate dashboard wrapper bg and main padding to design tokens"
```

---

## Task 8: Dashboard `SidebarSkeleton` tokens + width

**Files:**
- Modify: `web/src/app/(dashboard)/layout.tsx` (lines 4–18)

- [ ] **Step 8.1: Replace `SidebarSkeleton`**

Find:

```tsx
function SidebarSkeleton() {
  return (
    <>
      {/* Mobile topbar skeleton */}
      <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-gray-900 border-b border-gray-700 animate-pulse" />
      {/* Desktop sidebar skeleton */}
      <aside className="hidden lg:flex w-64 bg-gray-900 h-screen sticky top-0 flex-col shrink-0">
        <div className="p-6 space-y-2">
          <div className="h-6 w-24 bg-gray-700 rounded animate-pulse" />
          <div className="h-4 w-32 bg-gray-700 rounded animate-pulse" />
        </div>
      </aside>
    </>
  );
}
```

Replace with:

```tsx
function SidebarSkeleton() {
  return (
    <>
      {/* Mobile topbar skeleton */}
      <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-k-dark border-b border-k-sidebar-active animate-pulse" />
      {/* Desktop sidebar skeleton */}
      <aside className="hidden lg:flex w-[220px] bg-k-dark h-screen sticky top-0 flex-col shrink-0">
        <div className="p-6 space-y-2">
          <div className="h-6 w-24 bg-k-sidebar-active rounded animate-pulse" />
          <div className="h-4 w-32 bg-k-sidebar-active rounded animate-pulse" />
        </div>
      </aside>
    </>
  );
}
```

Changes: `bg-gray-900`→`bg-k-dark`, `bg-gray-700`→`bg-k-sidebar-active`, `border-gray-700`→`border-k-sidebar-active`, `w-64`→`w-[220px]` (matches new sidebar expanded width — prevents layout shift on auth resolution).

- [ ] **Step 8.2: Type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 8.3: Commit**

```bash
git add web/src/app/\(dashboard\)/layout.tsx
git commit -m "refactor(layout): migrate dashboard SidebarSkeleton to design tokens and new width"
```

---

## Task 9: Final verification gate

No code changes. Confirm the migration is clean and complete.

- [ ] **Step 9.1: Confirm zero stale gray/red/indigo classes in scoped files**

Run from repo root:

```bash
grep -rn "bg-gray-900\|bg-gray-700\|bg-gray-800\|text-gray-300\|text-gray-400\|text-gray-500\|bg-red-500\|text-indigo-400\|bg-gray-50" \
  web/src/components/layout web/src/app/\(dashboard\)/layout.tsx
```

Expected: zero matches.

- [ ] **Step 9.2: Confirm zero `brandSubtitle` references anywhere**

```bash
grep -rn "brandSubtitle" web/src web/messages
```

Expected: zero matches.

- [ ] **Step 9.3: Final type-check**

```bash
cd web && npx tsc --noEmit
```
Expected: zero errors.

- [ ] **Step 9.4: Lint (if configured)**

```bash
cd web && npm run lint --if-present
```
Expected: zero new warnings/errors.

- [ ] **Step 9.5: Manual visual smoke matrix**

```bash
cd web && npm run dev
```

Open `http://localhost:3000` (logged in). Walk through this matrix from spec §11:

| Viewport | Route / Action | Verify |
|---|---|---|
| Desktop ≥1024px | `/students` | sidebar `w-[220px]`, dark bg `#0A0A0A`, KLogo top-left, tenant name in subtle gray, Students item has lime 3px accent bar + lime icon, all other icons subtle, hover other = bg `#1A1A1A` |
| Desktop, collapsed | toggle chevron | `w-16`, accent bar still on active, icon centered, tooltip on hover |
| Desktop | `/payment-proofs` (admin only) | red badge `bg-k-danger-text` (#CC2200) next to label when count > 0; collapsed dot indicator visible on icon |
| Mobile <1024px | `/students` | fixed topbar dark `#0A0A0A`, KLogo + tenant subtle, hamburger opens drawer |
| Mobile drawer | open | drawer dark, KLogo header, no subtitle line, nav items match desktop, footer with name/role/doc + sign out |
| Any | route change while drawer open | drawer auto-closes |
| Any | press Escape with drawer open | drawer closes |
| Any | drawer open | body scroll locked |
| Any | initial load (slow throttle in DevTools) | skeleton dark + `w-[220px]`, no flash of `bg-gray-50` |
| Any | toggle locale en↔es (logout/login as different-language tenant) | nav labels switch language, brand stays "klasio", tenant name shown |

Open Chrome DevTools → Elements → inspect computed colors. Verify they match the token map (e.g. `bg-k-dark` resolves to `rgb(10, 10, 10)` = `#0A0A0A`).

Stop the dev server when done.

- [ ] **Step 9.6: (Optional) Final summary commit**

If any small fix-ups arose during smoke (e.g. a missed class), commit them. Otherwise nothing to do — Tasks 1–8 already produced the full migration.

---

## Self-Review (against spec)

| Spec section | Covered by |
|---|---|
| §1 Goal | Tasks 1–8 in aggregate |
| §2 Files Touched | Tasks 1, 2, 3, 4, 5, 6, 7, 8 |
| §3 Decisions: tokens | Tasks 2–8 |
| §3 Decisions: KLogo | Tasks 1, 2, 3 |
| §3 Decisions: drop brandSubtitle | Task 2 (en/es JSON + Brand component) + Task 3 (drawer subtitle line removed) |
| §3 Decisions: pt-20 mobile padding | Task 7 |
| §3 Decisions: Approach B refactor | Task 4 (helper extract), Task 6 (footer dedupe) |
| §4 Token map | Distributed across Tasks 2–8 |
| §5 KLogo component | Task 1 |
| §6.1 Widths | Tasks 5 (live), 8 (skeleton) |
| §6.2 Active accent indicator | Task 4 |
| §6.3 Nav item className helper | Task 4 |
| §6.4 Notification badge | Task 4 |
| §6.5 Brand block | Task 2 |
| §6.6 Mobile topbar | Task 3 |
| §6.7 Mobile drawer | Task 3 |
| §6.8 Collapse / chevron button | Task 5 |
| §6.9 Footer dedupe | Task 6 |
| §6.10 Inline auth-loading skeleton | Task 5 |
| §7.1 Wrapper + main | Task 7 |
| §7.2 SidebarSkeleton | Task 8 |
| §8 i18n cleanup | Task 2 |
| §9 Preserved logic | Implicit in every task — no logic touched. Verified by tsc + manual smoke. |
| §10 Edge cases E1–E10 | Verified by Task 9.5 smoke matrix |
| §11 Verification | Task 9 |
| §12 Out of scope | Honored — no tests for KLogo, no skeleton dedupe across files, no login redesign, no notification-bell internals |
| §13 Risk & rollback | Each commit is independently revertable |

No placeholders, no TBDs. Method/prop signatures consistent across tasks (`UserFooter` props in Task 6 match call sites in same task; `KLogo` props in Task 1 match imports in Task 2/3).

Implementation plan complete.
