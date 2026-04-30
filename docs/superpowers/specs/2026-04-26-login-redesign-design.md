# Login Page Redesign — Design Spec

**Date:** 2026-04-26  
**Branch:** feature/full-redesign  
**Scope:** Visual-only. Zero auth logic changes.

---

## Overview

Replace the current centered-card login layout with a full-screen horizontal split. Dark left panel with brand hero copy; light right panel with the form. Mobile: left panel hidden, right panel full-width.

---

## Files Changed

| File | Nature of change |
|---|---|
| `web/src/app/(auth)/login/page.tsx` | Full replace — two-panel layout, inline SVG logo |
| `web/src/components/auth/LoginForm.tsx` | Style-only — inline label+input, custom button, error styles |
| `web/messages/es.json` | Update `loginPage.title` + `loginPage.subtitle`; add 9 new keys |
| `web/messages/en.json` | Same as es.json |

---

## Left Panel

- **Width:** 45% on ≥768px; hidden on <768px
- **Background:** `#0A0A0A`
- **Layout:** `flex-col`, `justify-content: space-between`, padding `48px 56px`

### Top — Logo lockup

Inline SVG `KLogoMark` (28×28, viewBox 0 0 40 40):

```svg
<svg width="28" height="28" viewBox="0 0 40 40" fill="none">
  <rect x="6" y="6" width="6" height="28" fill="#CAFF4D" />
  <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9" />
  <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6" />
</svg>
```

Wordmark: `"klasio"`, `var(--font-main)`, 20px, weight 800, `#FAFAF8`, tracking `-0.03em`. Row with gap 12px.

### Middle — Hero copy

**Tagline label:** `var(--font-mono)`, 10px, uppercase, tracking `0.12em`, `#4A4A48`, mb 24px. Text: `t("tagline")`.

**Headline:** 40px, weight 800, `#FAFAF8`, tracking `-0.03em`, line-height 1.1, mb 20px.
```
{t("heroLine1")}
<span #CAFF4D>{t("heroAccent")}</span>{t("heroLine2")}
{t("heroLine3")}
```

**Body:** 14px, `#4A4A48`, line-height 1.7, max-width 280px. Text: `t("heroBody")`.

### Bottom — Tag pills

Three pills: `t("tag1")`, `t("tag2")`, `t("tag3")`. Row, gap 16px.  
Style: `var(--font-mono)`, 10px, tracking `0.08em`, `#4A4A48`, border `1px solid #2A2A2A`, border-radius 6px, padding `4px 10px`.

---

## Right Panel

- **Width:** `flex: 1` (≥768px); full width on mobile
- **Background:** `#F4F4F2`
- **Layout:** centered (items-center + justify-center)
- **Content max-width:** 360px

**h1:** `t("title")`, 26px, weight 800, `#0A0A0A`, tracking `-0.02em`, mb 6px.  
**Subtitle p:** `t("subtitle")`, 13px, `#9A9A98`, mb 36px.

Embeds `<LoginForm />` with no changes to its props or logic.

---

## LoginForm — Style Changes Only

All logic preserved: `handleSubmit`, error state, loading state, show/hide password toggle, `useTranslations("auth.login")`.

### Error alert

Replace Tailwind classes with inline styles:
- Container: `background #FFE8E8`, `border: 1px solid #FFCCCC`, `borderRadius: 8`, `padding: 12px 16px`, `marginBottom: 16`
- Text: `fontSize: 13`, `color: #CC2200`
- Setup-account link: `color: #0A0A0A`, `textDecoration: underline` (remove `text-indigo-700`)

All error code branches (`ACCOUNT_LOCKED`, `EMAIL_NOT_VERIFIED`, `ACCOUNT_SETUP_PENDING`) unchanged.

### Email field

Replace `<Input label=... />` with manual label + input:

**Label:** `var(--font-mono)`, 11px, weight 600, uppercase, tracking `0.06em`, `#4A4A48`, `display: block`, `marginBottom: 6`.  
**Input:** `width: 100%`, `background: #FAFAF8`, `border: 1.5px solid #DDDDD8`, `borderRadius: 8`, `padding: 8px 12px`, `fontSize: 13`, `color: #0A0A0A`, `outline: none`, `var(--font-main)`.  
Focus: `borderColor: #CAFF4D`. Blur: `borderColor: #DDDDD8` (managed via `onFocus`/`onBlur` on state or inline handler).

### Password field

Same label + input styles as email. Show/hide toggle button kept as-is; button color `#9A9A98`, hover `#4A4A48` (remove indigo classes).

### Forgot password link

`fontSize: 12`, `color: #9A9A98`, hover `#4A4A48`. No underline by default. Remove `text-indigo-600` / `hover:text-indigo-500`.

### Submit button

Replace `<Button variant="volt">` with plain `<button>`:
- `width: 100%`, `background: #0A0A0A`, `color: #FAFAF8`, `borderRadius: 10`, `padding: 13px 0`, `fontSize: 14`, `fontWeight: 600`, `border: none`, `cursor: pointer`, `marginTop: 8`, `var(--font-main)`
- Disabled: `opacity: 0.6`, `cursor: not-allowed`
- Loading text: `t("submitting")` (unchanged)

---

## i18n Keys

### loginPage (es.json updates)

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
}
```

### loginPage (en.json updates)

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
}
```

---

## Responsive

| Breakpoint | Left panel | Right panel |
|---|---|---|
| ≥768px | visible, 45% width | flex-1 |
| <768px | hidden (`hidden md:flex`) | full width, padding `40px 24px` |

Implementation: Tailwind `hidden md:flex` on left panel div.

---

## Verification Checklist

1. `npx tsc --noEmit` — zero errors
2. `/login` renders two-panel layout (dark left, light right)
3. Left panel: logo SVG + wordmark, headline with lime `#CAFF4D` accent, body text, 3 tag pills
4. Right panel: "Iniciar sesión" h1, subtitle, form fields
5. Email field focus → lime border `#CAFF4D`
6. Password show/hide toggle works
7. Submit → redirects to dashboard on success
8. Wrong credentials → error alert with `#CC2200` text (no `red-50`, no indigo)
9. Mobile (<768px) → only right panel shown, full width

---

## Out of Scope

- No new components created
- No changes to `<Input>` or `<Button>` primitives
- No auth logic, routing, or API changes
- No other auth pages touched (forgot-password, reset-password, setup-account)
