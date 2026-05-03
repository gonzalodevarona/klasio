# Klasio Marketing Landing Page — Design

**Status:** Approved
**Date:** 2026-05-03
**Owner:** @gonzalo
**Related:** none (standalone marketing asset, decoupled from app)

## Goal

Ship a production-ready marketing landing page for **klasio.club** — a single self-contained HTML file showcasing Klasio's value props and driving WhatsApp inquiries for demo/onboarding.

## Non-goals

- Not a Next.js page, not part of the `web/` app.
- No backend integration (no signup form, no analytics endpoint, no CMS).
- No A/B testing infra. No CI/CD pipeline yet.
- Not bilingual via routing (`/en`, `/es`) — single page with client-side i18n via `navigator.language`.

## Scope

One file: `klasio-landing/index.html` at repo root.

8 sections in fixed order: Nav → Hero → Value props → Features → How it works → Metrics → Final CTA → Footer. Plus floating WhatsApp button.

Two languages bundled in-page: **English (default)** + **Spanish** (auto-applied for `navigator.language.startsWith('es')`).

## Decisions

### D1 — File location: repo root `klasio-landing/`

**Why:** Marketing site has a different deploy cadence and likely a different domain (`klasio.club` vs `app.klasio.club`) than the Next.js app. Standalone static file deploys cheaply anywhere (Cloudflare Pages, Netlify, S3 + CloudFront). Decouples from the `web/` app's build/lint/CI. Spec requires `file://` double-click to work — implies zero framework coupling.

**Rejected:** placing under `web/public/landing/` (couples deploy and domain to the app), separate repo (overkill for a single file).

### D2 — Single HTML file, inline CSS, vanilla JS

**Why:** Spec mandates it. Benefits: zero build step, owner can edit copy directly, deployable to any static host, openable from local filesystem for previews.

**Constraints:** Only external dep = Google Fonts CDN link. All SVGs inline. All CSS in one `<style>` tag in `<head>`. All JS in one `<script>` tag before `</body>`.

### D3 — i18n: client-side, auto-detect, no toggle

**Why:** User requested EN as canonical default with auto-Spanish for Spanish-speaking visitors. Simplest impl that meets the requirement.

**Mechanism:**
- `const I18N = { en: { key: "..." }, es: { key: "..." } }` declared at top of `<script>`.
- HTML elements that contain translatable copy carry `data-i18n="key"` (or `data-i18n-html="key"` when value contains nested `<span>` markup like the volt-accented words).
- On `DOMContentLoaded`: `const lang = navigator.language?.toLowerCase().startsWith('es') ? 'es' : 'en';` → set `document.documentElement.lang = lang;` → loop over `[data-i18n]` and `[data-i18n-html]` swapping `textContent` / `innerHTML`.
- WhatsApp link `text=` query param is also translated (the prefilled message body).
- No language toggle button. No `localStorage` persistence.
- Static fallback: HTML ships with **English** as the literal default content, so users with JS disabled or pre-hydration see English (the canonical version).

**Rejected:** manual toggle (not requested, can be added later as a 1-line nav addition), localStorage (added complexity for marginal UX gain), URL-routed locales (would require server config or hash routing — overkill).

### D4 — WhatsApp number = single runtime config constant (not hardcoded in `href`s)

**Why:** Owner wants a single place to change the number in the future. Hardcoding into 8 separate `href` attributes means an HTML find-replace; a single config constant means changing one line.

**Mechanism:** Top of the `<script>` block declares the config object — this is the **only** place the number lives in the codebase:

```js
// === Klasio Landing — Runtime Config ===
// Override at deploy time (Cloudflare Pages / Netlify) by injecting a
// <script>window.KLASIO_CONFIG = { whatsappNumber: '...' }</script>
// before this script runs, OR edit this default value directly.
window.KLASIO_CONFIG = window.KLASIO_CONFIG || {};
const KLASIO_WHATSAPP_NUMBER = window.KLASIO_CONFIG.whatsappNumber || '573127061833';
```

**Default value:** `573127061833` (the user-provided `+573127061833` Colombia number, stripped of `+` for `wa.me` format).

**Env var name (deploy-time convention):** `KLASIO_WHATSAPP_NUMBER` — for use with hosting platforms that support build-time string substitution (Cloudflare Pages env vars, Netlify build envs). The substitution would inject a `<script>window.KLASIO_CONFIG = { whatsappNumber: '${KLASIO_WHATSAPP_NUMBER}' }</script>` snippet before the main script.

**WhatsApp link wiring (no literal `href`s in the markup):**
- Each WhatsApp `<a>` tag in the markup has `data-wa-msg="key"` and a placeholder `href="#"`.
- The script defines a small map: `const WA_MESSAGES = { 'hero_primary': { en: 'Hi, I want a free demo of Klasio', es: 'Hola, quiero una demo gratuita de Klasio' }, ... }`.
- On init: loop `[data-wa-msg]`, look up the message for the current `lang`, and set `href = \`https://wa.me/${KLASIO_WHATSAPP_NUMBER}?text=${encodeURIComponent(msg)}\``.
- Same loop also sets `target="_blank" rel="noopener"` (so the markup author can't forget).

**Result:** zero literal `wa.me` URLs anywhere in HTML. Number swap = edit one constant. Message tweaks = edit one map.

### D5 — Brand fidelity = exact

Colors, fonts, spacing, radii, button system, KLogoMark SVG — all match the app's design system verbatim per the spec's "Brand & Design System" block. Defined as CSS variables in `:root`. No deviations.

### D6 — Animations: minimal, accessible

- `.fade-up` + IntersectionObserver (threshold 0.12) for scroll-triggered reveals.
- Hero `.fade-up` elements unblock immediately on load (`setTimeout(100ms)`).
- Pulsing dot in hero tag pill (CSS keyframes only).
- WhatsApp button hover scale.
- `prefers-reduced-motion: reduce` disables the fade transitions and the pulse animation (additive — kept as a media-query block at the bottom of `<style>`).

## Architecture

```
klasio-landing/
└── index.html        ← single file, ~1500 lines
```

**Inside `index.html`:**

```
<head>
  <meta> — viewport, charset, title, description, OG tags
  <link> — Google Fonts (DM Sans + DM Mono)
  <style> — all CSS:
    :root vars (palette + radii + typography)
    Reset/base
    Components: .nav, .btn-volt, .btn-outline, .btn-ghost, .tag-pill,
                .section-title, .fade-up
    Sections: .hero, .value-grid, .feature-grid, .how-it-works,
              .metrics-grid, .final-cta, .footer
    Decorative: .noise-overlay, .volt-glow, .wa-fab
    Animations: @keyframes pulse, fade transitions
    Media queries: @media (max-width: 900px), 768px, 560px;
                   @media (prefers-reduced-motion: reduce)
</head>

<body>
  <nav> — fixed top, logo + links + CTA buttons
  <section class="hero" id="hero">
  <section class="value" id="valor">
  <section class="features" id="funcionalidades">
  <section class="how-it-works" id="como-funciona">
  <section class="metrics" id="metricas">
  <section class="final-cta" id="cta">
  <footer>
  <a class="wa-fab" target="_blank" rel="noopener">…WhatsApp SVG…</a>

  <script>
    const I18N = { en: {…}, es: {…} };
    // detect lang, apply translations, init IntersectionObserver
  </script>
</body>
```

## Section-by-Section Spec

The visual + structural spec for all 8 sections, the floating WhatsApp button, the design tokens, the SVG icon paths, the dashboard mockup, and the animation behaviour is defined verbatim in the brainstorming brief. **Treat that brief as the source of truth for every pixel-level detail.** This design doc only captures the architecture-level decisions; the implementation plan will translate the brief into ordered tasks.

Key reference points from the brief:
- Section 1 (Nav): fixed, blurred, 64px height
- Section 2 (Hero): split grid, copy left + dashboard mockup right, hides mockup on `<900px`
- Section 3 (Value Props): 2×2 bordered grid, 4 cards
- Section 4 (Features): 3-col grid, 9 cards, first card `.featured`
- Section 5 (How it works): 4-step list left + roles card right
- Section 6 (Metrics): 4-col grid, 1px gaps for hairline divider effect
- Section 7 (Final CTA): centered, decorative volt glow
- Section 8 (Footer): minimal flex row

## Copy — English (canonical) + Spanish

All copy below is the **canonical EN** + **ES translation**. Implementer must produce both sets and inject via `data-i18n` keys. Spec-provided Spanish copy is preserved as-is for ES; EN translations are added below.

### Nav
| key | EN | ES |
|---|---|---|
| nav.benefits | Benefits | Beneficios |
| nav.features | Features | Funcionalidades |
| nav.how | How it works | Cómo funciona |
| nav.contact | Contact | Contactar |
| nav.demo | See demo → | Ver demo → |

### Hero
| key | EN | ES |
|---|---|---|
| hero.tag | Professional sports management | Gestión deportiva profesional |
| hero.h1.line1 | Your club, | Tu club, |
| hero.h1.line2 | managed | administrado |
| hero.h1.line3 | with `<span>precision.</span>` | con `<span>precisión.</span>` |
| hero.subtitle | Klasio is the all-in-one platform for sports clubs. Manage memberships, attendance, payments and programs from a single place — no spreadsheets, no chaos. | Klasio es la plataforma todo-en-uno para clubes deportivos. Controla membresías, asistencias, pagos y programas desde un solo lugar — sin planillas, sin caos. |
| hero.cta.primary | Request free demo → | Solicitar demo gratuita → |
| hero.cta.secondary | See features ↓ | Ver funcionalidades ↓ |
| hero.stat1.value | 100% | 100% |
| hero.stat1.label | Digital | Digital |
| hero.stat2.value | 5 | 5 |
| hero.stat2.label | User roles | Roles de usuario |
| hero.stat3.value | ∞ | ∞ |
| hero.stat3.label | Students | Estudiantes |

### Hero dashboard mockup (translated for visual coherence)
| key | EN | ES |
|---|---|---|
| mock.dashboard | Dashboard | Dashboard |
| mock.subtitle | NORTH LEAGUE · CURRENT PERIOD | LIGA NORTE · PERÍODO ACTUAL |
| mock.kpi1.label | STUDENTS | ESTUDIANTES |
| mock.kpi1.sub | ↑ 12 this month | ↑ 12 este mes |
| mock.kpi2.label | HOURS USED | HORAS CONSUMIDAS |
| mock.kpi3.label | PENDING PAYMENTS | PAGOS PENDIENTES |
| mock.attendance.title | Attendance control | Control de asistencia |
| mock.attendance.cta | Start class | Iniciar clase |
| mock.row1.program | Advanced Swimming | Natación Avanzado |
| mock.row1.status | Active | Activo |
| mock.row2.program | Youth Karate | Karate Juvenil |
| mock.row2.status | Expiring | Por vencer |
| mock.row3.program | Basketball | Basketball |
| mock.row3.status | Active | Activo |
| mock.row4.program | Track & Field | Atletismo Base |
| mock.row4.status | Inactive | Inactivo |
*(Student names are kept in original Spanish — proper nouns, not translated.)*

### Value Props
| key | EN | ES |
|---|---|---|
| value.tag | Why Klasio | Por qué Klasio |
| value.title | Stop running your club on spreadsheets. | Deja de administrar con planillas. |
| value.sub | Club owners lose hours every week on tasks Klasio handles in seconds. | Los dueños de clubes pierden horas cada semana en tareas que Klasio resuelve en segundos. |
| value.01.title | Real-time attendance control | Control de asistencia en tiempo real |
| value.01.body | Mark attendance straight from the app. Know who showed up, who skipped, and how many hours each student has left — no Excel sheets. | Marcá la asistencia directamente desde la app. Sabé quién vino, quién faltó y cuántas horas le quedan a cada estudiante — sin planillas Excel. |
| value.02.title | Memberships and payments under control | Membresías y pagos bajo control |
| value.02.body | Manage plans, expirations, and payment proofs in one place. Get automatic alerts before a membership expires. | Gestioná planes, vencimientos y comprobantes de pago en un solo lugar. Recibí alertas automáticas antes de que una membresía venza. |
| value.03.title | Your students, organized | Tus estudiantes, organizados |
| value.03.body | Every student's full record in a single profile: enrollments, level, class history, and hour movements. Accessible in seconds. | Toda la información de cada alumno en un perfil: inscripciones, nivel, historial de clases y movimientos de horas. Accesible en segundos. |
| value.04.title | Decisions backed by data | Decisiones basadas en datos |
| value.04.body | Dashboard with key metrics: hours consumed, attendance rate, pending payments, active programs. The whole club at a glance. | Dashboard con métricas clave: horas consumidas, tasa de asistencia, pagos pendientes, programas activos. Toda la foto del club de un vistazo. |

### Features
| key | EN | ES |
|---|---|---|
| feat.tag | Features | Funcionalidades |
| feat.title | Everything your club needs. | Todo lo que tu club necesita. |
| feat.sub | A complete platform without unnecessary complexity. | Una plataforma completa, sin complejidad innecesaria. |
| feat.featured.badge | Featured | Destacado |
| feat.1.title | Admin dashboard | Dashboard de administración |
| feat.1.body | 360° view of your club. Active students, hours consumed, pending payments, and attendance control with status filters. | Vista 360° del club. Estudiantes activos, horas consumidas, pagos pendientes y control de asistencia con filtros por estado. |
| feat.2.title | Student management | Gestión de estudiantes |
| feat.2.body | Sign-up, deactivation, full profile, membership history, class enrollments, and per-program level tracking. | Alta, baja, perfil completo, historial de membresías, inscripciones a clases y seguimiento de nivel por programa. |
| feat.3.title | Attendance control | Control de asistencia |
| feat.3.body | Mark presence or absence per session. The system auto-deducts hours and keeps the full history. | Marcá presencia o ausencia por sesión. El sistema descuenta horas automáticamente y registra todo el historial. |
| feat.4.title | Memberships and plans | Membresías y planes |
| feat.4.body | Build custom plans per program. Hour allocation, expiry dates, and automatic alerts included. | Creá planes personalizados por programa. Asignación de horas, fechas de vencimiento y alertas automáticas incluidas. |
| feat.5.title | Payment proofs | Comprobantes de pago |
| feat.5.body | Students upload their payment proof from the app. The admin approves or rejects directly — no more WhatsApp back-and-forth. | Los estudiantes suben su comprobante desde la app. El admin aprueba o rechaza directamente, sin idas y vueltas por WhatsApp. |
| feat.6.title | Smart notifications | Notificaciones inteligentes |
| feat.6.body | Alerts for expiring memberships, cancelled classes, new payments, and more. Always informed, no manual checking. | Alertas de membresías por vencer, clases canceladas, pagos nuevos y más. Siempre informado, sin revisar manualmente. |
| feat.7.title | Programs and classes | Programas y clases |
| feat.7.body | Organize your offering into programs with multiple classes. Set schedules, capacity limits, and required levels. | Organizá tu oferta deportiva en programas con múltiples clases. Definí horarios, cupos máximos y nivel requerido. |
| feat.8.title | Coach management | Gestión de profesores |
| feat.8.body | Assign coaches to classes, manage their profiles, and give them their own access to mark attendance. | Asigná profesores a clases, gestioná su perfil y dales acceso propio para que marquen asistencia. |
| feat.9.title | Student app | App para estudiantes |
| feat.9.body | Your students see available classes, register for sessions, check remaining hours, and upload payment proofs — all from their phone. | Tus alumnos ven sus clases disponibles, se registran a sesiones, consultan horas restantes y suben comprobantes — todo desde el celular. |

### How it works
| key | EN | ES |
|---|---|---|
| how.tag | How it works | Cómo funciona |
| how.title | Up and running in less than a day. | En marcha en menos de un día. |
| how.sub | No installs, no complex setup. Your club operating digitally starting today. | Sin instalaciones, sin configuraciones complejas. Tu club operando digitalmente desde hoy. |
| how.1.title | We create your club in Klasio | Creamos tu club en Klasio |
| how.1.body | We give you admin access and configure your tenant with your club's data. | Te damos acceso como administrador y configuramos tu tenant con los datos de tu club. |
| how.2.title | You load your programs and plans | Cargás tus programas y planes |
| how.2.body | Define your disciplines, classes, schedules, and membership plans. Quick and uncomplicated. | Definís tus disciplinas, clases, horarios y planes de membresía. Rápido y sin complicaciones. |
| how.3.title | Invite your team and students | Invitás a tu equipo y estudiantes |
| how.3.body | Coaches and students get their access. Each role sees exactly what they need. | Profesores y alumnos reciben su acceso. Cada rol ve exactamente lo que necesita. |
| how.4.title | Run your club with precision | Operás tu club con precisión |
| how.4.body | Attendance, payments, reports, notifications — everything centralized, everything under control. | Asistencias, pagos, reportes, notificaciones — todo centralizado, todo bajo control. |
| how.cta | Get started → | Empezar ahora → |
| roles.title | Roles in Klasio | Roles en Klasio |
| roles.admin.name | Administrator | Administrador |
| roles.admin.desc | Full control: students, payments, reports, config | Control total: estudiantes, pagos, reportes, config |
| roles.admin.badge | Admin | Admin |
| roles.coach.name | Coach | Profesor |
| roles.coach.desc | Marks attendance, sees their class roster | Marca asistencia, ve el roster de sus clases |
| roles.coach.badge | Coach | Profesor |
| roles.student.name | Student | Estudiante |
| roles.student.desc | Checks classes, hours, uploads proofs | Consulta clases, horas, sube comprobantes |
| roles.student.badge | Student | Estudiante |
| roles.manager.name | Manager | Manager |
| roles.manager.desc | Operational control without config access | Gestión operativa sin acceso a configuración |
| roles.manager.badge | Manager | Manager |

### Metrics
| key | EN | ES |
|---|---|---|
| metrics.tag | Results | Resultados |
| metrics.title | The impact of digitizing your club. | El impacto de digitalizar tu club. |
| metrics.cta | Request demo → | Solicitar demo → |
| metrics.1.label | Less time on admin | Menos tiempo en admin |
| metrics.2.label | Spreadsheets needed | Planillas necesarias |
| metrics.3.label | Students supported | Estudiantes soportados |
| metrics.4.label | Access from anywhere | Acceso desde cualquier lugar |

### Final CTA
| key | EN | ES |
|---|---|---|
| cta.tag | Ready to get started? | ¿Listo para empezar? |
| cta.title | Your club deserves `<span>professional</span>` tools. | Tu club merece herramientas `<span>profesionales.</span>` |
| cta.sub | Let's chat on WhatsApp. We'll show you Klasio in action and configure it for your club the same day. | Hablemos por WhatsApp. Te mostramos Klasio en acción y lo configuramos para tu club en el mismo día. |
| cta.button | Contact via WhatsApp | Contactar por WhatsApp |
| cta.note | No commitments · Reply in under 1 hour | Sin compromisos · Respuesta en menos de 1 hora |

### Footer
| key | EN | ES |
|---|---|---|
| footer.benefits | Benefits | Beneficios |
| footer.features | Features | Funcionalidades |
| footer.contact | Contact | Contacto |
| footer.copy | © 2026 klasio.club · All rights reserved | © 2026 klasio.club · Todos los derechos reservados |

### Document metadata (also translated)
| key | EN | ES |
|---|---|---|
| meta.title | Klasio — Sports club management software | Klasio — Software de gestión para clubes deportivos |
| meta.description | All-in-one platform to manage memberships, attendance, payments and programs for your sports club. No spreadsheets, no chaos. | Plataforma todo-en-uno para gestionar membresías, asistencias, pagos y programas de tu club deportivo. Sin planillas, sin caos. |
| meta.og.title | Klasio — Sports club management software | Klasio — Software de gestión para clubes deportivos |
| meta.og.description | The professional way to run your sports club. | La forma profesional de administrar tu club deportivo. |

The i18n script must update `<title>` (`document.title = I18N[lang]['meta.title']`) and the `<meta name="description">` / OG meta tags via `document.querySelector('meta[name="description"]').content = …` etc., in addition to swapping `[data-i18n]` text nodes.

### WhatsApp prefilled messages — `WA_MESSAGES` map

The `data-wa-msg="<key>"` attribute on each `<a>` selects the message key from `WA_MESSAGES`. The script picks the variant matching the active locale.

| `data-wa-msg` key | Used by | EN | ES |
|---|---|---|---|
| `nav_contact` | Nav "Contact" button + footer "Contact" link | Hi, I want to learn more about Klasio | Hola, quiero saber más sobre Klasio |
| `nav_demo` | Nav "See demo →" button + Metrics section CTA | Hi, I want a demo of Klasio | Hola, quiero una demo de Klasio |
| `hero_primary` | Hero "Request free demo →" CTA + Final CTA button | Hi, I want a free demo of Klasio | Hola, quiero una demo gratuita de Klasio |
| `how_setup` | How-it-works "Get started →" CTA | Hi, I want to set up my club on Klasio | Hola, quiero configurar mi club en Klasio |
| `fab` | Floating WhatsApp button | Hi, I want to learn more about Klasio | Hola, quiero saber más sobre Klasio |

The init script: `for each [data-wa-msg]: const key = el.dataset.waMsg; el.href = \`https://wa.me/${KLASIO_WHATSAPP_NUMBER}?text=${encodeURIComponent(WA_MESSAGES[key][lang])}\`; el.target = '_blank'; el.rel = 'noopener';`

## Accessibility

- `<html lang="en">` initially, swapped by JS based on detected locale.
- Semantic HTML: `<nav>`, `<section>`, `<footer>`, `<h1>`, `<h2>`, `<h3>`.
- Decorative SVGs marked `aria-hidden="true"`.
- Functional SVGs (WhatsApp fab, button arrows) get `aria-label` on the parent `<a>`/`<button>`.
- All anchor CTAs have visible text; icons are decorative.
- Color contrast: text on `--dark` (#FAFAF8 on #0A0A0A) and `--volt` text on volt bg (#2A4A00 on #CAFF4D) both clear AAA.
- `prefers-reduced-motion: reduce` disables fade-in animations and the pulsing dot.
- Focus styles preserved on all interactive elements (do not `outline: none` without a visible replacement).

## SEO / metadata

- `<title>`: "Klasio — Sports club management software" (EN) / set by JS for ES locale.
- `<meta name="description">`: short pitch (~150 chars), translated.
- `<html lang>`: dynamic.
- Open Graph: `og:title`, `og:description`, `og:type=website`, `og:url=https://klasio.club`. (No `og:image` for v1 — can be added later.)
- `<link rel="canonical" href="https://klasio.club">`.

## Performance

- Single file, ~50–80 KB total uncompressed.
- One external request: Google Fonts CSS (and the font files it pulls).
- `<link rel="preconnect" href="https://fonts.googleapis.com">` + `<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>` to speed up font fetch.
- No images. All icons inline SVG. Dashboard mockup is pure CSS/HTML.
- IntersectionObserver runs once per element (disconnects after first reveal).

## Browser support

Modern evergreen (Chrome/Edge/Firefox/Safari current + 2 prior). No IE.

`backdrop-filter` (used in nav) and `clamp()` (used in typography) both supported in Safari 14+, which is the floor.

## Quality checklist (must verify before shipping)

- [ ] File opens by double-click (`file://` protocol) — no broken paths, no CORS-blocked fetches.
- [ ] Visual check at 1440 / 1024 / 768 / 375 px widths.
- [ ] All WhatsApp `<a>` tags use `data-wa-msg="..."` (no literal `wa.me` URLs in HTML); on load they resolve to `https://wa.me/${KLASIO_WHATSAPP_NUMBER}?text=...` with `target="_blank" rel="noopener"`.
- [ ] Changing `KLASIO_WHATSAPP_NUMBER` (or injecting `window.KLASIO_CONFIG.whatsappNumber`) updates every WhatsApp link on the page.
- [ ] All 8 sections have correct `id` for anchor nav.
- [ ] No `console.error` / `console.warn` in browser devtools on load.
- [ ] HTML validates (no unclosed tags).
- [ ] Lighthouse mobile: Performance ≥ 95, Accessibility ≥ 95, Best Practices ≥ 95, SEO ≥ 95.
- [ ] i18n: visiting with browser locale `es-CO` / `es-ES` shows Spanish; locale `en-US` shows English.
- [ ] `prefers-reduced-motion: reduce` disables fade animations.
- [ ] No JS errors with JS disabled (page degrades to static English).

## Out of scope (deferred)

- Manual language toggle in nav.
- `localStorage` persistence of language choice.
- Open Graph hero image.
- Cookie banner / GDPR consent.
- Analytics integration (GA4, Plausible, etc.).
- A/B testing of headlines.
- Dark/light mode toggle (page is dark-only by design).
- Server-side i18n via `Accept-Language` header.
- Sub-pages (pricing, blog, legal) — single-page only for v1.

## Risks / open questions

- **Domain not provisioned yet.** `klasio.club` must be registered + DNS pointed at chosen static host before launch. Not a blocker for building the file.
- **WhatsApp number rate limits.** A single number receiving demo requests may hit WhatsApp Business API limits at scale. Acceptable for v1 (manual sales).
- **i18n via `navigator.language` is imperfect.** A Spanish speaker on an English-set browser sees English. Mitigation: a manual toggle is a 1-line addition if/when needed.
