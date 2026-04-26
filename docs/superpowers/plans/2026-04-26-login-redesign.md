# Login Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the centered-card login layout with a full-screen horizontal split — dark brand panel on the left, form panel on the right — without touching any auth logic.

**Architecture:** Three files change. i18n keys first (no deps), then LoginForm styles (self-contained client component), then the page shell last (depends on both). No new components, no new routes, no logic changes.

**Tech Stack:** Next.js 15 App Router, next-intl, Tailwind CSS 3.4, React 19, TypeScript 5.9. Font CSS vars `--font-main` (DM Sans) and `--font-mono` (DM Mono) already defined in layout.

---

## File Map

| File | Change |
|---|---|
| `web/messages/es.json` | Update `loginPage.title` + `loginPage.subtitle`; add 8 new hero keys |
| `web/messages/en.json` | Same |
| `web/src/components/auth/LoginForm.tsx` | Style-only: inline label+input, plain button, inline error styles. Zero logic change. |
| `web/src/app/(auth)/login/page.tsx` | Full replace: two-panel server component with inline SVG logo and i18n hero copy |

> **Note on TDD:** This feature has no business logic to unit-test. The correctness signal is TypeScript (`npx tsc --noEmit`) + manual browser verification. Each task ends with a tsc check.

---

## Task 1: Update i18n keys

**Files:**
- Modify: `web/messages/es.json` (lines ~1007–1010, the `loginPage` section)
- Modify: `web/messages/en.json` (lines ~1007–1010, the `loginPage` section)

- [ ] **Step 1: Update `loginPage` section in `web/messages/es.json`**

Find and replace the entire `"loginPage"` block (currently only `title` + `subtitle`):

```json
"loginPage": {
  "title": "Iniciar sesión",
  "subtitle": "Ingresa tus credenciales para continuar",
  "tagline": "Gestión deportiva",
  "heroLine1": "Administra tu",
  "heroAccent": "liga",
  "heroLine2": " con",
  "heroLine3": "precisión.",
  "heroBody": "Control de asistencia, membresías, pagos y programas en una sola plataforma.",
  "tag1": "Membresías",
  "tag2": "Asistencia",
  "tag3": "Pagos"
},
```

- [ ] **Step 2: Update `loginPage` section in `web/messages/en.json`**

Find and replace the entire `"loginPage"` block:

```json
"loginPage": {
  "title": "Sign in",
  "subtitle": "Enter your credentials to continue",
  "tagline": "Sports management",
  "heroLine1": "Manage your",
  "heroAccent": "league",
  "heroLine2": " with",
  "heroLine3": "precision.",
  "heroBody": "Attendance, memberships, payments and programs — all in one platform.",
  "tag1": "Memberships",
  "tag2": "Attendance",
  "tag3": "Payments"
},
```

- [ ] **Step 3: Verify JSON is valid**

```bash
cd web && node -e "JSON.parse(require('fs').readFileSync('messages/es.json','utf8')); JSON.parse(require('fs').readFileSync('messages/en.json','utf8')); console.log('JSON valid')"
```

Expected output: `JSON valid`

- [ ] **Step 4: Commit**

```bash
git add web/messages/es.json web/messages/en.json
git commit -m "feat(login): add hero copy i18n keys to loginPage namespace"
```

---

## Task 2: Restyle LoginForm.tsx

**Files:**
- Modify: `web/src/components/auth/LoginForm.tsx`

All auth logic (`handleSubmit`, `setError`, `setLoading`, `showPassword`, `window.location.href`) is preserved exactly. Only imports and JSX styles change.

- [ ] **Step 1: Replace the full file content**

```tsx
"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import type { LoginResponse, AuthError } from "@/lib/types/auth";

export default function LoginForm() {
  const t = useTranslations("auth.login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<{
    code: string;
    message: string;
    lockedUntil?: string;
  } | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json();

      if (!response.ok) {
        const raw = (data as AuthError | { error: string }).error;
        if (typeof raw === "string") {
          setError({ code: raw, message: raw });
        } else {
          setError({
            code: raw.code,
            message: raw.message,
            lockedUntil: raw.lockedUntil,
          });
        }
        return;
      }

      const loginData = data as LoginResponse;
      window.location.href = loginData.dashboardUrl;
    } catch {
      setError({ code: "NETWORK_ERROR", message: t("errorNetwork") });
    } finally {
      setLoading(false);
    }
  }

  const inputStyle: React.CSSProperties = {
    width: "100%",
    background: "#FAFAF8",
    border: "1.5px solid #DDDDD8",
    borderRadius: 8,
    padding: "8px 12px",
    fontSize: 13,
    color: "#0A0A0A",
    outline: "none",
    fontFamily: "var(--font-main)",
    boxSizing: "border-box",
  };

  const labelStyle: React.CSSProperties = {
    fontFamily: "var(--font-mono)",
    fontSize: 11,
    fontWeight: 600,
    textTransform: "uppercase",
    letterSpacing: "0.06em",
    color: "#4A4A48",
    display: "block",
    marginBottom: 6,
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      {error && (
        <div
          role="alert"
          style={{
            background: "#FFE8E8",
            border: "1px solid #FFCCCC",
            borderRadius: 8,
            padding: "12px 16px",
          }}
        >
          <p style={{ fontSize: 13, color: "#CC2200", margin: 0 }}>
            {error.code === "ACCOUNT_LOCKED" && error.lockedUntil
              ? t("errorAccountLocked", { date: new Date(error.lockedUntil).toLocaleString() })
              : error.code === "EMAIL_NOT_VERIFIED"
                ? t("errorEmailNotVerified")
                : error.code === "ACCOUNT_SETUP_PENDING"
                  ? t("errorAccountSetupPending")
                  : error.message}
          </p>
          {error.code === "ACCOUNT_SETUP_PENDING" && (
            <p style={{ marginTop: 8, fontSize: 13, color: "#CC2200", margin: "8px 0 0" }}>
              {t("accountSetupPendingResend")}{" "}
              <a href="/setup-account" style={{ color: "#0A0A0A", textDecoration: "underline" }}>
                {t("accountSetupPendingLink")}
              </a>
            </p>
          )}
        </div>
      )}

      <div>
        <label htmlFor="email" style={labelStyle}>
          {t("emailLabel")}
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          placeholder={t("emailPlaceholder")}
          style={inputStyle}
          onFocus={(e) => { e.currentTarget.style.borderColor = "#CAFF4D"; }}
          onBlur={(e) => { e.currentTarget.style.borderColor = "#DDDDD8"; }}
        />
      </div>

      <div>
        <label htmlFor="password" style={labelStyle}>
          {t("passwordLabel")}
        </label>
        <div style={{ position: "relative" }}>
          <input
            id="password"
            type={showPassword ? "text" : "password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={{ ...inputStyle, paddingRight: 40 }}
            onFocus={(e) => { e.currentTarget.style.borderColor = "#CAFF4D"; }}
            onBlur={(e) => { e.currentTarget.style.borderColor = "#DDDDD8"; }}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            style={{
              position: "absolute",
              top: 0,
              right: 0,
              bottom: 0,
              display: "flex",
              alignItems: "center",
              paddingRight: 12,
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "#9A9A98",
            }}
            onMouseEnter={(e) => { e.currentTarget.style.color = "#4A4A48"; }}
            onMouseLeave={(e) => { e.currentTarget.style.color = "#9A9A98"; }}
            aria-label={showPassword ? t("hidePassword") : t("showPassword")}
          >
            {showPassword ? (
              <svg xmlns="http://www.w3.org/2000/svg" style={{ width: 20, height: 20 }} viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" style={{ width: 20, height: 20 }} viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
              </svg>
            )}
          </button>
        </div>
      </div>

      <div style={{ textAlign: "right", marginTop: -8 }}>
        <a
          href="/forgot-password"
          style={{ fontSize: 12, color: "#9A9A98", textDecoration: "none" }}
          onMouseEnter={(e) => { e.currentTarget.style.color = "#4A4A48"; }}
          onMouseLeave={(e) => { e.currentTarget.style.color = "#9A9A98"; }}
        >
          {t("forgotPassword")}
        </a>
      </div>

      <button
        type="submit"
        disabled={loading}
        style={{
          width: "100%",
          background: "#0A0A0A",
          color: "#FAFAF8",
          borderRadius: 10,
          padding: "13px 0",
          fontSize: 14,
          fontWeight: 600,
          border: "none",
          cursor: loading ? "not-allowed" : "pointer",
          marginTop: 8,
          fontFamily: "var(--font-main)",
          opacity: loading ? 0.6 : 1,
        }}
      >
        {loading ? t("submitting") : t("submit")}
      </button>
    </form>
  );
}
```

- [ ] **Step 2: Run TypeScript check**

```bash
cd web && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add web/src/components/auth/LoginForm.tsx
git commit -m "feat(login): restyle LoginForm with design system tokens"
```

---

## Task 3: Replace login/page.tsx with two-panel layout

**Files:**
- Modify: `web/src/app/(auth)/login/page.tsx`

This is a server component. It renders the full split layout and embeds `<LoginForm />` in the right panel. Left panel is hidden on mobile via Tailwind `hidden md:flex`. Right panel uses Tailwind flex utilities for centering; inline styles for colors and sizing.

- [ ] **Step 1: Replace the full file content**

```tsx
import { getTranslations } from "next-intl/server";
import LoginForm from "@/components/auth/LoginForm";

export default async function LoginPage() {
  const t = await getTranslations("loginPage");

  return (
    <div style={{ display: "flex", minHeight: "100vh", fontFamily: "var(--font-main)" }}>

      {/* LEFT PANEL — hidden on mobile */}
      <div
        className="hidden md:flex"
        style={{
          width: "45%",
          background: "#0A0A0A",
          flexDirection: "column",
          justifyContent: "space-between",
          padding: "48px 56px",
        }}
      >
        {/* TOP: logo lockup */}
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <svg width="28" height="28" viewBox="0 0 40 40" fill="none">
            <rect x="6" y="6" width="6" height="28" fill="#CAFF4D" />
            <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9" />
            <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6" />
          </svg>
          <span style={{ fontSize: 20, fontWeight: 800, color: "#FAFAF8", letterSpacing: "-0.03em" }}>
            klasio
          </span>
        </div>

        {/* MIDDLE: hero copy */}
        <div>
          <p style={{
            fontFamily: "var(--font-mono)",
            fontSize: 10,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
            color: "#4A4A48",
            marginBottom: 24,
          }}>
            {t("tagline")}
          </p>
          <h2 style={{
            fontSize: 40,
            fontWeight: 800,
            color: "#FAFAF8",
            letterSpacing: "-0.03em",
            lineHeight: 1.1,
            marginBottom: 20,
          }}>
            {t("heroLine1")}<br />
            <span style={{ color: "#CAFF4D" }}>{t("heroAccent")}</span>{t("heroLine2")}<br />
            {t("heroLine3")}
          </h2>
          <p style={{ fontSize: 14, color: "#4A4A48", lineHeight: 1.7, maxWidth: 280 }}>
            {t("heroBody")}
          </p>
        </div>

        {/* BOTTOM: tag pills */}
        <div style={{ display: "flex", gap: 16 }}>
          {[t("tag1"), t("tag2"), t("tag3")].map((tag) => (
            <span
              key={tag}
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 10,
                letterSpacing: "0.08em",
                color: "#4A4A48",
                border: "1px solid #2A2A2A",
                borderRadius: 6,
                padding: "4px 10px",
              }}
            >
              {tag}
            </span>
          ))}
        </div>
      </div>

      {/* RIGHT PANEL */}
      <div
        className="flex-1 flex items-center justify-center"
        style={{ background: "#F4F4F2", padding: "40px 24px" }}
      >
        <div style={{ width: "100%", maxWidth: 360 }}>
          <h1 style={{
            fontSize: 26,
            fontWeight: 800,
            color: "#0A0A0A",
            letterSpacing: "-0.02em",
            marginBottom: 6,
          }}>
            {t("title")}
          </h1>
          <p style={{ fontSize: 13, color: "#9A9A98", marginBottom: 36 }}>
            {t("subtitle")}
          </p>
          <LoginForm />
        </div>
      </div>

    </div>
  );
}
```

- [ ] **Step 2: Run TypeScript check**

```bash
cd web && npx tsc --noEmit
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add web/src/app/\(auth\)/login/page.tsx
git commit -m "feat(login): implement split-panel login page layout"
```

---

## Task 4: Manual Verification

Start the dev server and verify all 9 checklist items.

- [ ] **Step 1: Start dev server**

```bash
cd web && npm run dev
```

Open `http://localhost:3000/login` in a browser.

- [ ] **Step 2: Verify layout**

| # | Check | Pass? |
|---|---|---|
| 1 | Two-panel layout visible on desktop — dark left, light right | |
| 2 | Left: KLogoMark SVG + "klasio" wordmark at top | |
| 3 | Left: tagline label ("Gestión deportiva"), headline with lime "liga", body text, 3 tag pills | |
| 4 | Right: "Iniciar sesión" h1 + subtitle + form fields | |
| 5 | Email field focus → lime border `#CAFF4D` (click the email input) | |
| 6 | Password show/hide toggle works (click the eye icon) | |
| 7 | Submit with correct credentials → redirects to dashboard | |
| 8 | Submit with wrong credentials → red alert block, text `#CC2200`, no red-50 classes, no indigo | |
| 9 | Resize browser to <768px → left panel disappears, right panel full width | |
