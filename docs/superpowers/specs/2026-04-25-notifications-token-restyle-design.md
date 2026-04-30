# Notifications Token Restyle — Design

**Date:** 2026-04-25
**Scope:** Frontend (web/) — visual restyle only
**Status:** Approved

## Goal

Migrate the three notifications components from raw Tailwind palette classes (gray/blue/indigo/amber/red) to Klasio design tokens. No logic, hooks, state, props, routing, or i18n keys change. The diff is restricted to `className` strings and two purely structural additions (an accent-bar span and an icon wrapper div).

## Non-Goals

- No behavior changes (read/unread, navigation, pagination logic untouched).
- No new props or component signatures.
- No new translation keys, no edits to `messages/en.json` / `messages/es.json`.
- No refactor of `useNotifications` / `useMarkNotificationRead` / `useMarkAllNotificationsRead` hooks or `NotificationCountContext`.
- No accessibility regressions: focus-visible ring preserved (token-colored), button semantics unchanged.

## Files in Scope

1. `web/src/components/notifications/NotificationItem.tsx`
2. `web/src/components/notifications/NotificationTypeIcon.tsx`
3. `web/src/components/notifications/NotificationList.tsx`

## Token Inventory (verified present in `web/tailwind.config.ts`)

Colors: `k-bg` `#F4F4F2`, `k-surface` `#FAFAF8`, `k-dark` `#0A0A0A`, `k-muted` `#9A9A98`, `k-border` `#DDDDD8`, `k-line` `#EBEBEA`, `k-volt` `#CAFF4D`, `k-warn-text` `#8A5A00`, `k-danger-text` `#CC2200`. Radii: `rounded-k-sm` (8px), `rounded-k-md` (12px). Shadow: `shadow-k-card`. Mono font via `var(--font-mono)`. All confirmed present in `web/tailwind.config.ts`.

## Component Changes

### 1. NotificationItem.tsx

**Outer `<button>`**
- Add `relative` to className (anchors the unread accent bar).
- Background: `notification.read ? "bg-k-surface" : "bg-[#F9FFEA]"` (subtle volt tint for unread; literal hex chosen because it is not yet a named token).
- Hover: `hover:bg-gray-50` → `hover:bg-k-bg`.
- Focus ring: `focus-visible:ring-indigo-500` → `focus-visible:ring-k-volt`. Keep `focus-visible:ring-2` and `focus:outline-none`.

**Unread accent bar** (new sibling, first child inside the button, only when `!notification.read`):
```tsx
{!notification.read && (
  <span className="absolute left-0 top-0 bottom-0 w-[3px] bg-k-volt rounded-r-full" />
)}
```

**Icon wrapper** — replace the existing `<div className="mt-0.5 shrink-0">` with:
```tsx
<div className="mt-0.5 shrink-0 w-8 h-8 rounded-[8px] bg-k-bg flex items-center justify-center">
  <NotificationTypeIcon type={notification.type} />
</div>
```

**Typography**
- Title `<p>`: `text-gray-900` → `text-k-dark`. Keep `font-medium text-sm truncate`.
- Body `<p>`: `text-gray-500` → `text-k-muted`. Keep `text-xs mt-0.5 line-clamp-2`.
- Timestamp `<p>`: `text-xs text-gray-400 mt-1` → `text-[#9A9A98] font-[var(--font-mono)] text-[10px] mt-1`.

### 2. NotificationTypeIcon.tsx

- `CLASS_SESSION_ALERTED`: `AlertTriangle` `text-amber-500` → `text-k-warn-text`.
- `CLASS_SESSION_CANCELLED`: `Bell` `text-red-500` → `text-k-danger-text`.
- Default: `Bell` `text-gray-400` → `text-k-muted`.
- Sizing (`w-5 h-5`) unchanged.

### 3. NotificationList.tsx

**Tab toggle** — replace the entire toggle block:
```tsx
<div className="flex gap-1 bg-k-bg rounded-k-sm p-1">
  <button
    type="button"
    onClick={() => handleTabChange(false)}
    className={[
      "px-3 py-1 text-sm rounded-[6px] transition-colors",
      !unreadOnly
        ? "bg-k-surface font-semibold text-k-dark shadow-k-card"
        : "text-k-muted hover:text-k-dark",
    ].join(" ")}
  >
    {t("tabAll")}
  </button>
  <button
    type="button"
    onClick={() => handleTabChange(true)}
    className={[
      "px-3 py-1 text-sm rounded-[6px] transition-colors",
      unreadOnly
        ? "bg-k-surface font-semibold text-k-dark shadow-k-card"
        : "text-k-muted hover:text-k-dark",
    ].join(" ")}
  >
    {t("tabUnreadOnly")}
  </button>
</div>
```

**Mark-all button**: `text-sm text-indigo-600 hover:text-indigo-800 transition-colors` → `text-k-muted hover:text-k-dark text-sm font-medium transition-colors`.

**List container `<div>`**: `rounded-lg border border-gray-200 divide-y divide-gray-100 overflow-hidden` → `rounded-k-md border border-k-border divide-y divide-k-line overflow-hidden`.

**Loading / empty `<p>`**: `text-gray-400` → `text-k-muted`. Keep `text-sm py-6 text-center`.

**Error `<p>`**: `text-red-500` → `text-k-danger-text`. Keep `text-sm py-6 text-center`.

**Pagination wrapper**: container keeps `flex items-center justify-between text-sm`, but replace the wrapper's `text-gray-500` → `text-k-muted`. The summary `<span>` adopts `font-[var(--font-mono)] text-xs` to match the timestamp register.

**Pagination buttons**: `border-gray-200 hover:bg-gray-50` → `border-k-border hover:bg-k-bg`. Keep `px-3 py-1 rounded disabled:opacity-40 transition-colors`.

## Verification

- `cd web && npx tsc --noEmit` returns zero errors.
- Manual smoke: render `/notifications` route logged in; verify unread item shows volt left bar + tinted bg; hover changes to `k-bg`; keyboard focus shows volt ring; tab switch toggles active pill; mark-all click triggers existing handler; loading/empty/error states render with new colors; pagination buttons hover correctly.
- Confirm grep returns zero matches in the three files: `bg-blue-50|bg-white|bg-gray-50|ring-indigo|text-gray-900|text-gray-500|text-gray-400|text-indigo-600|text-amber-500|text-red-500|border-gray-200|divide-gray-100`.

## Risk & Rollback

Risk is minimal — pure className swaps plus two purely structural element additions (accent span + icon wrapper). Rollback is `git revert` on the feature commit. No DB, no API, no contract change.
