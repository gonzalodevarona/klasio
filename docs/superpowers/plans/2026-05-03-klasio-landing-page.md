# Klasio Marketing Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single self-contained HTML marketing landing page for klasio.club at `klasio-landing/index.html` — 8 sections, EN-canonical with auto-Spanish via `navigator.language`, all WhatsApp links built from a single runtime constant.

**Architecture:** One HTML file. All CSS in one `<style>` tag in `<head>`. All JS in one `<script>` tag before `</body>`. Vanilla JS (~80 lines: i18n + IntersectionObserver + WhatsApp link wiring). Google Fonts CDN as the only external dependency. No build step.

**Tech Stack:** HTML5, CSS3 (custom properties, grid, clamp, backdrop-filter), Vanilla JS (IntersectionObserver, querySelectorAll), Google Fonts (DM Sans + DM Mono).

**Reference spec:** `docs/superpowers/specs/2026-05-03-klasio-landing-page-design.md` — pull i18n key copy + WA_MESSAGES map from there.

---

## File Structure

```
klasio-landing/
└── index.html        ← single file, ~1500 lines
```

**`index.html` internal structure:**

```
<head>
  <meta charset, viewport, description, og:tags>
  <link rel="canonical">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?...">
  <style>
    :root { --volt, --dark, ...; }
    /* Reset / base */
    /* Components: .btn-volt, .btn-outline, .btn-ghost, .tag-pill, .section-title, .fade-up */
    /* Sections: nav, hero, value, features, how-it-works, metrics, final-cta, footer */
    /* Decorative: .noise-overlay, .volt-glow, .wa-fab */
    /* Animations: @keyframes pulse */
    /* Media queries: <900px, <768px, <560px, prefers-reduced-motion */
  </style>
</head>
<body>
  <nav>...</nav>
  <section id="hero" class="hero">...</section>
  <section id="valor" class="value">...</section>
  <section id="funcionalidades" class="features">...</section>
  <section id="como-funciona" class="how-it-works">...</section>
  <section id="metricas" class="metrics">...</section>
  <section id="cta" class="final-cta">...</section>
  <footer>...</footer>
  <a class="wa-fab" data-wa-msg="fab" aria-label="...">...</a>

  <script>
    // 1. KLASIO_WHATSAPP_NUMBER + KLASIO_CONFIG override
    // 2. I18N dictionary (en + es)
    // 3. WA_MESSAGES map
    // 4. detectLang() → applyTranslations() + wireWhatsApp()
    // 5. IntersectionObserver for .fade-up
  </script>
</body>
```

---

## Reference: Design Tokens

Defined verbatim from spec. Used throughout.

```css
:root {
  --volt: #CAFF4D;
  --volt-dark: #2A4A00;
  --dark: #0A0A0A;
  --surface: #111111;
  --surface2: #181818;
  --border: #1E1E1E;
  --border2: #2A2A2A;
  --muted: #4A4A48;
  --subtle: #6A6A68;
  --text: #FAFAF8;
}
```

KLogoMark SVG (used in nav, dashboard sidebar, footer):

```html
<svg width="26" height="26" viewBox="0 0 40 40" fill="none" aria-hidden="true">
  <rect x="6" y="6" width="6" height="28" fill="#CAFF4D"/>
  <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9"/>
  <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6"/>
</svg>
```

---

## Task 1: Scaffold the file + base CSS variables and reset

**Files:**
- Create: `klasio-landing/index.html`

- [ ] **Step 1: Create the directory and empty file**

```bash
mkdir -p klasio-landing
touch klasio-landing/index.html
```

- [ ] **Step 2: Write the full HTML scaffold with `<head>`, font links, CSS variables, base reset, and empty `<body>` + `<script>`**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Klasio — Sports club management software</title>
  <meta name="description" content="All-in-one platform to manage memberships, attendance, payments and programs for your sports club. No spreadsheets, no chaos.">
  <link rel="canonical" href="https://klasio.club">

  <meta property="og:type" content="website">
  <meta property="og:url" content="https://klasio.club">
  <meta property="og:title" content="Klasio — Sports club management software">
  <meta property="og:description" content="The professional way to run your sports club.">

  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,300;0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;0,9..40,800&family=DM+Mono:wght@400;500&display=swap">

  <style>
    :root {
      --volt: #CAFF4D;
      --volt-dark: #2A4A00;
      --dark: #0A0A0A;
      --surface: #111111;
      --surface2: #181818;
      --border: #1E1E1E;
      --border2: #2A2A2A;
      --muted: #4A4A48;
      --subtle: #6A6A68;
      --text: #FAFAF8;
    }

    *, *::before, *::after { box-sizing: border-box; }
    html { scroll-behavior: smooth; }
    body {
      margin: 0;
      background: var(--dark);
      color: var(--text);
      font-family: 'DM Sans', system-ui, -apple-system, sans-serif;
      -webkit-font-smoothing: antialiased;
      -moz-osx-font-smoothing: grayscale;
      overflow-x: hidden;
      line-height: 1.5;
    }
    a { color: inherit; text-decoration: none; }
    button { font-family: inherit; cursor: pointer; }
    img, svg { display: block; max-width: 100%; }
    h1, h2, h3, p { margin: 0; }

    /* Scrollbar */
    ::-webkit-scrollbar { width: 5px; }
    ::-webkit-scrollbar-track { background: transparent; }
    ::-webkit-scrollbar-thumb { background: #2A2A2A; border-radius: 99px; }
  </style>
</head>
<body>

  <script>
    // populated in later tasks
  </script>
</body>
</html>
```

- [ ] **Step 3: Verify file opens in browser**

Run: `open klasio-landing/index.html` (macOS) — page loads, dark background visible, no console errors.
Expected: blank dark page, font loads (visible in devtools Network tab), no errors.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): scaffold klasio-landing/index.html with base CSS"
```

---

## Task 2: Add CSS for component primitives

**Files:**
- Modify: `klasio-landing/index.html` — append to `<style>` block

- [ ] **Step 1: Append component primitives CSS inside `<style>` (after the scrollbar block)**

```css
    /* === Component primitives === */

    /* Buttons */
    .btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-weight: 700;
      font-size: 13px;
      border-radius: 10px;
      border: none;
      cursor: pointer;
      transition: all 0.15s ease;
      white-space: nowrap;
    }
    .btn-volt {
      background: var(--volt);
      color: var(--dark);
      padding: 13px 28px;
    }
    .btn-volt:hover {
      background: #B8EE3A;
      transform: translateY(-1px);
    }
    .btn-outline {
      background: transparent;
      color: var(--text);
      border: 1.5px solid var(--border2);
      padding: 12px 24px;
    }
    .btn-outline:hover {
      border-color: var(--volt);
      color: var(--volt);
    }
    .btn-ghost {
      background: transparent;
      color: var(--subtle);
      border: none;
      padding: 10px 16px;
      border-radius: 8px;
      font-weight: 600;
    }
    .btn-ghost:hover { color: var(--text); }

    /* Tag pill (e.g., "Why Klasio") */
    .tag {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-family: 'DM Mono', monospace;
      font-size: 10px;
      font-weight: 500;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: var(--volt);
      background: rgba(202,255,77,0.08);
      border: 1px solid rgba(202,255,77,0.2);
      padding: 6px 12px;
      border-radius: 6px;
    }
    .tag::before {
      content: '';
      width: 6px;
      height: 6px;
      background: var(--volt);
      border-radius: 50%;
      animation: pulse 2s infinite;
    }
    .tag.no-dot::before { display: none; }

    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(0.8); }
    }

    /* Section titles */
    .section-title {
      font-size: clamp(32px, 4vw, 52px);
      font-weight: 800;
      letter-spacing: -0.03em;
      line-height: 1.05;
      color: var(--text);
      margin-bottom: 16px;
    }
    .section-sub {
      font-size: 17px;
      color: var(--subtle);
      line-height: 1.7;
      max-width: 640px;
    }
    .section-header { margin-bottom: 64px; }

    /* Layout container */
    .container {
      max-width: 1120px;
      margin: 0 auto;
      width: 100%;
    }

    /* Logo lockup */
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }
    .logo .wordmark {
      font-family: 'DM Sans', sans-serif;
      font-weight: 800;
      font-size: 18px;
      letter-spacing: -0.03em;
      color: var(--text);
    }

    /* Volt accent text inside titles */
    .volt-text { color: var(--volt); }

    /* Fade-up base (animation triggered via JS — see Task 14) */
    .fade-up {
      opacity: 0;
      transform: translateY(24px);
      transition: opacity 0.6s ease, transform 0.6s ease;
    }
    .fade-up.visible {
      opacity: 1;
      transform: translateY(0);
    }
    .fade-up.d1 { transition-delay: 0.1s; }
    .fade-up.d2 { transition-delay: 0.2s; }
    .fade-up.d3 { transition-delay: 0.3s; }
    .fade-up.d4 { transition-delay: 0.4s; }
```

- [ ] **Step 2: Verify file still loads, no CSS syntax errors**

Reload browser. Open devtools Console — no errors. Open Elements tab — `:root` variables present. Page still blank but CSS valid.

- [ ] **Step 3: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add CSS primitives (buttons, tag, titles, fade-up)"
```

---

## Task 3: Build the fixed nav

**Files:**
- Modify: `klasio-landing/index.html` — append nav CSS to `<style>`, add `<nav>` to `<body>`

- [ ] **Step 1: Append nav CSS inside `<style>`**

```css
    /* === Nav === */
    .nav {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      height: 64px;
      padding: 0 32px;
      background: rgba(10,10,10,0.85);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-bottom: 1px solid var(--border);
      z-index: 100;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .nav-link {
      font-size: 14px;
      color: var(--subtle);
      padding: 8px 14px;
      border-radius: 8px;
      transition: all 0.15s ease;
    }
    .nav-link:hover {
      color: var(--text);
      background: rgba(255,255,255,0.03);
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    /* Smaller nav button variants */
    .nav-actions .btn-outline { padding: 9px 18px; font-size: 13px; }
    .nav-actions .btn-volt { padding: 10px 20px; font-size: 13px; }

    @media (max-width: 768px) {
      .nav-links { display: none; }
      .nav { padding: 0 20px; }
      .nav-actions .btn-outline { padding: 8px 14px; font-size: 12px; }
      .nav-actions .btn-volt { padding: 9px 16px; font-size: 12px; }
    }
```

- [ ] **Step 2: Add the `<nav>` element as the first child of `<body>`**

```html
  <nav class="nav" aria-label="Primary">
    <a href="#" class="logo" aria-label="Klasio home">
      <svg width="26" height="26" viewBox="0 0 40 40" fill="none" aria-hidden="true">
        <rect x="6" y="6" width="6" height="28" fill="#CAFF4D"/>
        <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9"/>
        <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6"/>
      </svg>
      <span class="wordmark">klasio</span>
    </a>
    <div class="nav-links">
      <a href="#valor" class="nav-link" data-i18n="nav.benefits">Benefits</a>
      <a href="#funcionalidades" class="nav-link" data-i18n="nav.features">Features</a>
      <a href="#como-funciona" class="nav-link" data-i18n="nav.how">How it works</a>
    </div>
    <div class="nav-actions">
      <a href="#" class="btn btn-outline" data-wa-msg="nav_contact" data-i18n="nav.contact">Contact</a>
      <a href="#" class="btn btn-volt" data-wa-msg="nav_demo" data-i18n="nav.demo">See demo →</a>
    </div>
  </nav>
```

- [ ] **Step 3: Verify in browser at desktop + mobile**

Reload. Verify:
- Nav fixed top, blurred translucent background, 64px tall.
- Logo + 3 center links + 2 right buttons visible at ≥768px.
- Resize to <768px → center links disappear, logo + buttons remain.
- Hover states work (links subtle bg, buttons color shifts).

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add fixed nav with logo, links and CTAs"
```

---

## Task 4: Build the hero left column (copy)

**Files:**
- Modify: `klasio-landing/index.html` — append hero CSS, add `<section id="hero">`

- [ ] **Step 1: Append hero CSS inside `<style>`**

```css
    /* === Hero === */
    .hero {
      position: relative;
      min-height: 100vh;
      padding: 120px 32px 80px;
      overflow: hidden;
    }
    .hero-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 80px;
      align-items: center;
      max-width: 1120px;
      margin: 0 auto;
      position: relative;
      z-index: 2;
    }
    .hero h1 {
      font-size: clamp(42px, 5.5vw, 72px);
      font-weight: 800;
      letter-spacing: -0.035em;
      line-height: 1.0;
      margin: 28px 0 24px;
      color: var(--text);
    }
    .hero h1 .line { display: block; }
    .hero-subtitle {
      font-size: 17px;
      color: var(--subtle);
      line-height: 1.7;
      max-width: 520px;
      margin-bottom: 40px;
    }
    .hero-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 52px;
      flex-wrap: wrap;
    }
    .hero-stats {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .hero-stat {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .hero-stat-value {
      font-size: 22px;
      font-weight: 800;
      letter-spacing: -0.03em;
      color: var(--text);
      line-height: 1;
    }
    .hero-stat-label {
      font-family: 'DM Mono', monospace;
      font-size: 10px;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--muted);
    }
    .hero-stat-divider {
      width: 1px;
      height: 36px;
      background: var(--border2);
    }

    @media (max-width: 900px) {
      .hero-grid { grid-template-columns: 1fr; }
      .hero { padding: 100px 24px 60px; }
    }
```

- [ ] **Step 2: Add the `<section>` after the `<nav>` (right column placeholder for now)**

```html
  <section id="hero" class="hero">
    <div class="hero-grid">
      <div class="hero-copy">
        <span class="tag fade-up" data-i18n="hero.tag">Professional sports management</span>
        <h1 class="fade-up d1">
          <span class="line" data-i18n="hero.h1.line1">Your club,</span>
          <span class="line" data-i18n="hero.h1.line2">managed</span>
          <span class="line" data-i18n-html="hero.h1.line3">with <span class="volt-text">precision.</span></span>
        </h1>
        <p class="hero-subtitle fade-up d2" data-i18n="hero.subtitle">Klasio is the all-in-one platform for sports clubs. Manage memberships, attendance, payments and programs from a single place — no spreadsheets, no chaos.</p>
        <div class="hero-actions fade-up d3">
          <a href="#" class="btn btn-volt" data-wa-msg="hero_primary" data-i18n="hero.cta.primary">Request free demo →</a>
          <a href="#funcionalidades" class="btn btn-ghost" data-i18n="hero.cta.secondary">See features ↓</a>
        </div>
        <div class="hero-stats fade-up d4">
          <div class="hero-stat">
            <span class="hero-stat-value" data-i18n="hero.stat1.value">100%</span>
            <span class="hero-stat-label" data-i18n="hero.stat1.label">Digital</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value" data-i18n="hero.stat2.value">5</span>
            <span class="hero-stat-label" data-i18n="hero.stat2.label">User roles</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value" data-i18n="hero.stat3.value">∞</span>
            <span class="hero-stat-label" data-i18n="hero.stat3.label">Students</span>
          </div>
        </div>
      </div>
      <!-- Right column: dashboard mockup added in Task 5 -->
      <div class="hero-mockup-placeholder" aria-hidden="true"></div>
    </div>
  </section>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Hero copy visible: tag pill (with pulsing green dot), 3-line h1 ("precision." in volt green), subtitle, two buttons, stats row with vertical dividers.
- `.fade-up` elements still hidden (opacity 0 — JS not wired yet, this is expected).
- Temporarily remove `class="fade-up"` from one element to confirm it renders correctly.

**Important:** Re-add the `fade-up` classes after this manual sanity check.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add hero left column (copy, CTAs, stats)"
```

---

## Task 5: Build the hero right column (CSS dashboard mockup)

**Files:**
- Modify: `klasio-landing/index.html` — append mockup CSS, replace `.hero-mockup-placeholder` with the mockup HTML

- [ ] **Step 1: Append mockup CSS inside `<style>`**

```css
    /* === Hero dashboard mockup === */
    .hero-mockup {
      position: relative;
      background: #111;
      border-radius: 20px;
      border: 1px solid var(--border2);
      box-shadow: 0 40px 120px rgba(0,0,0,0.8), 0 0 0 1px rgba(255,255,255,0.04);
      overflow: hidden;
      min-height: 420px;
    }
    .hero-mockup::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(202,255,77,0.4), transparent);
    }
    .mockup-sidebar {
      position: absolute;
      top: 0; left: 0; bottom: 0;
      width: 52px;
      background: #0A0A0A;
      border-right: 1px solid var(--border);
      padding: 14px 10px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
    }
    .mockup-sidebar .mockup-logo { margin-bottom: 8px; }
    .mockup-nav-item {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }
    .mockup-nav-item .dot {
      width: 16px;
      height: 4px;
      background: #2A2A2A;
      border-radius: 2px;
    }
    .mockup-nav-item.active {
      background: #1A1A1A;
    }
    .mockup-nav-item.active::before {
      content: '';
      position: absolute;
      left: -7px;
      top: 4px;
      bottom: 4px;
      width: 3px;
      background: var(--volt);
      border-radius: 0 2px 2px 0;
    }
    .mockup-nav-item.active .dot {
      background: rgba(202,255,77,0.7);
    }

    .mockup-content {
      margin-left: 52px;
      padding: 20px 18px;
    }
    .mockup-header { margin-bottom: 16px; }
    .mockup-header .title {
      font-size: 14px;
      font-weight: 800;
      color: #FAFAF8;
      letter-spacing: -0.02em;
      margin-bottom: 4px;
    }
    .mockup-header .subtitle {
      font-family: 'DM Mono', monospace;
      font-size: 8px;
      color: #4A4A48;
      letter-spacing: 0.08em;
    }

    .mockup-kpis {
      display: flex;
      gap: 8px;
      margin-bottom: 14px;
    }
    .mockup-kpi {
      flex: 1;
      background: #0A0A0A;
      border-radius: 10px;
      padding: 12px 14px;
    }
    .mockup-kpi .label {
      font-family: 'DM Mono', monospace;
      font-size: 8px;
      color: #4A4A48;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      margin-bottom: 6px;
    }
    .mockup-kpi .value {
      font-size: 22px;
      font-weight: 800;
      letter-spacing: -0.03em;
      line-height: 1;
    }
    .mockup-kpi .sub {
      font-size: 9px;
      color: #2A8A00;
      margin-top: 4px;
    }

    .mockup-attendance .att-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;
    }
    .mockup-attendance .att-title {
      font-size: 12px;
      font-weight: 700;
      color: #FAFAF8;
    }
    .mockup-attendance .att-cta {
      font-size: 9px;
      font-weight: 700;
      background: var(--volt);
      color: var(--dark);
      padding: 4px 10px;
      border-radius: 6px;
    }
    .mockup-table {
      border-radius: 8px;
      border: 1px solid #1E1E1E;
      overflow: hidden;
    }
    .mockup-row {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 10px;
      border-bottom: 1px solid #151515;
    }
    .mockup-row:nth-child(odd) { background: #0D0D0D; }
    .mockup-row:nth-child(even) { background: #111111; }
    .mockup-row:last-child { border-bottom: none; }
    .mockup-row .name {
      flex: 1;
      font-size: 10px;
      font-weight: 600;
      color: #FAFAF8;
    }
    .mockup-row .program {
      flex: 1.2;
      font-size: 9px;
      color: #4A4A48;
    }
    .mockup-bar-wrap {
      flex: 1;
      height: 3px;
      background: #1E1E1E;
      border-radius: 99px;
      overflow: hidden;
    }
    .mockup-bar {
      height: 100%;
      border-radius: 99px;
    }
    .mockup-badge {
      font-size: 8px;
      font-weight: 700;
      padding: 2px 7px;
      border-radius: 99px;
      white-space: nowrap;
    }

    @media (max-width: 900px) {
      .hero-mockup { display: none; }
    }
```

- [ ] **Step 2: Replace the `<div class="hero-mockup-placeholder">` with the full mockup**

```html
      <div class="hero-mockup fade-up d2" aria-hidden="true">
        <aside class="mockup-sidebar">
          <svg class="mockup-logo" width="26" height="26" viewBox="0 0 40 40" fill="none">
            <rect x="6" y="6" width="6" height="28" fill="#CAFF4D"/>
            <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9"/>
            <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6"/>
          </svg>
          <div class="mockup-nav-item active"><span class="dot"></span></div>
          <div class="mockup-nav-item"><span class="dot"></span></div>
          <div class="mockup-nav-item"><span class="dot"></span></div>
          <div class="mockup-nav-item"><span class="dot"></span></div>
          <div class="mockup-nav-item"><span class="dot"></span></div>
        </aside>

        <div class="mockup-content">
          <div class="mockup-header">
            <div class="title" data-i18n="mock.dashboard">Dashboard</div>
            <div class="subtitle" data-i18n="mock.subtitle">NORTH LEAGUE · CURRENT PERIOD</div>
          </div>

          <div class="mockup-kpis">
            <div class="mockup-kpi">
              <div class="label" data-i18n="mock.kpi1.label">STUDENTS</div>
              <div class="value" style="color:#FAFAF8">248</div>
              <div class="sub" data-i18n="mock.kpi1.sub">↑ 12 this month</div>
            </div>
            <div class="mockup-kpi">
              <div class="label" data-i18n="mock.kpi2.label">HOURS USED</div>
              <div class="value" style="color:var(--volt)">1,840</div>
            </div>
            <div class="mockup-kpi">
              <div class="label" data-i18n="mock.kpi3.label">PENDING PAYMENTS</div>
              <div class="value" style="color:#FFC107">14</div>
            </div>
          </div>

          <div class="mockup-attendance">
            <div class="att-header">
              <div class="att-title" data-i18n="mock.attendance.title">Attendance control</div>
              <div class="att-cta" data-i18n="mock.attendance.cta">Start class</div>
            </div>
            <div class="mockup-table">
              <div class="mockup-row">
                <div class="name">Carlos Rodríguez</div>
                <div class="program" data-i18n="mock.row1.program">Advanced Swimming</div>
                <div class="mockup-bar-wrap"><div class="mockup-bar" style="width:100%; background:var(--volt);"></div></div>
                <div class="mockup-badge" style="background:var(--volt); color:var(--volt-dark);" data-i18n="mock.row1.status">Active</div>
              </div>
              <div class="mockup-row">
                <div class="name">Ana Martínez</div>
                <div class="program" data-i18n="mock.row2.program">Youth Karate</div>
                <div class="mockup-bar-wrap"><div class="mockup-bar" style="width:66%; background:#FFC107;"></div></div>
                <div class="mockup-badge" style="background:#FFF0C2; color:#8A5A00;" data-i18n="mock.row2.status">Expiring</div>
              </div>
              <div class="mockup-row">
                <div class="name">Sofía Torres</div>
                <div class="program" data-i18n="mock.row3.program">Basketball</div>
                <div class="mockup-bar-wrap"><div class="mockup-bar" style="width:80%; background:var(--volt);"></div></div>
                <div class="mockup-badge" style="background:var(--volt); color:var(--volt-dark);" data-i18n="mock.row3.status">Active</div>
              </div>
              <div class="mockup-row">
                <div class="name">Miguel Herrera</div>
                <div class="program" data-i18n="mock.row4.program">Track & Field</div>
                <div class="mockup-bar-wrap"><div class="mockup-bar" style="width:30%; background:#CC2200;"></div></div>
                <div class="mockup-badge" style="background:#1E1E1E; color:#4A4A48;" data-i18n="mock.row4.status">Inactive</div>
              </div>
            </div>
          </div>
        </div>
      </div>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Dashboard mockup card visible on right side at desktop widths.
- Sidebar strip with KLogoMark + 5 nav dots, first one has volt accent bar.
- 3 KPI cards (white "248", volt "1,840", amber "14").
- Attendance table with 4 rows, alternating bg, color-coded bars + status badges.
- Top edge has subtle volt highlight gradient.
- Drop-shadow visible behind mockup.
- Resize <900px → mockup disappears, copy column takes full width.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add hero dashboard mockup (CSS-only)"
```

---

## Task 6: Add hero background decorations (noise + volt glow)

**Files:**
- Modify: `klasio-landing/index.html` — append decorative CSS, add two `<div>`s inside `.hero`

- [ ] **Step 1: Append decorative CSS inside `<style>`**

```css
    /* === Hero decorations === */
    .hero-noise {
      position: absolute;
      inset: 0;
      opacity: 0.035;
      pointer-events: none;
      mix-blend-mode: overlay;
      z-index: 1;
    }
    .hero-glow {
      position: absolute;
      top: -100px;
      left: 50%;
      transform: translateX(-50%);
      width: 900px;
      height: 600px;
      background: radial-gradient(ellipse at center, rgba(202,255,77,0.07) 0%, transparent 60%);
      pointer-events: none;
      z-index: 1;
    }
```

- [ ] **Step 2: Inject the decorative `<div>`s as the first children of `<section id="hero">`**

```html
    <div class="hero-glow" aria-hidden="true"></div>
    <svg class="hero-noise" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
      <filter id="noiseFilter">
        <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" stitchTiles="stitch"/>
      </filter>
      <rect width="100%" height="100%" filter="url(#noiseFilter)"/>
    </svg>
```

These go before the existing `<div class="hero-grid">`.

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Subtle radial volt glow visible at top-center of hero.
- Subtle noise texture overlay on hero (very faint at 0.035 opacity).
- Z-index: hero-grid content sits above decorations (z-index: 2 vs 1).
- Decorations don't block clicks on hero buttons.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add hero noise + volt glow background decorations"
```

---

## Task 7: Build the value props section

**Files:**
- Modify: `klasio-landing/index.html` — append value-section CSS, add `<section id="valor">`

- [ ] **Step 1: Append value-section CSS inside `<style>`**

```css
    /* === Value props === */
    .value {
      background: var(--dark);
      padding: 96px 32px;
    }
    .value-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      border: 1px solid var(--border);
      border-radius: 20px;
      overflow: hidden;
      margin-top: 16px;
    }
    .value-cell {
      padding: 40px 44px;
      border-right: 1px solid var(--border);
      border-bottom: 1px solid var(--border);
      transition: background 0.2s ease;
    }
    .value-cell:nth-child(2n) { border-right: none; }
    .value-cell:nth-last-child(-n+2) { border-bottom: none; }
    .value-cell:hover { background: var(--surface); }
    .value-num {
      font-family: 'DM Mono', monospace;
      font-size: 11px;
      color: var(--volt);
      letter-spacing: 0.1em;
      margin-bottom: 20px;
      display: block;
    }
    .value-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: rgba(202,255,77,0.08);
      border: 1px solid rgba(202,255,77,0.15);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
    }
    .value-title {
      font-size: 20px;
      font-weight: 700;
      letter-spacing: -0.02em;
      color: var(--text);
      margin-bottom: 10px;
    }
    .value-body {
      font-size: 14px;
      color: var(--subtle);
      line-height: 1.7;
    }

    @media (max-width: 768px) {
      .value-grid { grid-template-columns: 1fr; }
      .value-cell { border-right: none; }
      .value-cell:nth-last-child(2) { border-bottom: 1px solid var(--border); }
      .value-cell { padding: 32px 28px; }
      .value { padding: 72px 20px; }
    }
```

- [ ] **Step 2: Add the `<section>` after the hero `<section>`**

```html
  <section id="valor" class="value">
    <div class="container">
      <div class="section-header">
        <span class="tag fade-up" data-i18n="value.tag">Why Klasio</span>
        <h2 class="section-title fade-up d1" data-i18n="value.title" style="margin-top:24px">Stop running your club on spreadsheets.</h2>
        <p class="section-sub fade-up d2" data-i18n="value.sub">Club owners lose hours every week on tasks Klasio handles in seconds.</p>
      </div>

      <div class="value-grid">
        <div class="value-cell fade-up">
          <span class="value-num">01</span>
          <div class="value-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
          </div>
          <h3 class="value-title" data-i18n="value.01.title">Real-time attendance control</h3>
          <p class="value-body" data-i18n="value.01.body">Mark attendance straight from the app. Know who showed up, who skipped, and how many hours each student has left — no Excel sheets.</p>
        </div>

        <div class="value-cell fade-up d1">
          <span class="value-num">02</span>
          <div class="value-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <h3 class="value-title" data-i18n="value.02.title">Memberships and payments under control</h3>
          <p class="value-body" data-i18n="value.02.body">Manage plans, expirations, and payment proofs in one place. Get automatic alerts before a membership expires.</p>
        </div>

        <div class="value-cell fade-up d2">
          <span class="value-num">03</span>
          <div class="value-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <h3 class="value-title" data-i18n="value.03.title">Your students, organized</h3>
          <p class="value-body" data-i18n="value.03.body">Every student's full record in a single profile: enrollments, level, class history, and hour movements. Accessible in seconds.</p>
        </div>

        <div class="value-cell fade-up d3">
          <span class="value-num">04</span>
          <div class="value-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
            </svg>
          </div>
          <h3 class="value-title" data-i18n="value.04.title">Decisions backed by data</h3>
          <p class="value-body" data-i18n="value.04.body">Dashboard with key metrics: hours consumed, attendance rate, pending payments, active programs. The whole club at a glance.</p>
        </div>
      </div>
    </div>
  </section>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Value section appears below hero.
- 2×2 grid with rounded outer border, hairline borders between cells.
- Each cell: number tag (volt mono), volt-tinted icon box, title, body.
- Hover on cell → bg shifts to surface color.
- Resize <768px → 1 column layout, all borders correct (no double borders).

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add value props section (2x2 grid, 4 cards)"
```

---

## Task 8: Build the features section

**Files:**
- Modify: `klasio-landing/index.html` — append features CSS, add `<section id="funcionalidades">`

- [ ] **Step 1: Append features CSS inside `<style>`**

```css
    /* === Features === */
    .features {
      background: #0D0D0D;
      padding: 96px 32px;
    }
    .feature-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
    }
    .feature-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 28px;
      transition: all 0.2s ease;
    }
    .feature-card:hover {
      border-color: var(--border2);
      transform: translateY(-2px);
    }
    .feature-card.featured {
      border-color: rgba(202,255,77,0.25);
      background: linear-gradient(145deg, #141414, #111);
    }
    .feature-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: rgba(202,255,77,0.08);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 18px;
    }
    .feature-title {
      font-size: 15px;
      font-weight: 700;
      letter-spacing: -0.01em;
      color: var(--text);
      margin-bottom: 8px;
    }
    .feature-body {
      font-size: 13px;
      color: var(--subtle);
      line-height: 1.65;
    }
    .feature-tag {
      display: inline-block;
      font-family: 'DM Mono', monospace;
      font-size: 9px;
      color: var(--volt);
      background: rgba(202,255,77,0.08);
      padding: 3px 8px;
      border-radius: 4px;
      margin-top: 14px;
    }

    @media (max-width: 900px) {
      .feature-grid { grid-template-columns: repeat(2, 1fr); }
    }
    @media (max-width: 560px) {
      .feature-grid { grid-template-columns: 1fr; }
      .features { padding: 72px 20px; }
    }
```

- [ ] **Step 2: Add the `<section>` after value section**

```html
  <section id="funcionalidades" class="features">
    <div class="container">
      <div class="section-header">
        <span class="tag fade-up" data-i18n="feat.tag">Features</span>
        <h2 class="section-title fade-up d1" data-i18n="feat.title" style="margin-top:24px">Everything your club needs.</h2>
        <p class="section-sub fade-up d2" data-i18n="feat.sub">A complete platform without unnecessary complexity.</p>
      </div>

      <div class="feature-grid">

        <article class="feature-card featured fade-up">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.1.title">Admin dashboard</h3>
          <p class="feature-body" data-i18n="feat.1.body">360° view of your club. Active students, hours consumed, pending payments, and attendance control with status filters.</p>
          <span class="feature-tag" data-i18n="feat.featured.badge">Featured</span>
        </article>

        <article class="feature-card fade-up d1">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.2.title">Student management</h3>
          <p class="feature-body" data-i18n="feat.2.body">Sign-up, deactivation, full profile, membership history, class enrollments, and per-program level tracking.</p>
        </article>

        <article class="feature-card fade-up d2">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.3.title">Attendance control</h3>
          <p class="feature-body" data-i18n="feat.3.body">Mark presence or absence per session. The system auto-deducts hours and keeps the full history.</p>
        </article>

        <article class="feature-card fade-up">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.4.title">Memberships and plans</h3>
          <p class="feature-body" data-i18n="feat.4.body">Build custom plans per program. Hour allocation, expiry dates, and automatic alerts included.</p>
        </article>

        <article class="feature-card fade-up d1">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.5.title">Payment proofs</h3>
          <p class="feature-body" data-i18n="feat.5.body">Students upload their payment proof from the app. The admin approves or rejects directly — no more WhatsApp back-and-forth.</p>
        </article>

        <article class="feature-card fade-up d2">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M22 17H2a3 3 0 0 0 3-3V9a7 7 0 0 1 14 0v5a3 3 0 0 0 3 3zm-8.27 4a2 2 0 0 1-3.46 0"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.6.title">Smart notifications</h3>
          <p class="feature-body" data-i18n="feat.6.body">Alerts for expiring memberships, cancelled classes, new payments, and more. Always informed, no manual checking.</p>
        </article>

        <article class="feature-card fade-up">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.7.title">Programs and classes</h3>
          <p class="feature-body" data-i18n="feat.7.body">Organize your offering into programs with multiple classes. Set schedules, capacity limits, and required levels.</p>
        </article>

        <article class="feature-card fade-up d1">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="23" y1="11" x2="17" y2="11"/><line x1="20" y1="8" x2="20" y2="14"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.8.title">Coach management</h3>
          <p class="feature-body" data-i18n="feat.8.body">Assign coaches to classes, manage their profiles, and give them their own access to mark attendance.</p>
        </article>

        <article class="feature-card fade-up d2">
          <div class="feature-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#CAFF4D" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>
            </svg>
          </div>
          <h3 class="feature-title" data-i18n="feat.9.title">Student app</h3>
          <p class="feature-body" data-i18n="feat.9.body">Your students see available classes, register for sessions, check remaining hours, and upload payment proofs — all from their phone.</p>
        </article>

      </div>
    </div>
  </section>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- 9 feature cards in 3-column grid at desktop.
- First card has the "Featured" tag and a subtle volt-tinted border + gradient.
- All icons stroke in volt color, sit in volt-tinted square.
- Hover: card lifts (translateY -2px) + border brightens.
- Resize <900px → 2 columns.
- Resize <560px → 1 column.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add features section (9 cards, featured first)"
```

---

## Task 9: Build the how-it-works section

**Files:**
- Modify: `klasio-landing/index.html` — append how-it-works CSS, add `<section id="como-funciona">`

- [ ] **Step 1: Append how-it-works CSS inside `<style>`**

```css
    /* === How it works === */
    .how-it-works {
      background: var(--dark);
      padding: 96px 32px;
    }
    .how-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 80px;
      align-items: center;
    }
    .how-step {
      display: flex;
      gap: 24px;
      padding: 28px 0;
      border-bottom: 1px solid var(--border);
    }
    .how-step:last-of-type { border-bottom: none; }
    .how-num {
      font-family: 'DM Mono', monospace;
      font-size: 11px;
      color: var(--volt);
      letter-spacing: 0.1em;
      min-width: 28px;
    }
    .how-step-title {
      font-size: 16px;
      font-weight: 700;
      color: var(--text);
      margin-bottom: 6px;
    }
    .how-step-body {
      font-size: 13px;
      color: var(--subtle);
      line-height: 1.65;
    }

    /* Roles card */
    .roles-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 28px;
      box-shadow: 0 24px 80px rgba(0,0,0,0.5);
    }
    .roles-title {
      font-family: 'DM Mono', monospace;
      font-size: 9px;
      color: var(--muted);
      letter-spacing: 0.1em;
      text-transform: uppercase;
      margin-bottom: 20px;
    }
    .role-item {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 14px 16px;
      border-radius: 12px;
      background: var(--surface2);
      border: 1px solid var(--border);
      margin-bottom: 8px;
    }
    .role-item:last-child { margin-bottom: 0; }
    .role-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      flex-shrink: 0;
    }
    .role-info { flex: 1; }
    .role-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--text);
      margin-bottom: 2px;
    }
    .role-desc {
      font-size: 11px;
      color: var(--subtle);
    }
    .role-badge {
      font-family: 'DM Mono', monospace;
      font-size: 9px;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      padding: 3px 8px;
      border-radius: 4px;
    }

    @media (max-width: 900px) {
      .how-grid { grid-template-columns: 1fr; gap: 48px; }
      .how-it-works { padding: 72px 20px; }
    }
```

- [ ] **Step 2: Add the `<section>` after the features `<section>`**

```html
  <section id="como-funciona" class="how-it-works">
    <div class="container how-grid">

      <div class="how-left">
        <span class="tag fade-up" data-i18n="how.tag">How it works</span>
        <h2 class="section-title fade-up d1" data-i18n="how.title" style="margin-top:24px">Up and running in less than a day.</h2>
        <p class="section-sub fade-up d2" data-i18n="how.sub" style="margin-bottom:24px">No installs, no complex setup. Your club operating digitally starting today.</p>

        <div class="how-step fade-up">
          <span class="how-num">01</span>
          <div>
            <h3 class="how-step-title" data-i18n="how.1.title">We create your club in Klasio</h3>
            <p class="how-step-body" data-i18n="how.1.body">We give you admin access and configure your tenant with your club's data.</p>
          </div>
        </div>
        <div class="how-step fade-up d1">
          <span class="how-num">02</span>
          <div>
            <h3 class="how-step-title" data-i18n="how.2.title">You load your programs and plans</h3>
            <p class="how-step-body" data-i18n="how.2.body">Define your disciplines, classes, schedules, and membership plans. Quick and uncomplicated.</p>
          </div>
        </div>
        <div class="how-step fade-up d2">
          <span class="how-num">03</span>
          <div>
            <h3 class="how-step-title" data-i18n="how.3.title">Invite your team and students</h3>
            <p class="how-step-body" data-i18n="how.3.body">Coaches and students get their access. Each role sees exactly what they need.</p>
          </div>
        </div>
        <div class="how-step fade-up d3">
          <span class="how-num">04</span>
          <div>
            <h3 class="how-step-title" data-i18n="how.4.title">Run your club with precision</h3>
            <p class="how-step-body" data-i18n="how.4.body">Attendance, payments, reports, notifications — everything centralized, everything under control.</p>
          </div>
        </div>

        <a href="#" class="btn btn-volt fade-up" data-wa-msg="how_setup" data-i18n="how.cta" style="margin-top:36px">Get started →</a>
      </div>

      <aside class="roles-card fade-up d2">
        <div class="roles-title" data-i18n="roles.title">Roles in Klasio</div>

        <div class="role-item">
          <div class="role-icon" style="background:rgba(202,255,77,0.12)" aria-hidden="true">🛡️</div>
          <div class="role-info">
            <div class="role-name" data-i18n="roles.admin.name">Administrator</div>
            <div class="role-desc" data-i18n="roles.admin.desc">Full control: students, payments, reports, config</div>
          </div>
          <span class="role-badge" style="background:rgba(202,255,77,0.10); color:var(--volt)" data-i18n="roles.admin.badge">Admin</span>
        </div>

        <div class="role-item">
          <div class="role-icon" style="background:rgba(255,193,7,0.10)" aria-hidden="true">🎓</div>
          <div class="role-info">
            <div class="role-name" data-i18n="roles.coach.name">Coach</div>
            <div class="role-desc" data-i18n="roles.coach.desc">Marks attendance, sees their class roster</div>
          </div>
          <span class="role-badge" style="background:rgba(255,193,7,0.10); color:#FFC107" data-i18n="roles.coach.badge">Coach</span>
        </div>

        <div class="role-item">
          <div class="role-icon" style="background:rgba(0,102,187,0.12)" aria-hidden="true">👤</div>
          <div class="role-info">
            <div class="role-name" data-i18n="roles.student.name">Student</div>
            <div class="role-desc" data-i18n="roles.student.desc">Checks classes, hours, uploads proofs</div>
          </div>
          <span class="role-badge" style="background:rgba(0,102,187,0.10); color:#5599DD" data-i18n="roles.student.badge">Student</span>
        </div>

        <div class="role-item">
          <div class="role-icon" style="background:rgba(202,255,77,0.08)" aria-hidden="true">⚙️</div>
          <div class="role-info">
            <div class="role-name" data-i18n="roles.manager.name">Manager</div>
            <div class="role-desc" data-i18n="roles.manager.desc">Operational control without config access</div>
          </div>
          <span class="role-badge" style="background:rgba(202,255,77,0.08); color:var(--volt)" data-i18n="roles.manager.badge">Manager</span>
        </div>
      </aside>

    </div>
  </section>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- 2-column layout: steps on left, roles card on right.
- 4 steps with volt-mono numbers (01–04), separator borders, last step has no border.
- "Get started →" button below steps.
- Roles card: dark surface, 4 role rows with emoji + name/desc + colored badge.
- Resize <900px → stacks to 1 column with 48px gap.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add how-it-works section (steps + roles card)"
```

---

## Task 10: Build the metrics section

**Files:**
- Modify: `klasio-landing/index.html` — append metrics CSS, add `<section id="metricas">`

- [ ] **Step 1: Append metrics CSS inside `<style>`**

```css
    /* === Metrics === */
    .metrics {
      background: #0D0D0D;
      border-top: 1px solid var(--border);
      padding: 96px 32px;
    }
    .metrics-header {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 32px;
      margin-bottom: 48px;
      flex-wrap: wrap;
    }
    .metrics-header .section-title {
      font-size: clamp(28px, 3.5vw, 44px);
      margin-bottom: 12px;
    }
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1px;
      background: var(--border);
      border-radius: 16px;
      overflow: hidden;
    }
    .metric-cell {
      background: var(--surface);
      padding: 32px 28px;
      text-align: center;
    }
    .metric-value {
      font-size: 44px;
      font-weight: 800;
      letter-spacing: -0.04em;
      line-height: 1;
      color: var(--text);
      margin-bottom: 8px;
    }
    .metric-value .accent { color: var(--volt); }
    .metric-label {
      font-family: 'DM Mono', monospace;
      font-size: 10px;
      color: var(--muted);
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    @media (max-width: 768px) {
      .metrics-grid { grid-template-columns: repeat(2, 1fr); }
      .metrics { padding: 72px 20px; }
    }
```

- [ ] **Step 2: Add the `<section>` after how-it-works section**

```html
  <section id="metricas" class="metrics">
    <div class="container">
      <div class="metrics-header">
        <div>
          <span class="tag fade-up" data-i18n="metrics.tag">Results</span>
          <h2 class="section-title fade-up d1" data-i18n="metrics.title" style="margin-top:24px">The impact of digitizing your club.</h2>
        </div>
        <a href="#" class="btn btn-volt fade-up d2" data-wa-msg="nav_demo" data-i18n="metrics.cta">Request demo →</a>
      </div>

      <div class="metrics-grid">
        <div class="metric-cell fade-up">
          <div class="metric-value"><span class="accent">90</span>%</div>
          <div class="metric-label" data-i18n="metrics.1.label">Less time on admin</div>
        </div>
        <div class="metric-cell fade-up d1">
          <div class="metric-value">0</div>
          <div class="metric-label" data-i18n="metrics.2.label">Spreadsheets needed</div>
        </div>
        <div class="metric-cell fade-up d2">
          <div class="metric-value"><span class="accent">∞</span></div>
          <div class="metric-label" data-i18n="metrics.3.label">Students supported</div>
        </div>
        <div class="metric-cell fade-up d3">
          <div class="metric-value"><span class="accent">24</span>/7</div>
          <div class="metric-label" data-i18n="metrics.4.label">Access from anywhere</div>
        </div>
      </div>
    </div>
  </section>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Header row: tag + title on left, "Request demo →" CTA right-aligned at bottom.
- 4 metric cells in single row at desktop, hairline gap between cells (1px border showing through).
- Values use volt accent on numbers (90, ∞, 24).
- Resize <768px → 2 columns.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add metrics section (4-cell hairline grid)"
```

---

## Task 11: Build the final CTA section

**Files:**
- Modify: `klasio-landing/index.html` — append CTA CSS, add `<section id="cta">`

- [ ] **Step 1: Append final-cta CSS inside `<style>`**

```css
    /* === Final CTA === */
    .final-cta {
      background: var(--dark);
      padding: 96px 32px;
      position: relative;
      overflow: hidden;
    }
    .final-cta-glow {
      position: absolute;
      bottom: -200px;
      left: 50%;
      transform: translateX(-50%);
      width: 800px;
      height: 500px;
      background: radial-gradient(ellipse at center, rgba(202,255,77,0.06) 0%, transparent 60%);
      pointer-events: none;
    }
    .final-cta-content {
      max-width: 720px;
      margin: 0 auto;
      text-align: center;
      position: relative;
      z-index: 1;
    }
    .final-cta .section-title {
      font-size: clamp(36px, 4.5vw, 60px);
      margin: 24px auto 24px;
    }
    .final-cta-sub {
      font-size: 17px;
      color: var(--subtle);
      line-height: 1.7;
      max-width: 480px;
      margin: 0 auto 40px;
    }
    .final-cta .btn-volt {
      font-size: 15px;
      padding: 15px 32px;
    }
    .final-cta-note {
      font-family: 'DM Mono', monospace;
      font-size: 11px;
      color: var(--muted);
      letter-spacing: 0.06em;
      margin-top: 24px;
    }
```

- [ ] **Step 2: Add the `<section>` after metrics section**

```html
  <section id="cta" class="final-cta">
    <div class="final-cta-glow" aria-hidden="true"></div>
    <div class="final-cta-content fade-up">
      <span class="tag" data-i18n="cta.tag">Ready to get started?</span>
      <h2 class="section-title" data-i18n-html="cta.title">Your club deserves <span class="volt-text">professional</span> tools.</h2>
      <p class="final-cta-sub" data-i18n="cta.sub">Let's chat on WhatsApp. We'll show you Klasio in action and configure it for your club the same day.</p>
      <a href="#" class="btn btn-volt" data-wa-msg="hero_primary" aria-label="Contact via WhatsApp">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
        </svg>
        <span data-i18n="cta.button">Contact via WhatsApp</span>
      </a>
      <div class="final-cta-note" data-i18n="cta.note">No commitments · Reply in under 1 hour</div>
    </div>
  </section>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Centered content, max-width 720px.
- Tag pill, big title with "professional" in volt green, subtitle, CTA button with WhatsApp icon, mono note below.
- Subtle volt glow at bottom center.
- Title uses `data-i18n-html` because of the inner span.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add final CTA section with WhatsApp button"
```

---

## Task 12: Build the footer

**Files:**
- Modify: `klasio-landing/index.html` — append footer CSS, add `<footer>`

- [ ] **Step 1: Append footer CSS inside `<style>`**

```css
    /* === Footer === */
    .footer {
      background: #070707;
      border-top: 1px solid var(--border);
      padding: 48px 32px;
    }
    .footer-inner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 24px;
      max-width: 1120px;
      margin: 0 auto;
    }
    .footer-links {
      display: flex;
      gap: 24px;
    }
    .footer-link {
      font-family: 'DM Mono', monospace;
      font-size: 11px;
      color: var(--muted);
      letter-spacing: 0.06em;
      text-transform: uppercase;
      transition: color 0.15s ease;
    }
    .footer-link:hover { color: var(--volt); }
    .footer-copy {
      font-family: 'DM Mono', monospace;
      font-size: 10px;
      color: #2A2A2A;
    }
```

- [ ] **Step 2: Add the `<footer>` after the final CTA section**

```html
  <footer class="footer">
    <div class="footer-inner">
      <div class="logo">
        <svg width="22" height="22" viewBox="0 0 40 40" fill="none" aria-hidden="true">
          <rect x="6" y="6" width="6" height="28" fill="#CAFF4D"/>
          <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9"/>
          <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6"/>
        </svg>
        <span class="wordmark">klasio</span>
      </div>
      <nav class="footer-links" aria-label="Footer">
        <a href="#valor" class="footer-link" data-i18n="footer.benefits">Benefits</a>
        <a href="#funcionalidades" class="footer-link" data-i18n="footer.features">Features</a>
        <a href="#" class="footer-link" data-wa-msg="nav_contact" data-i18n="footer.contact">Contact</a>
      </nav>
      <div class="footer-copy" data-i18n="footer.copy">© 2026 klasio.club · All rights reserved</div>
    </div>
  </footer>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Footer at bottom: dark bg (#070707), top border.
- Logo + 3 mono links + copyright in flex row.
- Wraps gracefully at narrow widths.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add footer with logo, links and copyright"
```

---

## Task 13: Add the floating WhatsApp button (FAB)

**Files:**
- Modify: `klasio-landing/index.html` — append FAB CSS, add `<a class="wa-fab">` before closing `</body>`

- [ ] **Step 1: Append FAB CSS inside `<style>`**

```css
    /* === Floating WhatsApp button === */
    .wa-fab {
      position: fixed;
      bottom: 28px;
      right: 28px;
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: #25D366;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 8px 32px rgba(37,211,102,0.4);
      z-index: 200;
      transition: all 0.2s ease;
      color: #FFFFFF;
    }
    .wa-fab:hover {
      transform: scale(1.1);
      box-shadow: 0 12px 40px rgba(37,211,102,0.55);
    }
```

- [ ] **Step 2: Add the FAB anchor right before the `<script>` tag**

```html
  <a class="wa-fab" href="#" data-wa-msg="fab" aria-label="Contact us on WhatsApp">
    <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
    </svg>
  </a>
```

- [ ] **Step 3: Verify in browser**

Reload. Verify:
- Green circular WhatsApp button fixed at bottom-right.
- Hover → scales up + shadow grows.
- Visible on every section as you scroll.

- [ ] **Step 4: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add floating WhatsApp button (FAB)"
```

---

## Task 14: Wire up i18n + WhatsApp link generation (the JS)

**Files:**
- Modify: `klasio-landing/index.html` — replace the empty `<script>` block contents

- [ ] **Step 1: Replace the contents of the `<script>` tag with the full init script**

```html
  <script>
    // === Klasio Landing — Runtime Config ===
    // Override at deploy time (Cloudflare Pages / Netlify) by injecting:
    //   <script>window.KLASIO_CONFIG = { whatsappNumber: '...' }</script>
    // BEFORE this script runs. Otherwise edit the default below.
    window.KLASIO_CONFIG = window.KLASIO_CONFIG || {};
    const KLASIO_WHATSAPP_NUMBER = window.KLASIO_CONFIG.whatsappNumber || '573127061833';

    // === i18n dictionary ===
    const I18N = {
      en: {
        'meta.title': 'Klasio — Sports club management software',
        'meta.description': 'All-in-one platform to manage memberships, attendance, payments and programs for your sports club. No spreadsheets, no chaos.',
        'meta.og.title': 'Klasio — Sports club management software',
        'meta.og.description': 'The professional way to run your sports club.',

        'nav.benefits': 'Benefits',
        'nav.features': 'Features',
        'nav.how': 'How it works',
        'nav.contact': 'Contact',
        'nav.demo': 'See demo →',

        'hero.tag': 'Professional sports management',
        'hero.h1.line1': 'Your club,',
        'hero.h1.line2': 'managed',
        'hero.h1.line3': 'with <span class="volt-text">precision.</span>',
        'hero.subtitle': 'Klasio is the all-in-one platform for sports clubs. Manage memberships, attendance, payments and programs from a single place — no spreadsheets, no chaos.',
        'hero.cta.primary': 'Request free demo →',
        'hero.cta.secondary': 'See features ↓',
        'hero.stat1.value': '100%', 'hero.stat1.label': 'Digital',
        'hero.stat2.value': '5',    'hero.stat2.label': 'User roles',
        'hero.stat3.value': '∞',    'hero.stat3.label': 'Students',

        'mock.dashboard': 'Dashboard',
        'mock.subtitle': 'NORTH LEAGUE · CURRENT PERIOD',
        'mock.kpi1.label': 'STUDENTS',  'mock.kpi1.sub': '↑ 12 this month',
        'mock.kpi2.label': 'HOURS USED',
        'mock.kpi3.label': 'PENDING PAYMENTS',
        'mock.attendance.title': 'Attendance control',
        'mock.attendance.cta': 'Start class',
        'mock.row1.program': 'Advanced Swimming', 'mock.row1.status': 'Active',
        'mock.row2.program': 'Youth Karate',      'mock.row2.status': 'Expiring',
        'mock.row3.program': 'Basketball',        'mock.row3.status': 'Active',
        'mock.row4.program': 'Track & Field',     'mock.row4.status': 'Inactive',

        'value.tag': 'Why Klasio',
        'value.title': 'Stop running your club on spreadsheets.',
        'value.sub': 'Club owners lose hours every week on tasks Klasio handles in seconds.',
        'value.01.title': 'Real-time attendance control',
        'value.01.body': 'Mark attendance straight from the app. Know who showed up, who skipped, and how many hours each student has left — no Excel sheets.',
        'value.02.title': 'Memberships and payments under control',
        'value.02.body': 'Manage plans, expirations, and payment proofs in one place. Get automatic alerts before a membership expires.',
        'value.03.title': 'Your students, organized',
        'value.03.body': "Every student's full record in a single profile: enrollments, level, class history, and hour movements. Accessible in seconds.",
        'value.04.title': 'Decisions backed by data',
        'value.04.body': 'Dashboard with key metrics: hours consumed, attendance rate, pending payments, active programs. The whole club at a glance.',

        'feat.tag': 'Features',
        'feat.title': 'Everything your club needs.',
        'feat.sub': 'A complete platform without unnecessary complexity.',
        'feat.featured.badge': 'Featured',
        'feat.1.title': 'Admin dashboard',
        'feat.1.body': '360° view of your club. Active students, hours consumed, pending payments, and attendance control with status filters.',
        'feat.2.title': 'Student management',
        'feat.2.body': 'Sign-up, deactivation, full profile, membership history, class enrollments, and per-program level tracking.',
        'feat.3.title': 'Attendance control',
        'feat.3.body': 'Mark presence or absence per session. The system auto-deducts hours and keeps the full history.',
        'feat.4.title': 'Memberships and plans',
        'feat.4.body': 'Build custom plans per program. Hour allocation, expiry dates, and automatic alerts included.',
        'feat.5.title': 'Payment proofs',
        'feat.5.body': 'Students upload their payment proof from the app. The admin approves or rejects directly — no more WhatsApp back-and-forth.',
        'feat.6.title': 'Smart notifications',
        'feat.6.body': 'Alerts for expiring memberships, cancelled classes, new payments, and more. Always informed, no manual checking.',
        'feat.7.title': 'Programs and classes',
        'feat.7.body': 'Organize your offering into programs with multiple classes. Set schedules, capacity limits, and required levels.',
        'feat.8.title': 'Coach management',
        'feat.8.body': 'Assign coaches to classes, manage their profiles, and give them their own access to mark attendance.',
        'feat.9.title': 'Student app',
        'feat.9.body': 'Your students see available classes, register for sessions, check remaining hours, and upload payment proofs — all from their phone.',

        'how.tag': 'How it works',
        'how.title': 'Up and running in less than a day.',
        'how.sub': 'No installs, no complex setup. Your club operating digitally starting today.',
        'how.1.title': 'We create your club in Klasio',
        'how.1.body': "We give you admin access and configure your tenant with your club's data.",
        'how.2.title': 'You load your programs and plans',
        'how.2.body': 'Define your disciplines, classes, schedules, and membership plans. Quick and uncomplicated.',
        'how.3.title': 'Invite your team and students',
        'how.3.body': 'Coaches and students get their access. Each role sees exactly what they need.',
        'how.4.title': 'Run your club with precision',
        'how.4.body': 'Attendance, payments, reports, notifications — everything centralized, everything under control.',
        'how.cta': 'Get started →',
        'roles.title': 'Roles in Klasio',
        'roles.admin.name': 'Administrator', 'roles.admin.desc': 'Full control: students, payments, reports, config', 'roles.admin.badge': 'Admin',
        'roles.coach.name': 'Coach',         'roles.coach.desc': 'Marks attendance, sees their class roster',       'roles.coach.badge': 'Coach',
        'roles.student.name': 'Student',     'roles.student.desc': 'Checks classes, hours, uploads proofs',         'roles.student.badge': 'Student',
        'roles.manager.name': 'Manager',     'roles.manager.desc': 'Operational control without config access',     'roles.manager.badge': 'Manager',

        'metrics.tag': 'Results',
        'metrics.title': 'The impact of digitizing your club.',
        'metrics.cta': 'Request demo →',
        'metrics.1.label': 'Less time on admin',
        'metrics.2.label': 'Spreadsheets needed',
        'metrics.3.label': 'Students supported',
        'metrics.4.label': 'Access from anywhere',

        'cta.tag': 'Ready to get started?',
        'cta.title': 'Your club deserves <span class="volt-text">professional</span> tools.',
        'cta.sub': "Let's chat on WhatsApp. We'll show you Klasio in action and configure it for your club the same day.",
        'cta.button': 'Contact via WhatsApp',
        'cta.note': 'No commitments · Reply in under 1 hour',

        'footer.benefits': 'Benefits',
        'footer.features': 'Features',
        'footer.contact': 'Contact',
        'footer.copy': '© 2026 klasio.club · All rights reserved'
      },
      es: {
        'meta.title': 'Klasio — Software de gestión para clubes deportivos',
        'meta.description': 'Plataforma todo-en-uno para gestionar membresías, asistencias, pagos y programas de tu club deportivo. Sin planillas, sin caos.',
        'meta.og.title': 'Klasio — Software de gestión para clubes deportivos',
        'meta.og.description': 'La forma profesional de administrar tu club deportivo.',

        'nav.benefits': 'Beneficios',
        'nav.features': 'Funcionalidades',
        'nav.how': 'Cómo funciona',
        'nav.contact': 'Contactar',
        'nav.demo': 'Ver demo →',

        'hero.tag': 'Gestión deportiva profesional',
        'hero.h1.line1': 'Tu club,',
        'hero.h1.line2': 'administrado',
        'hero.h1.line3': 'con <span class="volt-text">precisión.</span>',
        'hero.subtitle': 'Klasio es la plataforma todo-en-uno para clubes deportivos. Controla membresías, asistencias, pagos y programas desde un solo lugar — sin planillas, sin caos.',
        'hero.cta.primary': 'Solicitar demo gratuita →',
        'hero.cta.secondary': 'Ver funcionalidades ↓',
        'hero.stat1.value': '100%', 'hero.stat1.label': 'Digital',
        'hero.stat2.value': '5',    'hero.stat2.label': 'Roles de usuario',
        'hero.stat3.value': '∞',    'hero.stat3.label': 'Estudiantes',

        'mock.dashboard': 'Dashboard',
        'mock.subtitle': 'LIGA NORTE · PERÍODO ACTUAL',
        'mock.kpi1.label': 'ESTUDIANTES',  'mock.kpi1.sub': '↑ 12 este mes',
        'mock.kpi2.label': 'HORAS CONSUMIDAS',
        'mock.kpi3.label': 'PAGOS PENDIENTES',
        'mock.attendance.title': 'Control de asistencia',
        'mock.attendance.cta': 'Iniciar clase',
        'mock.row1.program': 'Natación Avanzado', 'mock.row1.status': 'Activo',
        'mock.row2.program': 'Karate Juvenil',    'mock.row2.status': 'Por vencer',
        'mock.row3.program': 'Basketball',        'mock.row3.status': 'Activo',
        'mock.row4.program': 'Atletismo Base',    'mock.row4.status': 'Inactivo',

        'value.tag': 'Por qué Klasio',
        'value.title': 'Deja de administrar con planillas.',
        'value.sub': 'Los dueños de clubes pierden horas cada semana en tareas que Klasio resuelve en segundos.',
        'value.01.title': 'Control de asistencia en tiempo real',
        'value.01.body': 'Marcá la asistencia directamente desde la app. Sabé quién vino, quién faltó y cuántas horas le quedan a cada estudiante — sin planillas Excel.',
        'value.02.title': 'Membresías y pagos bajo control',
        'value.02.body': 'Gestioná planes, vencimientos y comprobantes de pago en un solo lugar. Recibí alertas automáticas antes de que una membresía venza.',
        'value.03.title': 'Tus estudiantes, organizados',
        'value.03.body': 'Toda la información de cada alumno en un perfil: inscripciones, nivel, historial de clases y movimientos de horas. Accesible en segundos.',
        'value.04.title': 'Decisiones basadas en datos',
        'value.04.body': 'Dashboard con métricas clave: horas consumidas, tasa de asistencia, pagos pendientes, programas activos. Toda la foto del club de un vistazo.',

        'feat.tag': 'Funcionalidades',
        'feat.title': 'Todo lo que tu club necesita.',
        'feat.sub': 'Una plataforma completa, sin complejidad innecesaria.',
        'feat.featured.badge': 'Destacado',
        'feat.1.title': 'Dashboard de administración',
        'feat.1.body': 'Vista 360° del club. Estudiantes activos, horas consumidas, pagos pendientes y control de asistencia con filtros por estado.',
        'feat.2.title': 'Gestión de estudiantes',
        'feat.2.body': 'Alta, baja, perfil completo, historial de membresías, inscripciones a clases y seguimiento de nivel por programa.',
        'feat.3.title': 'Control de asistencia',
        'feat.3.body': 'Marcá presencia o ausencia por sesión. El sistema descuenta horas automáticamente y registra todo el historial.',
        'feat.4.title': 'Membresías y planes',
        'feat.4.body': 'Creá planes personalizados por programa. Asignación de horas, fechas de vencimiento y alertas automáticas incluidas.',
        'feat.5.title': 'Comprobantes de pago',
        'feat.5.body': 'Los estudiantes suben su comprobante desde la app. El admin aprueba o rechaza directamente, sin idas y vueltas por WhatsApp.',
        'feat.6.title': 'Notificaciones inteligentes',
        'feat.6.body': 'Alertas de membresías por vencer, clases canceladas, pagos nuevos y más. Siempre informado, sin revisar manualmente.',
        'feat.7.title': 'Programas y clases',
        'feat.7.body': 'Organizá tu oferta deportiva en programas con múltiples clases. Definí horarios, cupos máximos y nivel requerido.',
        'feat.8.title': 'Gestión de profesores',
        'feat.8.body': 'Asigná profesores a clases, gestioná su perfil y dales acceso propio para que marquen asistencia.',
        'feat.9.title': 'App para estudiantes',
        'feat.9.body': 'Tus alumnos ven sus clases disponibles, se registran a sesiones, consultan horas restantes y suben comprobantes — todo desde el celular.',

        'how.tag': 'Cómo funciona',
        'how.title': 'En marcha en menos de un día.',
        'how.sub': 'Sin instalaciones, sin configuraciones complejas. Tu club operando digitalmente desde hoy.',
        'how.1.title': 'Creamos tu club en Klasio',
        'how.1.body': 'Te damos acceso como administrador y configuramos tu tenant con los datos de tu club.',
        'how.2.title': 'Cargás tus programas y planes',
        'how.2.body': 'Definís tus disciplinas, clases, horarios y planes de membresía. Rápido y sin complicaciones.',
        'how.3.title': 'Invitás a tu equipo y estudiantes',
        'how.3.body': 'Profesores y alumnos reciben su acceso. Cada rol ve exactamente lo que necesita.',
        'how.4.title': 'Operás tu club con precisión',
        'how.4.body': 'Asistencias, pagos, reportes, notificaciones — todo centralizado, todo bajo control.',
        'how.cta': 'Empezar ahora →',
        'roles.title': 'Roles en Klasio',
        'roles.admin.name': 'Administrador', 'roles.admin.desc': 'Control total: estudiantes, pagos, reportes, config', 'roles.admin.badge': 'Admin',
        'roles.coach.name': 'Profesor',      'roles.coach.desc': 'Marca asistencia, ve el roster de sus clases',     'roles.coach.badge': 'Profesor',
        'roles.student.name': 'Estudiante',  'roles.student.desc': 'Consulta clases, horas, sube comprobantes',      'roles.student.badge': 'Estudiante',
        'roles.manager.name': 'Manager',     'roles.manager.desc': 'Gestión operativa sin acceso a configuración',   'roles.manager.badge': 'Manager',

        'metrics.tag': 'Resultados',
        'metrics.title': 'El impacto de digitalizar tu club.',
        'metrics.cta': 'Solicitar demo →',
        'metrics.1.label': 'Menos tiempo en admin',
        'metrics.2.label': 'Planillas necesarias',
        'metrics.3.label': 'Estudiantes soportados',
        'metrics.4.label': 'Acceso desde cualquier lugar',

        'cta.tag': '¿Listo para empezar?',
        'cta.title': 'Tu club merece herramientas <span class="volt-text">profesionales.</span>',
        'cta.sub': 'Hablemos por WhatsApp. Te mostramos Klasio en acción y lo configuramos para tu club en el mismo día.',
        'cta.button': 'Contactar por WhatsApp',
        'cta.note': 'Sin compromisos · Respuesta en menos de 1 hora',

        'footer.benefits': 'Beneficios',
        'footer.features': 'Funcionalidades',
        'footer.contact': 'Contacto',
        'footer.copy': '© 2026 klasio.club · Todos los derechos reservados'
      }
    };

    // === WhatsApp prefilled messages ===
    const WA_MESSAGES = {
      nav_contact:  { en: 'Hi, I want to learn more about Klasio',     es: 'Hola, quiero saber más sobre Klasio' },
      nav_demo:     { en: 'Hi, I want a demo of Klasio',               es: 'Hola, quiero una demo de Klasio' },
      hero_primary: { en: 'Hi, I want a free demo of Klasio',          es: 'Hola, quiero una demo gratuita de Klasio' },
      how_setup:    { en: 'Hi, I want to set up my club on Klasio',    es: 'Hola, quiero configurar mi club en Klasio' },
      fab:          { en: 'Hi, I want to learn more about Klasio',     es: 'Hola, quiero saber más sobre Klasio' }
    };

    // === Detect language ===
    function detectLang() {
      const nav = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
      return nav.startsWith('es') ? 'es' : 'en';
    }

    // === Apply translations ===
    function applyTranslations(lang) {
      const dict = I18N[lang] || I18N.en;
      document.documentElement.lang = lang;

      // Text-only swaps
      document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.dataset.i18n;
        if (dict[key] != null) el.textContent = dict[key];
      });
      // HTML swaps (for elements containing inner spans like volt-text)
      document.querySelectorAll('[data-i18n-html]').forEach(el => {
        const key = el.dataset.i18nHtml;
        if (dict[key] != null) el.innerHTML = dict[key];
      });
      // Document metadata
      if (dict['meta.title']) document.title = dict['meta.title'];
      const descMeta = document.querySelector('meta[name="description"]');
      if (descMeta && dict['meta.description']) descMeta.setAttribute('content', dict['meta.description']);
      const ogTitle = document.querySelector('meta[property="og:title"]');
      if (ogTitle && dict['meta.og.title']) ogTitle.setAttribute('content', dict['meta.og.title']);
      const ogDesc = document.querySelector('meta[property="og:description"]');
      if (ogDesc && dict['meta.og.description']) ogDesc.setAttribute('content', dict['meta.og.description']);
    }

    // === Wire WhatsApp anchors ===
    function wireWhatsApp(lang) {
      document.querySelectorAll('[data-wa-msg]').forEach(el => {
        const key = el.dataset.waMsg;
        const msg = (WA_MESSAGES[key] && WA_MESSAGES[key][lang]) || (WA_MESSAGES[key] && WA_MESSAGES[key].en) || '';
        el.href = `https://wa.me/${KLASIO_WHATSAPP_NUMBER}?text=${encodeURIComponent(msg)}`;
        el.target = '_blank';
        el.rel = 'noopener';
      });
    }

    // === Init on DOM ready ===
    document.addEventListener('DOMContentLoaded', () => {
      const lang = detectLang();
      applyTranslations(lang);
      wireWhatsApp(lang);
    });
  </script>
```

- [ ] **Step 2: Verify in browser — English path**

Reload the file. Open devtools Console:
```js
// Confirm number is set:
KLASIO_WHATSAPP_NUMBER
// → "573127061833"

// Confirm WhatsApp links resolved:
document.querySelectorAll('[data-wa-msg]').forEach(a => console.log(a.dataset.waMsg, a.href));
// → all hrefs start with "https://wa.me/573127061833?text="

// Confirm lang attr:
document.documentElement.lang
// → "en" (assuming default English browser)
```

Click any WhatsApp button — opens new tab with prefilled English message.

- [ ] **Step 3: Verify in browser — Spanish path**

Open devtools Sensors panel (Chrome: Cmd+Shift+P → "Sensors" → set Locale to `es-ES`), or open in a Spanish-set browser. Reload.

Or quicker: in Console, simulate by overriding before init:
```js
// Force ES rendering on demand:
applyTranslations('es'); wireWhatsApp('es');
// → All text swaps to Spanish, document.title updates, all WA links now have Spanish text= params.
```

Verify:
- Hero h1 reads "Tu club, administrado, con precisión."
- Nav reads "Beneficios / Funcionalidades / Cómo funciona".
- `document.title` → "Klasio — Software de gestión para clubes deportivos".
- WhatsApp button hrefs contain `text=Hola%2C%20quiero...`.

- [ ] **Step 4: Verify deploy-time override mechanism**

In Console:
```js
// Reset:
window.KLASIO_CONFIG = { whatsappNumber: '15555550100' };
// (would need to reload script — for verification, just inspect the override path manually)
```

Confirm the script reads `window.KLASIO_CONFIG.whatsappNumber || '573127061833'`. Document this in a brief comment if needed (already in the script header).

- [ ] **Step 5: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): wire i18n, WhatsApp link generation, runtime config"
```

---

## Task 15: Add IntersectionObserver fade-up animations

**Files:**
- Modify: `klasio-landing/index.html` — append IntersectionObserver block to existing `<script>`

- [ ] **Step 1: Append the IntersectionObserver block at the end of the `<script>` (after `DOMContentLoaded` listener)**

```js
    // === Scroll-triggered fade-up ===
    document.addEventListener('DOMContentLoaded', () => {
      const heroFades = document.querySelectorAll('#hero .fade-up');
      // Hero elements unblock immediately
      setTimeout(() => heroFades.forEach(el => el.classList.add('visible')), 100);

      // Everything else: IntersectionObserver
      const restFades = document.querySelectorAll('.fade-up:not(#hero .fade-up)');
      if ('IntersectionObserver' in window) {
        const io = new IntersectionObserver((entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              entry.target.classList.add('visible');
              io.unobserve(entry.target);
            }
          });
        }, { threshold: 0.12 });
        restFades.forEach(el => io.observe(el));
      } else {
        // Fallback: reveal everything immediately
        restFades.forEach(el => el.classList.add('visible'));
      }
    });
```

- [ ] **Step 2: Verify in browser**

Reload. Verify:
- On load, hero copy + mockup fade up after a brief 100ms delay.
- Scroll down — value props, features, steps, metrics, CTA all fade up as they enter the viewport.
- Each `fade-up` element only animates once (no flicker on re-entry).

- [ ] **Step 3: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add IntersectionObserver fade-up scroll animations"
```

---

## Task 16: Add `prefers-reduced-motion` support

**Files:**
- Modify: `klasio-landing/index.html` — append media query at the bottom of `<style>`

- [ ] **Step 1: Append reduced-motion CSS at the end of `<style>` (before `</style>`)**

```css
    /* === Reduced motion accessibility === */
    @media (prefers-reduced-motion: reduce) {
      .fade-up {
        opacity: 1 !important;
        transform: none !important;
        transition: none !important;
      }
      .tag::before {
        animation: none !important;
      }
      .feature-card,
      .btn-volt,
      .wa-fab {
        transition: none !important;
      }
      *, *::before, *::after {
        scroll-behavior: auto !important;
      }
    }
```

- [ ] **Step 2: Verify in browser**

In devtools (Chrome): Cmd+Shift+P → "Show Rendering" → set "Emulate CSS media feature prefers-reduced-motion" to `reduce`.

Reload. Verify:
- All elements visible immediately, no fade-in.
- Pulsing dot in hero tag pill is static.
- No hover transition wobble.
- Smooth scroll on anchor click is disabled (instant jump).

Reset to "no-preference" — animations come back.

- [ ] **Step 3: Commit**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): respect prefers-reduced-motion (disable animations)"
```

---

## Task 17: Final quality verification

**Files:**
- No edits — verification only

- [ ] **Step 1: Open file via `file://` protocol**

```bash
open klasio-landing/index.html
```

Verify:
- Page loads with no errors in browser console.
- Fonts loaded (DM Sans + DM Mono visible in Network tab).
- All 8 sections rendered.

- [ ] **Step 2: Visual check at 4 viewport widths**

In Chrome DevTools (Cmd+Shift+M → device toolbar):
- 1440px (desktop large): Hero is split, 3-col features, 2-col how-it-works.
- 1024px (desktop): Same layout, slightly compressed.
- 768px (tablet): Nav center links hidden, value 1-col, features 2-col, how-it-works 1-col, metrics 2-col.
- 375px (mobile): Hero mockup hidden, all sections single-column, FAB still pinned.

Check: no horizontal scroll on any width. No overflowing text. All buttons remain tappable.

- [ ] **Step 3: Verify all WhatsApp anchors resolve correctly**

In Console:
```js
const all = document.querySelectorAll('[data-wa-msg]');
console.log('Total WhatsApp anchors:', all.length);
// Expected: 8 → nav contact, nav demo, hero primary, how_setup,
//              metrics CTA, final CTA button, footer contact, fab

const bad = [...all].filter(a => !a.href.startsWith('https://wa.me/573127061833?text='));
console.log('Misconfigured anchors:', bad.length, bad);
// Expected: 0
```

- [ ] **Step 4: Verify no literal `wa.me` URLs in HTML markup**

```bash
grep -n 'wa.me' klasio-landing/index.html
```

Expected: only matches in the JS template literal (`https://wa.me/...` inside the script) and in the spec doc comment. **No** matches in `<a href="https://wa.me/...">` form.

- [ ] **Step 5: Verify the WhatsApp number constant swap propagates**

In Console:
```js
// Before:
document.querySelector('.wa-fab').href
// → "https://wa.me/573127061833?text=Hi%2C%20I%20want..."

// After:
window.KLASIO_CONFIG = { whatsappNumber: '15555550100' };
// (cannot re-init in same page, but the override path is verified by reading the script)
```

Confirm the script's first line reads:
```js
const KLASIO_WHATSAPP_NUMBER = window.KLASIO_CONFIG.whatsappNumber || '573127061833';
```

- [ ] **Step 6: Verify language switching**

- Open in browser with English locale → see English copy.
- Switch DevTools Sensors → Locale `es-CO` (or `es-ES`) → reload → see Spanish copy.
- Verify `document.title` switches.
- Verify WhatsApp prefilled text switches language.

- [ ] **Step 7: Verify section anchor navigation**

Click each nav link:
- "Benefits" → scrolls to `#valor`
- "Features" → scrolls to `#funcionalidades`
- "How it works" → scrolls to `#como-funciona`

All smooth-scroll without jump.

- [ ] **Step 8: Run Lighthouse audit (mobile)**

In Chrome DevTools → Lighthouse tab → Categories: Performance + Accessibility + Best Practices + SEO. Mode: Navigation. Device: Mobile. Run.

Targets:
- Performance ≥ 95
- Accessibility ≥ 95
- Best Practices ≥ 95
- SEO ≥ 95

If a score is below target, address the specific Lighthouse-flagged issue (most common: missing `alt` or `aria-label`, missing `meta description` — already added).

- [ ] **Step 9: Verify JS-disabled fallback**

In Chrome DevTools → Cmd+Shift+P → "Disable JavaScript" → reload.

Verify:
- Page renders with English copy as the static fallback.
- `.fade-up` elements remain hidden (because JS doesn't add `.visible` class).

To make the JS-disabled experience usable, accept that animations are off but content is invisible. **Decision check:** if the spec demands JS-disabled = visible, add a `<noscript>` block with `<style>.fade-up { opacity: 1 !important; transform: none !important; }</style>`.

- [ ] **Step 10: Add the `<noscript>` reveal fallback**

Inside `<head>`, after `<style>`:

```html
  <noscript>
    <style>
      .fade-up { opacity: 1 !important; transform: none !important; }
      [data-wa-msg] { pointer-events: none; opacity: 0.6; }
    </style>
  </noscript>
```

Reload with JS still disabled — content visible, WhatsApp links visually de-emphasized (since they would 404 without JS to set the href).

Re-enable JavaScript.

- [ ] **Step 11: Commit verification fixes**

```bash
git add klasio-landing/index.html
git commit -m "feat(landing): add noscript fallback for JS-disabled visitors"
```

---

## Self-Review

**1. Spec coverage check** — every spec section has a task:

| Spec section | Task |
|---|---|
| D1 — repo root location | Task 1 (mkdir + scaffold) |
| D2 — single file, inline CSS, vanilla JS | All tasks |
| D3 — i18n auto-detect, no toggle, no localStorage | Task 14 |
| D4 — WhatsApp constant + WA_MESSAGES + data-wa-msg | Task 14 + verify in Task 17 |
| D5 — brand fidelity (CSS vars, fonts, KLogoMark) | Task 1 + reused throughout |
| D6 — animations (fade-up, pulse, reduced motion) | Tasks 2, 15, 16 |
| Section 1 Nav | Task 3 |
| Section 2 Hero (left + right + decorations) | Tasks 4, 5, 6 |
| Section 3 Value props | Task 7 |
| Section 4 Features | Task 8 |
| Section 5 How it works (steps + roles) | Task 9 |
| Section 6 Metrics | Task 10 |
| Section 7 Final CTA | Task 11 |
| Section 8 Footer | Task 12 |
| Floating WhatsApp FAB | Task 13 |
| All EN + ES copy keys | Task 14 (full I18N dict) |
| WA_MESSAGES map (5 keys) | Task 14 |
| Document metadata translation | Task 14 (applyTranslations updates title + meta tags) |
| Accessibility (semantic tags, aria, contrast, focus) | Inline throughout (semantic `<nav>`, `<section>`, `<footer>`, `<h1>`-`<h3>`, `aria-hidden` on decorative SVGs, `aria-label` on icon-only links) |
| SEO meta + canonical + OG tags | Task 1 (initial) + Task 14 (i18n updates) |
| Performance (preconnect, no images) | Task 1 (preconnect), all tasks (no `<img>` tags except none) |
| prefers-reduced-motion | Task 16 |
| Quality checklist (file://, 4 widths, no console errors, Lighthouse, i18n switch, JS-disabled) | Task 17 |

**2. Placeholder scan:** No "TODO", "TBD", "implement later" in the plan. Every code block is complete.

**3. Type/identifier consistency:** 
- `KLASIO_WHATSAPP_NUMBER` (constant name) consistent across Tasks 14, 17.
- `WA_MESSAGES` map keys (`nav_contact`, `nav_demo`, `hero_primary`, `how_setup`, `fab`) consistent between Task 14 definition and `data-wa-msg` attributes added in Tasks 3, 4, 9, 10, 11, 12, 13.
- `I18N` keys consistent between Task 14 dict and `data-i18n` / `data-i18n-html` attributes throughout.
- `applyTranslations(lang)`, `wireWhatsApp(lang)`, `detectLang()` function names match between definitions and call sites.
- CSS class names (`.btn-volt`, `.btn-outline`, `.btn-ghost`, `.tag`, `.fade-up`, `.section-title`, etc.) consistent between Task 2 definitions and section markup.

**4. Scope:** Single self-contained file, decomposed into 17 sequential tasks. No subsystem confusion.

---

## Plan Complete

Plan saved to `docs/superpowers/plans/2026-05-03-klasio-landing-page.md`.

**Two execution options:**

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
