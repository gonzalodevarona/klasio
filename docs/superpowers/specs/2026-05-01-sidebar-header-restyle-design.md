---
title: Sidebar Header Restyle
date: 2026-05-01
status: approved
---

# Sidebar Header Restyle

Pure CSS/style update — zero logic changes. Replaces the current flat brand block
in the sidebar header with a 3-row vertical layout and a role pill.

## Scope

Files touched:
- `tailwind.config.ts` — two token additions
- `messages/en.json` + `messages/es.json` — role label keys
- `web/src/components/layout/TenantBrand.tsx` — sizing only
- `web/src/components/layout/Sidebar.tsx` — Brand component JSX + mobile drawer header

No new files. No logic changes. No prop interface changes.

## 1. Tailwind config additions

```ts
// tailwind.config.ts → theme.extend
colors: {
  'k-volt-muted': 'rgba(202,255,77,0.1)',   // role pill background
},
fontFamily: {
  'k-mono': ['var(--font-mono)'],             // role pill font (wires existing CSS var)
},
```

## 2. i18n role labels

Added under the `"layout"` namespace in both locale files.

```json
// messages/en.json
"roleLabel": {
  "SUPERADMIN": "Superadmin",
  "ADMIN": "Admin",
  "MANAGER": "Manager",
  "PROFESSOR": "Professor",
  "STUDENT": "Student"
}

// messages/es.json
"roleLabel": {
  "SUPERADMIN": "Superadmin",
  "ADMIN": "Admin",
  "MANAGER": "Manager",
  "PROFESSOR": "Profesor",
  "STUDENT": "Estudiante"
}
```

Usage: `t(\`roleLabel.${role}\`)` via existing `t = useTranslations("layout")`.
next-intl's type checker requires a literal key path; use a type assertion
`t(\`roleLabel.${role}\` as Parameters<typeof t>[0])` or a small lookup object
`const ROLE_LABEL_KEYS = { SUPERADMIN: "roleLabel.SUPERADMIN", ... } as const`
to preserve type safety. Implementer chooses whichever satisfies `tsc --noEmit`.

## 3. TenantBrand.tsx — sizing changes only

| Element | Before | After |
|---|---|---|
| Loading skeleton | `h-6 w-32` | `h-4 w-28` |
| Logo img | `width={24} height={24}` | `width={18} height={18}` |
| Tenant name | `text-[18px] font-extrabold tracking-[-0.03em]` | `text-[13px] font-semibold` |

Props interface and conditional logic unchanged.

## 4. Sidebar.tsx — Brand component (desktop, expanded)

Replace current Brand JSX with 3-row vertical flex layout.

```
collapsed=true  → return null (unchanged)
collapsed=false →
  <div className="overflow-hidden min-w-0 flex flex-col gap-0">
    {/* Row 1 — logo lockup */}
    <KLogo size={28} />

    {/* Row 2 — tenant name (non-SUPERADMIN + !tenantFetchFailed only) */}
    {showTenantBrand && (
      <div className="mt-3">
        <TenantBrand tenantName={tenantName} tenantLogoUrl={tenantLogoUrl} loading={tenantLoading} />
      </div>
    )}

    {/* Row 3 — role pill (all roles) */}
    {role && (
      <span className="mt-2 self-start bg-k-volt-muted text-k-volt font-k-mono text-[9px] uppercase tracking-[0.12em] px-2 py-[3px] rounded-[4px]">
        {t(`roleLabel.${role}`)}
      </span>
    )}
  </div>
```

Removed: `<hr className="border-k-sidebar-active my-2" />`, old `<p className="text-[11px] text-k-subtle truncate">{role}</p>`.

`showTenantBrand` and `tenantLoading` use same logic as current `useTenantBrand`/`loading` locals.

## 5. Sidebar.tsx — Mobile drawer header

Replace the `{tenantBrandActive ? <TenantBrand ...> : <KLogo />} + <hr> + role <p>` block with identical 3-row structure:

```
<div className="overflow-hidden min-w-0 flex-1 flex flex-col gap-0">
  <KLogo size={28} />
  {tenantBrandActive && (
    <div className="mt-3">
      <TenantBrand tenantName={tenantName} tenantLogoUrl={tenantLogoUrl} loading={tenantBrandLoading} />
    </div>
  )}
  {primaryUserRole && (
    <span className="mt-2 self-start bg-k-volt-muted text-k-volt font-k-mono text-[9px] uppercase tracking-[0.12em] px-2 py-[3px] rounded-[4px]">
      {t(`roleLabel.${primaryUserRole}`)}
    </span>
  )}
</div>
```

Mobile top bar (`h-14`) — unchanged, no role pill added there.

## 6. Collapsed state

No change. `Brand` returns null when `collapsed=true`. KLogoMark centered via existing layout — unchanged.

## Visual outcome per role (expanded desktop + mobile drawer)

| Role | Row 1 | Row 2 | Row 3 |
|---|---|---|---|
| SUPERADMIN | KLogo lockup | — | "Superadmin" lime pill |
| ADMIN | KLogo lockup | Tenant name | "Admin" lime pill |
| MANAGER | KLogo lockup | Tenant name | "Manager" lime pill |
| PROFESSOR | KLogo lockup | Tenant name | "Professor" / "Profesor" lime pill |
| STUDENT | KLogo lockup | Tenant name | "Student" / "Estudiante" lime pill |

## Verification

- `npx tsc --noEmit` — zero errors
- Existing sidebar tests pass unchanged
