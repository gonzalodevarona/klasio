# Design System Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add canonical Klasio design tokens (colors, radius, shadow, typography) to Tailwind config and CSS base layer without touching any component files.

**Architecture:** Three-file change — Tailwind config gains all `k-*` tokens under `theme.extend`, `globals.css` gains `@layer base` defaults + scrollbar styles, `layout.tsx` loads DM Sans + DM Mono via `next/font/google` and injects CSS variables. All changes are additive; no existing classes or component files are modified.

**Tech Stack:** Next.js 15, Tailwind CSS v3, `next/font/google`, TypeScript 5

---

## File Map

| Action | File | Responsibility |
|---|---|---|
| Modify | `web/tailwind.config.ts` | Add all `k-*` color, radius, shadow, transition tokens |
| Replace | `web/src/app/globals.css` | `@layer base` defaults + custom scrollbar |
| Modify | `web/src/app/layout.tsx` | Load DM Sans + DM Mono, expose CSS variables, remove redundant `bg-gray-50` |

---

### Task 1: Add design tokens to tailwind.config.ts

**Files:**
- Modify: `web/tailwind.config.ts`

- [ ] **Step 1: Replace tailwind.config.ts with the full token set**

```typescript
import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
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
  plugins: [],
};

export default config;
```

- [ ] **Step 2: Verify TypeScript compiles**

Run from `web/`:
```bash
npx tsc --noEmit
```
Expected: no output (zero errors)

- [ ] **Step 3: Commit**

```bash
git add web/tailwind.config.ts
git commit -m "chore(design-system): add k-* design tokens to tailwind config"
```

---

### Task 2: Replace globals.css with base layer + scrollbar

**Files:**
- Replace: `web/src/app/globals.css`

> Note: `@apply bg-k-bg text-k-ink` depends on Task 1 tokens being present. Do not run this task before Task 1 is committed.

- [ ] **Step 1: Replace globals.css**

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
  }
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #DDDDD8;
  border-radius: 99px;
}
```

- [ ] **Step 2: Verify build**

Run from `web/`:
```bash
npm run build
```
Expected: Build completes successfully with no errors. The `@apply bg-k-bg text-k-ink` tokens must resolve — if they don't, Task 1 tokens are missing from config.

- [ ] **Step 3: Commit**

```bash
git add web/src/app/globals.css
git commit -m "chore(design-system): replace globals.css with base layer and scrollbar styles"
```

---

### Task 3: Load DM Sans + DM Mono fonts in layout.tsx

**Files:**
- Modify: `web/src/app/layout.tsx`

- [ ] **Step 1: Replace layout.tsx**

```typescript
import type { Metadata } from "next";
import "./globals.css";
import { DM_Sans, DM_Mono } from "next/font/google";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages } from "next-intl/server";
import { NotificationCountProvider } from "@/context/NotificationCountContext";

const dmSans = DM_Sans({
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700", "800"],
  variable: "--font-main",
});

const dmMono = DM_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "Klasio - Sports League Management",
  description: "Multitenant platform for managing sports leagues",
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale} className={`${dmSans.variable} ${dmMono.variable}`}>
      <body className="min-h-screen">
        <NextIntlClientProvider locale={locale} messages={messages}>
          <NotificationCountProvider>{children}</NotificationCountProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
```

Key changes from original:
- Added `DM_Sans` + `DM_Mono` imports and font objects
- Both `.variable` classes applied to `<html>` → injects `--font-main` and `--font-mono` CSS variables
- Removed `bg-gray-50` from `<body>` (now owned by `@layer base` in globals.css)
- Kept `min-h-screen` on `<body>`

- [ ] **Step 2: Verify build with zero TypeScript errors**

Run from `web/`:
```bash
npm run build
```
Expected: Build completes successfully. Zero TypeScript errors. Font variables `--font-main` and `--font-mono` are injected by Next.js font system automatically.

- [ ] **Step 3: Commit**

```bash
git add web/src/app/layout.tsx
git commit -m "chore(design-system): load DM Sans and DM Mono via next/font/google"
```
