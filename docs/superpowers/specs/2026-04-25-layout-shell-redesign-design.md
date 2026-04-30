# Layout Shell Redesign — Design Spec

**Date:** 2026-04-25
**Scope:** Step 4 of design system migration — migrate `Sidebar.tsx` and dashboard `layout.tsx` to the Klasio design language.
**Depends on:** Steps 1–3 (design tokens, primitives, compound components — all merged on `feature/full-redesign`).
**Out of scope:** Login layout, notification bell internals, page-level content, new features, backend.

---

## 1. Goal

Visually migrate the dashboard layout shell (sidebar + main content area) to the Klasio dark-sidebar / light-canvas design while preserving every existing behavior: auth gating, role-based navigation, mobile drawer, notification bell, i18n, collapse toggle, keyboard handling, body scroll lock.

The migration is purely structural and stylistic. No new pages, no new hooks, no new routes, no new permissions, no behavioral changes.

## 2. Files Touched

| File | Change |
|---|---|
| `web/src/components/layout/Sidebar.tsx` | Token swap, active accent bar, widths, footer dedupe, KLogo import, drop `brandSubtitle` line |
| `web/src/components/layout/KLogo.tsx` | **NEW** — wordmark component |
| `web/src/app/(dashboard)/layout.tsx` | Main bg → `bg-k-bg`, padding `pt-20 px-6 pb-6 lg:p-9`, skeleton tokens + width |
| `web/messages/en.json` | Remove `layout.brandSubtitle` key |
| `web/messages/es.json` | Remove `layout.brandSubtitle` key |

## 3. Design Decisions (resolved during brainstorming)

| Decision | Choice | Rationale |
|---|---|---|
| Color expression | **Tailwind named tokens** (`bg-k-dark`, `text-k-muted`, `text-k-subtle`, `bg-k-sidebar-active`, `bg-k-danger-text`, `text-k-volt`, `bg-k-volt`, `bg-k-bg`) | Tokens already defined in `tailwind.config.ts`. Step 1 of migration created them precisely so consumers stop hardcoding hex. Raw `[#hex]` here would re-introduce what tokens were meant to eliminate. |
| Brand mark | **`KLogo` component** | Likely reused in login, emails, error pages. Centralizing the wordmark now avoids future churn. |
| `brandSubtitle` ("Sports league management") | **Drop entirely** | Tenant identity is the meaningful subtitle; static brochure copy is noise. Spec replaces it with tenant name in the same slot. |
| Mobile fixed-topbar clearance | **Keep clearance via `pt-20`** | Spec literal `p-6` would slide content under the 56px fixed topbar. `pt-20 px-6 pb-6 lg:p-9` preserves the spec's `p-6 / lg:p-9` semantics on horizontal+bottom while clearing the topbar on mobile. |
| Migration scope | **Targeted refactor while migrating** (Approach B) | Real duplication exists in `Sidebar.tsx` (`UserFooter` vs `MobileUserFooter`, repeated nav-item className build). Dedupe is small, motivated, reduces future churn. Avoid full structural split (Approach C) — speculative for current pain. |

## 4. Token Map

| Spec hex | Token | Usage |
|---|---|---|
| `#0A0A0A` | `bg-k-dark` | sidebar bg, mobile topbar bg, drawer bg |
| `#1A1A1A` | `bg-k-sidebar-active` / `border-k-sidebar-active` | active item bg, hover bg, dividers |
| `#9A9A98` | `text-k-muted` | inactive nav text |
| `#4A4A48` | `text-k-subtle` | inactive icon, role label, doc id, tenant subtitle, sign-out idle, collapse-button idle |
| `#CAFF4D` | `bg-k-volt` / `text-k-volt` | active accent bar, active icon |
| `#CC2200` | `bg-k-danger-text` | notification badge bg, collapsed dot indicator |
| `#F4F4F2` | `bg-k-bg` | main content area |
| `text-white` | `text-white` | active item text, brand wordmark, user name, hover text |

All tokens already defined in `web/tailwind.config.ts`. No config edit required.

## 5. KLogo Component

`web/src/components/layout/KLogo.tsx`:

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

- Wordmark only (no SVG)
- `select-none` prevents accidental highlight on collapse toggle
- `className` passthrough for future contexts (login, emails as HTML, error pages)
- No i18n — brand is `klasio` in every locale

Imported by `Sidebar.tsx` in three places: desktop expanded brand block, mobile drawer header, mobile topbar.

## 6. Sidebar.tsx — Visual Changes

### 6.1 Widths
- Desktop expanded: `w-[220px]` (was `w-64`)
- Desktop collapsed: `w-16` (unchanged)
- Mobile drawer: `w-64` (unchanged)

### 6.2 Active accent indicator
Each `<li>` and the active `<Link>` are `relative`. Active link renders:

```tsx
{isActive && (
  <span
    aria-hidden="true"
    className="absolute left-0 top-[20%] bottom-[20%] w-[3px] bg-k-volt rounded-r-full"
  />
)}
```

Indicator is inside `<Link>` (not `<li>`) so the click target stays the full row and the indicator visually anchors to the rounded item background.

### 6.3 Nav item className helper
Extract:

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

Icon coloring:

```tsx
<Icon className={`h-5 w-5 ${isActive ? "text-k-volt" : "text-k-subtle"}`} />
```

### 6.4 Notification badge
- `bg-red-500` → `bg-k-danger-text` (full badge)
- `bg-red-500` → `bg-k-danger-text` (collapsed dot indicator)
- White text + min-width preserved

### 6.5 Brand block (desktop expanded)
```tsx
<div className="overflow-hidden">
  <KLogo />
  {tenantName && (
    <p className="text-xs text-k-subtle whitespace-nowrap mt-0.5 truncate">
      {tenantName}
    </p>
  )}
</div>
```
- `brandSubtitle` paragraph removed entirely
- Tenant moves from `text-indigo-400` → `text-k-subtle`

### 6.6 Mobile topbar
- `bg-gray-900` → `bg-k-dark`
- `border-gray-700` → `border-k-sidebar-active`
- Brand `<span>{t("brand")}</span>` → `<KLogo />`
- Tenant `text-indigo-400` → `text-k-subtle`
- Hamburger `text-gray-300 hover:text-white` → `text-k-subtle hover:text-white`

### 6.7 Mobile drawer
- Drawer `bg-gray-900` → `bg-k-dark`
- Header divider `border-gray-700` → `border-k-sidebar-active`
- Brand → `<KLogo />`
- Subtitle line removed
- Tenant `text-indigo-400` → `text-k-subtle`
- Close button → `text-k-subtle hover:text-white`
- Backdrop `bg-black/60 backdrop-blur-sm` unchanged

### 6.8 Collapse / chevron button (desktop)
`text-gray-400 hover:text-white` → `text-k-subtle hover:text-white`

### 6.9 Footer dedupe
Single `UserFooter` component, accept `forceExpanded?: boolean`:

```tsx
function UserFooter({
  role, displayName, identityDocumentType, identityNumber,
  collapsed, forceExpanded, onLogout, signOut,
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
  // ... renders identity block when `expanded`, button always rendered
}
```

Call sites:
- Desktop sidebar: `<UserFooter ... collapsed={collapsed} />`
- Mobile drawer: `<UserFooter ... collapsed={false} forceExpanded />`

`MobileUserFooter` deleted.

Footer color swaps:
- Border `border-gray-700` → `border-k-sidebar-active`
- Name `text-white` (unchanged)
- Role `text-gray-400` → `text-k-subtle`
- Doc `text-gray-500` → `text-k-subtle`
- Sign-out idle `text-gray-300` → `text-k-subtle`
- Sign-out hover `hover:bg-gray-800` → `hover:bg-k-sidebar-active`

### 6.10 Inline auth-loading skeleton (in `Sidebar.tsx` `loading` branch)
- `bg-gray-900` → `bg-k-dark`
- `bg-gray-700` → `bg-k-sidebar-active`
- `border-gray-700` → `border-k-sidebar-active`
- `w-64` → `w-[220px]`

## 7. Dashboard layout.tsx

### 7.1 Wrapper + main
```tsx
<div className="flex min-h-screen bg-k-bg">
  <Suspense fallback={<SidebarSkeleton />}>
    <Sidebar />
  </Suspense>
  <main className="flex-1 overflow-y-auto pt-20 px-6 pb-6 lg:p-9">
    {children}
  </main>
</div>
```

Changes:
- `bg-gray-50` → `bg-k-bg`
- Mobile padding `px-4 pb-4 pt-[4.5rem]` → `pt-20 px-6 pb-6` (24px sides/bottom; 80px top clears 56px fixed topbar with 24px breathing room — matches `p-6` semantics elsewhere)
- Desktop `lg:p-8` → `lg:p-9` (36px)

### 7.2 SidebarSkeleton (Suspense fallback)
```tsx
<>
  <div className="lg:hidden fixed top-0 inset-x-0 z-40 h-14 bg-k-dark border-b border-k-sidebar-active animate-pulse" />
  <aside className="hidden lg:flex w-[220px] bg-k-dark h-screen sticky top-0 flex-col shrink-0">
    <div className="p-6 space-y-2">
      <div className="h-6 w-24 bg-k-sidebar-active rounded animate-pulse" />
      <div className="h-4 w-32 bg-k-sidebar-active rounded animate-pulse" />
    </div>
  </aside>
</>
```

Aligned to new sidebar width and tokens — prevents layout shift on auth resolution.

## 8. i18n Cleanup

Remove key `layout.brandSubtitle` from:
- `web/messages/en.json`
- `web/messages/es.json`

Pre-removal sanity check:
```bash
grep -rn "brandSubtitle" web/src web/messages
```

Expected: matches only in `Sidebar.tsx` (about to be removed) and the two JSON files. If found elsewhere, address before commit.

Other `layout.*` keys preserved untouched: `brand`, `navTenants`, `navAdmins`, `navManagers`, `navProfessors`, `navStudents`, `navPrograms`, `navPlans`, `navClasses`, `navPaymentProofs`, `navDashboard`, `navMyMemberships`, `navMyEnrollments`, `navMyClasses`, `navMyRegistrations`, `signOut`, `openNav`, `closeNav`, `expandSidebar`, `collapseSidebar`, `notificationsBadgeMax`.

## 9. Preserved Logic

The following behaviors must remain identical after the migration:

- `useAuth()` loading branch returns skeleton; logged-out user redirect logic untouched
- `useSidebarIdentity(primaryUserRole, user?.tenantId)` call signature and destructure
- `usePendingProofsCount(canSeeProofQueue)` with `canSeeProofQueue = ADMIN || SUPERADMIN`
- `primaryRole(user.roles)` selection
- `computeNavItems(roles, navItemsByRole)` — role-union dedupe by href, ordered by privilege
- `useTranslations("layout")` namespace
- Mobile drawer state `mobileOpen` + close-on-route-change effect
- Escape key handler (drawer close)
- Body scroll lock effect while drawer open
- `setCollapsed` toggle
- `NotificationBell` in mobile topbar and desktop expanded header
- Active path match `pathname === href || pathname.startsWith(href + "/")`

## 10. Edge Cases

| ID | Case | Handling |
|---|---|---|
| E1 | Active item collapsed mode | Accent bar still renders (3px left edge). Active icon = `text-k-volt`. Pending-proof dot indicator coexists with active icon — visually stable. |
| E2 | Nested route active match | `/students/123` matches `Students` nav item. Accent bar shows. No change. |
| E3 | NotificationBadge >10 | Renders `t("notificationsBadgeMax")` ("10+"). Bg `bg-k-danger-text`. White text. min-width preserved. |
| E4 | No tenantName | Brand block renders `<KLogo />` only. No subtitle paragraph. |
| E5 | Skeleton → real sidebar transition | Both states `bg-k-dark` + `w-[220px]`. No horizontal jump; vertical content fade only. |
| E6 | Mobile drawer click outside | Backdrop unchanged. |
| E7 | Long tenant name | `truncate` preserved. `text-k-subtle` (#4A4A48) on `bg-k-dark` (#0A0A0A) is a pre-existing design system contrast decision. |
| E8 | Multi-role user (e.g. ADMIN + STUDENT) | `computeNavItems` dedupes by href, ordered by `primaryRole`. No change. |
| E9 | Suspense fallback (auth boot) | `bg-k-bg` applied immediately on wrapper — no `bg-gray-50` flash. |
| E10 | Two skeleton sources (Suspense fallback + `loading` branch) | Both updated identically. Future dedupe possible — out of scope. |

## 11. Verification

### Static
```bash
cd web
npx tsc --noEmit                    # zero errors
npm run lint                        # if configured; zero new warnings
grep -rn "brandSubtitle" src messages  # zero matches after cleanup
grep -rn "bg-gray-900\|bg-gray-700\|text-gray-300\|text-gray-400\|text-gray-500\|bg-red-500\|text-indigo-400" \
  src/components/layout src/app/\(dashboard\)/layout.tsx
# zero matches expected (scoped to migrated files)
```

### Visual smoke (manual, `npm run dev`)

| Viewport | Route / Action | Verify |
|---|---|---|
| Desktop ≥1024px | `/students` | sidebar `w-[220px]`, dark bg, KLogo top-left, tenant subtle gray, Students item has lime 3px accent bar + lime icon, all other icons subtle, hover other = bg `#1A1A1A` |
| Desktop, collapsed | toggle chevron | `w-16`, accent bar still on active, icon centered, tooltip on hover |
| Desktop | `/payment-proofs` (admin) | red badge `bg-k-danger-text` next to label when count >0; collapsed dot indicator visible on icon |
| Mobile <1024px | `/students` | fixed topbar `bg-k-dark`, KLogo + tenant, hamburger opens drawer |
| Mobile drawer | open | drawer `bg-k-dark`, KLogo header, nav items match desktop, footer with name/role/doc + sign out |
| Any | route change while drawer open | drawer auto-closes |
| Any | press Escape with drawer open | drawer closes |
| Any | drawer open | body scroll locked |
| Any | initial load (slow throttle) | skeleton uses `bg-k-dark` + `w-[220px]`, no flash of `bg-gray-50` |
| Any | toggle locale en↔es | nav labels switch, brand stays "klasio", tenant rendered |

### Browser inspection (per project CLAUDE.md "test UI in browser before reporting done")
Open Chrome DevTools → toggle responsive mode 375px and 1280px. Verify computed colors match tokens via inspector.

## 12. Out of Scope

- Login page redesign
- Notification bell internal styling
- Page-level content (only layout shell + sidebar)
- Backend changes
- New i18n keys
- Tests for `KLogo` (presentational; manual smoke covers it)
- Skeleton dedupe across `Sidebar.tsx` and `layout.tsx` (future cleanup)

## 13. Risk & Rollback

**Risk:** low. Changes are visual + minor structural refactor in a single feature area. No data, no API, no auth change.

**Rollback:** revert the commit(s) introducing the migration. `KLogo.tsx` is a new file; deletion + revert of `Sidebar.tsx` and `layout.tsx` restores prior state. i18n key removal is reversible by re-adding the JSON entries.
