# Design System Migration — Klasio Web

**Date:** 2026-04-24
**Scope:** Config and CSS only. Zero component changes.
**Branch:** current working branch (main)

---

## Goal

Replace the minimal Tailwind config with a complete design token layer. Provides a canonical palette, spacing, shadow, and typography contract that all future components consume via `k-*` class names.

---

## Files Changed

| File | Change |
|---|---|
| `web/tailwind.config.ts` | Add `theme.extend` tokens (colors, borderRadius, boxShadow, transitionDuration) |
| `web/src/app/layout.tsx` | Load DM Sans + DM Mono via `next/font/google`; inject CSS variables; remove redundant `bg-gray-50` |
| `web/src/app/globals.css` | Full replacement: keep `@tailwind` directives, add `@layer base` defaults and scrollbar styles |

---

## Typography

Loaded via `next/font/google` in `layout.tsx`. Exposed as CSS variables on `<html>`.

| Variable | Font | Weights |
|---|---|---|
| `--font-main` | DM Sans | 300, 400, 500, 600, 700, 800 |
| `--font-mono` | DM Mono | 400, 500 |

Applied in `@layer base`: `body { font-family: var(--font-main); }`.

---

## Color Tokens (`theme.extend.colors`)

All tokens use `k-` prefix. Additive — no existing Tailwind color removed.

| Token | Hex | Usage |
|---|---|---|
| `k-bg` | `#F4F4F2` | Page background |
| `k-surface` | `#FAFAF8` | Card / input background |
| `k-dark` | `#0A0A0A` | Sidebar, high-contrast elements |
| `k-ink` | `#2A2A28` | Body text |
| `k-muted` | `#9A9A98` | Secondary text, table headers |
| `k-subtle` | `#4A4A48` | Tertiary text |
| `k-border` | `#DDDDD8` | Default border |
| `k-line` | `#EBEBEA` | Table row dividers |
| `k-volt` | `#CAFF4D` | Primary accent (lime-green) |
| `k-volt-text` | `#2A4A00` | Text on volt background |
| `k-warn-bg` | `#FFF0C2` | Warning badge background |
| `k-warn-text` | `#8A5A00` | Warning badge text |
| `k-info-bg` | `#E8F4FF` | Info badge background |
| `k-info-text` | `#0066BB` | Info badge text |
| `k-danger-bg` | `#FFE8E8` | Danger badge background |
| `k-danger-text` | `#CC2200` | Danger badge text |
| `k-sidebar-active` | `#1A1A1A` | Active nav item background |

---

## Border Radius Tokens (`theme.extend.borderRadius`)

| Token | Value | Usage |
|---|---|---|
| `k-sm` | `8px` | Buttons, inputs, small elements |
| `k-md` | `12px` | Tables, medium cards |
| `k-lg` | `16px` | Stat cards |
| `k-xl` | `20px` | Modals, large panels |

---

## Shadow Tokens (`theme.extend.boxShadow`)

| Token | Value |
|---|---|
| `k-card` | `0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)` |
| `k-modal` | `0 24px 80px rgba(0,0,0,0.25)` |
| `k-dropdown` | `0 8px 24px rgba(0,0,0,0.12)` |

---

## Transition Duration Token

| Token | Value |
|---|---|
| `k` | `150ms` |

Applied via `duration-k` Tailwind class.

---

## `globals.css` Structure

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html {
    -webkit-font-smoothing: antialiased;
  }

  body {
    @apply bg-k-bg text-k-ink;
    font-family: var(--font-main);
    --font-mono: <injected by layout>;
  }
}

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #DDDDD8; border-radius: 99px; }
```

---

## `layout.tsx` Changes

- Import `DM_Sans` and `DM_Mono` from `next/font/google`
- Assign CSS variable names (`variable: '--font-main'`, `variable: '--font-mono'`)
- Apply both font `variable` classes to `<html>` element
- Remove `bg-gray-50` from `<body>` className (`@layer base` owns background)
- Keep `min-h-screen` on `<body>`

---

## Constraints

- No component files modified
- No existing Tailwind classes removed from any file
- All token names exactly as specified with `k-` prefix
- Compatible with Tailwind CSS v3
- Build must pass with zero TypeScript errors

---

## Verification

Run `npm run build` from `web/` directory. Zero type errors = success.
