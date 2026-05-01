# Sidebar Tenant Branding

**Date:** 2026-05-01
**Scope:** Frontend personalization — replace generic Klasio branding in the sidebar with the user's tenant identity (logo + name) once authenticated. Remove the redundant role label from the user footer.

## Problem

Today the sidebar shows a fixed "Klasio" wordmark in three places (desktop expanded brand, mobile top bar, mobile drawer header) regardless of which league the user belongs to. League managers, professors, and students see no visual signal that the platform is "their" league. Additionally, the user's role appears twice in the sidebar — once at the top inside `Brand`, once at the bottom inside `UserFooter` — which is redundant.

## Goals

1. Tenant-scoped users (ADMIN, MANAGER, PROFESSOR, STUDENT) see their tenant's logo and name in place of the Klasio wordmark, in all three sidebar surfaces.
2. SUPERADMIN keeps the Klasio brand (no tenant context).
3. Role label appears only once (top of sidebar in `Brand`), not in `UserFooter`.

## Non-goals

- Tenant logo upload/edit flow — already exists via `/tenants/{id}` admin form.
- Multi-tenant switching for users with cross-tenant access — out of scope; `primaryRole` already resolves a single tenant per session.
- Theming beyond logo/name (no color customization, no custom CSS).

## Backend changes

### `MeTenantController` extended response

**File:** `api/src/main/java/com/klasio/tenant/infrastructure/web/MeTenantController.java`

- Inject `LogoStorage` port (already used by `GetTenantDetailService` and `TenantController`).
- Extend response record:
  ```java
  record TenantInfoResponse(
      UUID id,
      String name,
      String discipline,
      String language,
      String logoUrl   // NEW — presigned URL or null
  ) {}
  ```
- In the handler: if `tenant.getLogoKey() != null`, generate presigned URL via `logoStorage.generatePresignedUrl(tenant.getLogoKey())`. Else `logoUrl = null`.

No new endpoint, no new permission, no new migration. RBAC unchanged (`hasAnyRole('ADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT')`).

### Backend tests

**File:** `api/src/test/java/com/klasio/tenant/infrastructure/web/MeTenantControllerTest.java` (new or extend)

- Tenant with logoKey → response contains non-null logoUrl matching presigned URL.
- Tenant with null logoKey → response logoUrl is null.
- SUPERADMIN call → 403 (already covered by RBAC).

## Frontend changes

### `useSidebarIdentity` extension

**File:** `web/src/hooks/useSidebarIdentity.ts`

- Extend `SidebarIdentity` interface: add `tenantLogoUrl: string | null`.
- Extend `TenantInfo` local interface: add `logoUrl: string | null`.
- Update `/api/me/tenant` consumer to read `data.logoUrl` and store via new `tenantLogoUrl` state.
- Add `tenantFetchFailed: boolean` state — set to `true` when fetch resolves with non-OK response or rejects. Used by `Brand` to fall back to Klasio brand after a failed network call (graceful degrade).

### New `TenantBrand` component

**File:** `web/src/components/layout/TenantBrand.tsx` (new)

```tsx
interface TenantBrandProps {
  tenantName: string | null;
  tenantLogoUrl: string | null;
  loading: boolean;     // true until name resolves OR fetch fails
}
```

Behavior:
- `loading === true` → render skeleton: `<div className="h-6 w-32 bg-k-sidebar-active rounded animate-pulse" />`.
- `loading === false && tenantName != null && tenantLogoUrl != null` → render `<img src={tenantLogoUrl} alt="" width={24} height={24} />` + `<span>{tenantName}</span>`.
- `loading === false && tenantName != null && tenantLogoUrl == null` → render `<span>{tenantName}</span>` only (no img per Q2 → A).
- `loading === false && tenantName == null` (fetch failed) → caller falls back to `<KLogo />`; `TenantBrand` is not rendered in this branch.

Layout matches `KLogo`: `flex items-center gap-2 select-none`, name styled `text-[18px] font-extrabold text-white tracking-[-0.03em] leading-none`.

### `Sidebar.tsx` — `Brand` rewrite

**File:** `web/src/components/layout/Sidebar.tsx`

Replace the existing `Brand` component with a role-aware version:

```tsx
function Brand({
  role,
  tenantName,
  tenantLogoUrl,
  tenantFetchFailed,
  collapsed,
}: {
  role: Role | undefined;
  tenantName: string | null;
  tenantLogoUrl: string | null;
  tenantFetchFailed: boolean;
  collapsed: boolean;
}) {
  if (collapsed) return null;

  const useTenantBrand =
    role !== undefined && role !== "SUPERADMIN" && !tenantFetchFailed;
  const loading = useTenantBrand && tenantName == null;

  return (
    <div className="overflow-hidden min-w-0">
      {useTenantBrand ? (
        <TenantBrand
          tenantName={tenantName}
          tenantLogoUrl={tenantLogoUrl}
          loading={loading}
        />
      ) : (
        <KLogo />
      )}
      <hr className="border-k-sidebar-active my-2" />
      {role && <p className="text-[11px] text-k-subtle truncate">{role}</p>}
    </div>
  );
}
```

Key effects:
- The old `tenantName` `<p>` line below the divider is **removed** (now lives inside `TenantBrand`).
- The role line below the divider stays — single source of truth for role.
- SUPERADMIN sees Klasio + role, exactly as before.

### `UserFooter` change

**File:** `web/src/components/layout/Sidebar.tsx` (lines 205–256)

Remove `<p className="text-xs text-k-subtle truncate">{role}</p>` (currently line 234). Keep displayName, ID document line, logout button. The `role` prop becomes unused — drop it from the props signature.

### Mobile top bar

**File:** `web/src/components/layout/Sidebar.tsx` (lines 317–334)

Replace the inline `<KLogo />` + tenant span with role-aware rendering:

```tsx
<div className="min-w-0 flex-1">
  {primaryUserRole && primaryUserRole !== "SUPERADMIN" && !tenantFetchFailed ? (
    <TenantBrand
      tenantName={tenantName}
      tenantLogoUrl={tenantLogoUrl}
      loading={tenantName == null}
    />
  ) : (
    <KLogo />
  )}
</div>
```

The legacy `tenantName` `<span>` after `<KLogo />` is dropped (redundant once tenant brand replaces logo).

### Mobile drawer header

**File:** `web/src/components/layout/Sidebar.tsx` (lines 346–365)

Same role-aware swap as desktop `Brand`. Drop the now-redundant `tenantName` `<p>` below the divider; keep the role `<p>` (single source).

## Data flow

```
Sidebar mount
  → useAuth (resolves user, roles, tenantId)
  → useSidebarIdentity(primaryRole, tenantId)
    → if role !== "SUPERADMIN" && tenantId
        → fetch /api/me/tenant
          → { name, logoUrl } | error → tenantFetchFailed = true
    → fetch /api/me/{role}-profile (existing) → { firstName, lastName, document }
  → Brand decides between TenantBrand and KLogo per role + fetch state
  → UserFooter renders name + document + logout (role removed)
```

## Edge cases

| Case | Behavior |
|---|---|
| SUPERADMIN | Never fetches `/me/tenant`; Klasio brand always. |
| Tenant fetch in flight | Skeleton placeholder in `TenantBrand`. |
| Tenant fetch fails (timeout/500) | `tenantFetchFailed` flips, Brand falls back to `KLogo` (graceful degrade per Q3 fallback → A). |
| Tenant has no logo (`logoKey` null) | `TenantBrand` renders name only, no img. |
| Tenant name present but logoUrl null mid-session | Same as above; no special UI. |
| Presigned URL expires during long session | Sidebar fetches fresh URL on next mount/route reload. No proactive refresh in v1; acceptable since URL TTL is hours, not minutes. Re-evaluate if support reports broken images. |
| Multi-role user (ADMIN + STUDENT) | `primaryRole` already resolves to single role upstream — tenant brand applies. |
| Collapsed sidebar | `Brand` returns `null` (existing behavior); icon-only nav. Tenant logo not shown — would not fit 16px column. |

## Testing

### Backend

- `MeTenantControllerTest`:
  - Authenticated ADMIN with tenant having logoKey → response logoUrl is presigned URL string.
  - Authenticated MANAGER with tenant having null logoKey → response logoUrl is null.
  - Verify `LogoStorage.generatePresignedUrl` invoked exactly once with correct key.

### Frontend

- `useSidebarIdentity.test.ts`:
  - SUPERADMIN role → no `/me/tenant` fetch.
  - Tenant role → `/me/tenant` fetch, populates name + logoUrl.
  - Fetch failure → `tenantFetchFailed` true, name/logoUrl null.
- `TenantBrand.test.tsx` (new):
  - Loading state → skeleton present.
  - Name + logoUrl → img + name rendered.
  - Name only → name rendered, no img.

### Manual

- Login as ADMIN of "Acme League" with logo → desktop sidebar header shows Acme logo + "Acme League".
- Login as STUDENT of same league → same brand, role line below shows "STUDENT".
- Login as SUPERADMIN → Klasio wordmark unchanged.
- Verify mobile top bar and drawer header match.
- Verify role no longer appears at sidebar bottom (only at top).

## Risks / open considerations

- **Presigned URL latency on every Sidebar mount.** `LogoStorage.generatePresignedUrl` is a fast in-process call (S3 SDK URL signing, no network). Acceptable.
- **CSP / image src trust.** Presigned URLs point to the same S3 bucket already used for tenant logo previews in the tenant detail page; CSP `img-src` already permits this origin.
- **Branch:** continues current `feature/full-redesign` work or new branch? To be decided at planning time.
