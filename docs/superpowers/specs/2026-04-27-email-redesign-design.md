# Klasio Email Redesign — Design Spec

*Date: 2026-04-27 · Branch: feature/full-redesign*

## Overview

Replace HTML content of all 9 transactional email templates with a new design system. Backend (Spring Boot + Thymeleaf + Brevo) unchanged. Only `.html` template files are modified. All Thymeleaf expressions preserved exactly.

---

## 1. Scope

Files to modify (HTML only — `.txt` variants untouched):

| # | File | EmailType |
|---|------|-----------|
| 01 | `account-setup.html` | `ACCOUNT_SETUP` |
| 02 | `professor-invitation.html` | `PROFESSOR_INVITATION` |
| 03 | `password-recovery.html` | `PASSWORD_RECOVERY` |
| 04 | `payment-proof-uploaded.html` | `PAYMENT_PROOF_UPLOADED` |
| 05 | `payment-rejected.html` | `PAYMENT_REJECTED` |
| 06 | `membership-activated.html` | `MEMBERSHIP_ACTIVATED` |
| 07 | `membership-expiry-warning.html` | `MEMBERSHIP_EXPIRY_WARNING` |
| 08 | `membership-depleted.html` | `MEMBERSHIP_DEPLETED` |
| 09 | `class-session-change.html` | `CLASS_SESSION_CHANGE` |

Also: `missing-template-fallback.html`

**No shared layout fragment.** Each template is self-contained. `layouts/base.html` exists on disk but is not used by any template; it stays untouched.

---

## 2. Design System

### 2.1 Typography

```
Primary: DM Sans (Google Fonts)
Mono:    DM Mono (Google Fonts)
```

```html
<link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
```

Full fallback stack on every font declaration: `'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif`

### 2.2 Color Tokens (inline values — no CSS variables)

| Token | Value | Usage |
|-------|-------|-------|
| `color-bg-page` | `#F4F4F2` | `<body>` background |
| `color-bg-card` | `#FFFFFF` | Card background |
| `color-header-bg` | `#0A0A0A` | Header band |
| `color-accent` | `#CAFF4D` | Accent bar, CTA text, logo fill |
| `color-text-primary` | `#0A0A0A` | Headings, strong values |
| `color-text-body` | `#3A3A38` | Paragraph copy |
| `color-text-muted` | `#8A8A88` | Labels, secondary info |
| `color-text-footer` | `#AAAAAA` | Footer text |
| `color-border` | `#F0F0EE` | Dividers, row separators |
| `color-cta-bg` | `#0A0A0A` | CTA button background |
| `color-cta-text` | `#CAFF4D` | CTA button label |

**Category info panel colors:**

| Category | Panel bg | Border | Label color |
|----------|----------|--------|-------------|
| Auth / neutral | `#F8F8F6` | `#E8E8E6` | `#6A6A68` |
| Payment success | `#F4FAF6` | `#BDE8CB` | `#2A7A4A` |
| Payment rejection | `#FFF5F5` | `#FFD1D1` | `#C43030` |
| Membership warning | `#FFFAF0` | `#FFE4A8` | `#8A6000` |
| Class / schedule | `#F8F5FF` | `#DBC8FF` | `#7040B0` |

### 2.3 Spacing

| Slot | Value |
|------|-------|
| Body section padding | `32px 40px` |
| Header padding | `28px 40px` |
| Footer padding | `20px 40px 28px` |
| CTA button padding | `13px 28px` |
| Info row padding | `10px 18px` |

### 2.4 Border Radius

| Element | Radius |
|---------|--------|
| Outer card | `16px` |
| CTA button | `8px` |
| Info panel | `10px` |
| Warning callout | `8px` |
| Logo mark | `8px` |

---

## 3. Shell Structure (all templates)

```
┌──────────────────────────────────────┐  max-width: 600px
│  HEADER (#0A0A0A)   28px 40px        │
│  [hexagon logo #CAFF4D]  Klasio      │
├──────────────────────────────────────┤
│  ACCENT BAR  3px  #CAFF4D → fade     │
├──────────────────────────────────────┤
│  BODY   32px 40px                    │
│  greeting (if applicable)            │
│  body copy                           │
│  info panel (if applicable)          │
│  CTA button                          │
│  expiry / note (if applicable)       │
├──────────────────────────────────────┤
│  FOOTER   20px 40px 28px             │
│  #{layout.footer(${tenantName})}     │
│  © 2026 Klasio · …                   │
└──────────────────────────────────────┘
```

Outer wrapper uses `<table role="presentation">` for Outlook compatibility. Inner body content uses `<div>` layout. No `<style>` blocks — all styles inline.

---

## 4. Shared Primitives

**Greeting:**
```html
<p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
  <span th:text="#{templateKey.hi}">Hi</span>,
  <strong style="color:#0A0A0A;" th:text="${recipientVar}">User</strong>
</p>
```

**Body copy:**
```html
<p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
   th:text="#{templateKey.body(${var})}">Copy.</p>
```

**CTA button:**
```html
<div style="margin:28px 0;">
  <a th:href="${ctaUrl}"
     style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
     th:text="#{templateKey.cta}">CTA</a>
</div>
```

**Expiry note:**
```html
<p style="margin:12px 0 0;font-size:13px;color:#8A8A88;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
   th:text="#{templateKey.expiry(${expiresAt})}">Link expires in 30 min.</p>
```

**Info panel** (replace `PANEL_BG` / `PANEL_BORDER` per category):
```html
<div style="background:PANEL_BG;border:1px solid PANEL_BORDER;border-radius:10px;padding:4px 0;margin-bottom:20px;">
  <div style="display:flex;gap:8px;padding:10px 18px;border-bottom:1px solid #F0F0EE;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
    <span style="color:#8A8A88;min-width:130px;">Label</span>
    <span style="color:#1A1A1A;font-weight:500;" th:text="${variable}">Value</span>
  </div>
  <!-- Last row: no border-bottom -->
  <div style="display:flex;gap:8px;padding:10px 18px;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
    <span style="color:#8A8A88;min-width:130px;">Label</span>
    <span style="color:#1A1A1A;font-weight:500;" th:text="${variable}">Value</span>
  </div>
</div>
```

**Warning callout** (replace color values per category):
```html
<div style="background:WARN_BG;border:1px solid WARN_BORDER;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
  <span style="display:block;font-size:12px;font-weight:700;color:WARN_LABEL_COLOR;letter-spacing:0.08em;text-transform:uppercase;font-family:'DM Mono',monospace;margin-bottom:6px;"
        th:text="#{templateKey.label}">LABEL</span>
  <p style="margin:0;font-size:14px;color:WARN_BODY_COLOR;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
     th:text="${variable}">Body text.</p>
</div>
```

---

## 5. Per-Template Body Spec

### 5.1 `account-setup.html`
Variables: `recipientName`, `role`, `tenantName`, `setupUrl`, `expiresAt`

1. Greeting → `recipientName`
2. Body copy → `#{accountSetup.body(${role}, ${tenantName})}`
3. CTA → `${setupUrl}` / `#{accountSetup.cta}`
4. Warning callout (bg `#FFF8F0`, border `#FFD9A8`, label color `#7A4000`):
   - Label: hardcoded `"⏱ Enlace temporal"` (not i18n)
   - Body: `#{accountSetup.expiry(${expiresAt})}`

### 5.2 `professor-invitation.html`
Variables: `professorName`, `tenantName`, `activationUrl`, `expiresAt`

1. Greeting → `professorName`
2. Body copy → `#{professorInvitation.body(${tenantName})}`
3. CTA → `${activationUrl}` / `#{professorInvitation.cta}`
4. Expiry note → `#{professorInvitation.expiry(${expiresAt})}`

### 5.3 `password-recovery.html`
Variables: `tenantName`, `resetUrl`, `expiresAt` — **no greeting**

1. Body copy → `#{passwordRecovery.intro}`
2. CTA → `${resetUrl}` / `#{passwordRecovery.cta}`
3. Expiry note → `#{passwordRecovery.expiry(${expiresAt})}`
4. No-request note → `#{passwordRecovery.noRequest}`

### 5.4 `payment-proof-uploaded.html`
Variables: `studentName`, `programName`, `tenantName`, `reviewUrl` — **no greeting**
Panel: neutral (`#F8F8F6` / `#E8E8E6`)

1. Body copy → `#{paymentProofUploaded.body(${studentName}, ${programName})}`
2. Info panel (neutral): `"Student"` → `${studentName}`, `"Program"` → `${programName}` (labels hardcoded)
3. CTA → `${reviewUrl}` / `#{paymentProofUploaded.cta}`

### 5.5 `payment-rejected.html`
Variables: `studentName`, `programName`, `reason`, `tenantName`, `retryUrl`
Callout: rejection red (`#FFF5F5` / `#FFD1D1`, label `#C43030`, body `#5A1A1A`)

1. Greeting → `studentName`
2. Body copy → `#{paymentRejected.body(${programName})}`
3. Warning callout: label `#{paymentRejected.reason}` (ALL-CAPS via CSS), body `${reason}`
4. CTA → `${retryUrl}` / `#{paymentRejected.cta}`

### 5.6 `membership-activated.html`
Variables: `studentName`, `programName`, `planName`, `totalHours`, `expiresAt`, `tenantName`, `loginUrl`
Panel: success green (`#F4FAF6` / `#BDE8CB`)

1. Greeting → `studentName`
2. Body copy → `#{membershipActivated.body(${programName})}`
3. Info panel (green): `"Plan"` → `${planName}`, `"Available hours"` → `${totalHours}`, `"Expires on"` → `${expiresAt}` (labels hardcoded)
4. CTA → `${loginUrl}` / `#{membershipActivated.cta}`

> `loginUrl` is injected globally by `EmailDispatcherService` — not in `EmailType.requiredKeys()` but always available.

### 5.7 `membership-expiry-warning.html`
Variables: `studentName`, `programName`, `remainingHours`, `expiresAt`, `tenantName`, `loginUrl`
Panel: amber (`#FFFAF0` / `#FFE4A8`)

1. Greeting → `studentName`
2. Body copy → `#{membershipExpiryWarning.body(${programName})}`
3. Info panel (amber): `"Remaining hours"` → `${remainingHours}`, `"Expires on"` → `${expiresAt}` (labels hardcoded)
4. CTA → `${loginUrl}` / `#{membershipExpiryWarning.cta}`

### 5.8 `membership-depleted.html`
Variables: `studentName`, `programName`, `tenantName`, `loginUrl` — **no info panel**

1. Greeting → `studentName`
2. Body copy → `#{membershipDepleted.body(${programName})}`
3. CTA → `${loginUrl}` / `#{membershipDepleted.cta}`

### 5.9 `class-session-change.html`
Variables: `studentName`, `className`, `startsAt`, `changeKind`, `reason` (nullable), `tenantName`, `loginUrl`
Panel: purple (`#F8F5FF` / `#DBC8FF`)

1. Greeting → `studentName`
2. Body copy → `#{classSessionChange.body(${className}, ${startsAt})}`
3. Info panel (purple):
   - Row 1: `"Change type"` → `${changeKind}` (raw variable, not `#{classSessionChange.changeKind(...)}`)
   - Row 2 (`th:if="${reason != null and !reason.isEmpty()}"`, no `border-bottom`): `"Reason"` → `${reason}`
4. CTA → `${loginUrl}` / `#{classSessionChange.cta}`

### 5.10 `missing-template-fallback.html`
Variables: `tenantName`, `emailTypeName` — no i18n keys

Body:
```html
<p>You have a new notification from <strong th:text="${tenantName}">Academy</strong>.</p>
<p style="font-family:'DM Mono',monospace;font-size:12px;color:#8A8A88;">
  (Template not configured: <span th:text="${emailTypeName}"></span>)
</p>
```

---

## 6. Constraints

- No `<style>` blocks — Gmail strips them. All styles inline.
- `<table role="presentation">` for outer skeleton (Outlook compatibility).
- No CSS custom properties. No CSS Grid/Flexbox in table cells.
- `display:flex` on info row `<div>`s is acceptable (degrades to block in Outlook).
- `border-radius` on outer `<table>` won't render in Outlook — acceptable.
- `linear-gradient` on accent bar won't render in Outlook — acceptable, no `mso-` fallback needed.
- No dark mode media queries.
- No new files. No shared layout fragment.
- Do not add, rename, or delete any `#{...}` message keys in `.properties` files.
- `th:fragment="subject"` in `<head>` must be preserved exactly on every template.

---

## 7. Key Findings from Code Exploration

- `layouts/base.html` exists but no template imports it — all are self-contained. Leave untouched.
- `tenantName`, `tenantSlug`, `loginUrl` injected globally by `EmailDispatcherService.java:69-71` — always available in every template regardless of `EmailType.requiredKeys()`.
- `ThymeleafTemplateRenderer` processes `Set.of("subject")` fragment to extract subject line — `th:fragment="subject"` in `<head>` is critical.
- Info panel labels for membership/class templates are hardcoded (not i18n) because the existing i18n keys embed label + value together via `{0}` substitution. Adding new label-only keys is out of scope — hardcoded labels are the correct approach.

---

## 8. Verification Checklist

- [ ] All 10 templates render without Thymeleaf errors
- [ ] Subject fragment extracted correctly (`Set.of("subject")` processing)
- [ ] No missing i18n key errors in `messages_en.properties` / `messages_es.properties`
- [ ] `th:if` on `reason` in `class-session-change.html` still functional
- [ ] CTA `<a>` tags have correct `th:href` per `EmailType.requiredKeys()`
- [ ] Outer card has `max-width: 600px`
- [ ] All `font-family` declarations include full system-font fallback stack
- [ ] No `<style>` blocks with selectors
- [ ] `${emailTypeName}` visible in fallback template
