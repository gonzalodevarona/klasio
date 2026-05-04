# Email header tenant branding + subject leak fix

**Date**: 2026-05-03
**Scope**: `com.klasio.email` module + tenant logo storage
**Owner**: Backend

## Problem

Two defects in transactional email rendering:

1. **Subject text leaks into email body.** Recipients see the email subject duplicated as bare text above the content card. Affects all 9 email templates.
2. **Header always shows Klasio branding.** Tenant emails should display the tenant's logo and name in the header. Klasio branding should appear only in system-level emails (no tenant context).

## Root cause

### Bug #1 — subject leak

Each template's `<head>` declares the subject fragment as:

```html
<th:block th:fragment="subject" th:text="#{xxx.subject}"></th:block>
```

`ThymeleafTemplateRenderer` (line 30) extracts the subject by rendering only that fragment. Line 31 then renders the full document for the HTML body. In the full render, Thymeleaf strips the `<th:block>` tag but keeps its text content as a bare text node in `<head>`. Email clients (Gmail, Outlook) normalize HTML and reflow the orphan text into the visible body, producing the duplicated-subject artifact.

### Bug #2 — hardcoded Klasio header

`api/src/main/resources/email-templates/*.html` (lines 14–32 of each template) hardcodes:

- Lime-green 32×32 rounded square badge with the Klasio SVG mark
- "Klasio" wordmark text

There is no branch for tenant branding. `TenantContext` does not expose a logo URL. `LogoStorage` exposes only `generatePresignedUrl()` (short-lived, breaks for emails opened later).

All current `EmailType`s are tenant-scoped. No system-level email type exists today; the design must template-support state 3 (system) but defer the no-tenant code path.

## Goals

- Eliminate subject text from rendered HTML body across all templates.
- Render the tenant's logo (when uploaded) and tenant name in the email header for tenant-scoped emails.
- Render Klasio branding (logo + wordmark) only when no tenant context is supplied.
- Keep the "via Klasio" footer attribution and copyright (per product decision).

## Non-goals

- CloudFront / CDN distribution for tenant logos. Public S3 URL is sufficient for v1; a CDN can be layered later without changing email code.
- Adding new system-level email types. The template will support state 3, but no `send(...)` overload without `tenantId` is added until the first system email type lands.
- i18n message-key refactors.
- Footer copy changes. Footer keeps "Sent by {tenantName} via Klasio · © 2026 Klasio".
- Sender display name change. Stays `"{tenantName} via Klasio"`.

## Design

### Header states

| State | Trigger | Header rendering |
|---|---|---|
| 1 | Tenant present, `logo_key` not null | Tenant logo image (32×32, `object-fit: cover`) + tenant name text |
| 2 | Tenant present, `logo_key` null | Klasio lime badge + Klasio SVG mark + tenant name text |
| 3 | No tenant (system email) | Klasio lime badge + Klasio SVG mark + "Klasio" wordmark |

State 3 is template-supported but has no caller in v1.

### Subject leak fix

Replace in all 9 templates:

```html
<th:block th:fragment="subject" th:text="#{xxx.subject}"></th:block>
```

with:

```html
<title><th:block th:fragment="subject" th:text="#{xxx.subject}"></th:block></title>
```

The `<th:block>` (and its fragment definition) is wrapped inside a real `<title>` element. In the full document render, Thymeleaf strips the `<th:block>` tag but keeps its text content — that text now sits **inside `<title>`**, which is a standard `<head>` element never rendered visibly by any email client. The visible-text leak is gone.

Fragment-only extraction (`ThymeleafTemplateRenderer.render(...)` line 30) still returns just the inner text (the `<title>` wrapper is outside the fragment scope), so `RenderedTemplate.subject` is unchanged. No renderer code change required.

### Header refactor

Extract the header (current lines 14–32 of each template) into a shared Thymeleaf fragment at:

```
api/src/main/resources/email-templates/layouts/header.html
```

The fragment contains:

```html
<table th:fragment="header" role="presentation" cellpadding="0" cellspacing="0">
  <tr>
    <td style="background:#0A0A0A;padding:28px 40px;">
      <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
        <tr>
          <!-- State 1: tenant logo -->
          <td th:if="${tenantLogoUrl}" style="vertical-align:middle;">
            <img th:src="${tenantLogoUrl}"
                 width="32" height="32"
                 style="display:block;width:32px;height:32px;border-radius:8px;object-fit:cover;"
                 alt=""/>
          </td>
          <!-- States 2 & 3: Klasio badge -->
          <td th:unless="${tenantLogoUrl}" style="vertical-align:middle;">
            <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/>
              </svg>
            </div>
          </td>
          <td style="vertical-align:middle;padding-left:10px;">
            <!-- States 1 & 2: tenant name; State 3: Klasio -->
            <span th:text="${tenantName != null ? tenantName : 'Klasio'}"
                  style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
```

Each of the 9 templates replaces the inline header (current lines 14–32) with:

```html
<th:block th:replace="~{layouts/header :: header}"></th:block>
```

### Domain + storage changes

#### `TenantContext` (record)

```java
public record TenantContext(
    UUID id,
    String slug,
    String name,
    String language,
    String timezone,
    String logoUrl   // NEW; nullable
) {}
```

#### `LogoStorage` (port)

Add:

```java
String getPublicUrl(String logoKey);
```

Returns `null` when `logoKey` is `null`. Returns the stable public S3 URL otherwise.

#### `S3LogoStorage`

Implements `getPublicUrl(...)` using AWS SDK's `S3Utilities.getUrl(GetUrlRequest)`. The SDK auto-builds the correct URL for the configured endpoint (production AWS or LocalStack), so the same code works in dev and prod:

```java
@Override
public String getPublicUrl(String logoKey) {
    if (logoKey == null) return null;
    return s3Client.utilities().getUrl(GetUrlRequest.builder()
            .bucket(bucket)
            .key(logoKey)
            .build()).toString();
}
```

Does not generate a signed URL — relies on the bucket policy granting public read on the `logos/*` prefix.

#### `JpaTenantContextAdapter`

JPQL projection adds `t.logoKey`. After projection, calls `logoStorage.getPublicUrl(logoKey)` to produce the URL field on `TenantContext`. Constructor injects `LogoStorage`.

#### `EmailDispatcherService.send(...)`

After resolving `TenantContext`, populate:

```java
model.put("tenantLogoUrl", tenant.logoUrl()); // may be null
```

`tenantName` is already in the model.

### Bucket policy (ops, separate task)

Grant public read on the tenant-logos bucket prefix only:

```json
{
  "Effect": "Allow",
  "Principal": "*",
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::<bucket>/logos/*"
}
```

Other prefixes (payment proofs, etc.) remain private.

## Affected files

### Backend (Java)

- `api/src/main/java/com/klasio/email/domain/model/TenantContext.java` — add `logoUrl`
- `api/src/main/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapter.java` — fetch `logo_key`, resolve via `LogoStorage`
- `api/src/main/java/com/klasio/email/infrastructure/service/EmailDispatcherService.java` — put `tenantLogoUrl` in model
- `api/src/main/java/com/klasio/tenant/domain/port/LogoStorage.java` — add `getPublicUrl(String)`
- `api/src/main/java/com/klasio/tenant/infrastructure/storage/S3LogoStorage.java` — implement `getPublicUrl`

### Templates (HTML + TXT)

- `api/src/main/resources/email-templates/layouts/header.html` — NEW fragment
- `api/src/main/resources/email-templates/account-setup.html`
- `api/src/main/resources/email-templates/professor-invitation.html`
- `api/src/main/resources/email-templates/password-recovery.html`
- `api/src/main/resources/email-templates/payment-proof-uploaded.html`
- `api/src/main/resources/email-templates/payment-rejected.html`
- `api/src/main/resources/email-templates/membership-activated.html`
- `api/src/main/resources/email-templates/membership-expiry-warning.html`
- `api/src/main/resources/email-templates/membership-depleted.html`
- `api/src/main/resources/email-templates/class-session-change.html`
- `api/src/main/resources/email-templates/missing-template-fallback.html`

For each: replace `<th:block th:fragment="subject">` with `<title th:fragment="subject">` and replace inline header block with fragment include.

`.txt` templates: no changes (no header / no `<head>`).

## Tests

### Unit

- `ThymeleafTemplateRendererTest`
  - For each of 9 templates: rendered HTML body has no bare text node in `<head>` (only inside `<title>`). Use Jsoup to assert.
  - For each of 9 templates: render with (a) `tenantLogoUrl != null`, (b) `tenantLogoUrl == null && tenantName != null`, (c) `tenantLogoUrl == null && tenantName == null`. Assert the correct header markup is emitted in each case.
- `JpaTenantContextAdapterTest` — `logoUrl` populated from `logo_key` via `LogoStorage.getPublicUrl`; `null` when `logo_key` null.
- `S3LogoStorageTest` — `getPublicUrl(...)` returns expected URL format; `null` for null key.

### Integration

- `EmailDispatcherServiceIT` — dispatch `MEMBERSHIP_ACTIVATED`. With tenant having `logo_key`, outbound `htmlBody` contains `<img src="https://...">`. With tenant lacking `logo_key`, `htmlBody` contains the lime badge SVG path and the tenant name.

### Manual verification

- Trigger each of 9 email types in local dev. Inspect `api/local/email-previews/*.html`: subject text not visible above content card; header reflects tenant state.
- Send a sample email per type to a Gmail web, iOS Mail, and Outlook desktop inbox. Confirm no subject leak and correct header rendering.

## Rollout

- Single PR, no feature flag required (purely visual + template fix; no behavior change beyond what users already expect).
- After merge, ops applies bucket policy update for `logos/*` public-read.
- Until bucket policy applies, tenant logos render as broken images. Coordinate merge timing with ops, or merge bucket policy first.

## Risks

- **Public bucket policy scope mistake** — granting public read on a wider prefix could leak payment proofs. Mitigation: explicit `logos/*` prefix in `Resource`; review policy diff before applying.
- **Image hotlinking** — public URLs allow third parties to embed tenant logos. Acceptable: logos are brand assets meant for public display.
- **Template render regressions in less-used clients** — Outlook desktop notoriously quirky with `object-fit`. Fallback: image still renders, may not crop. Acceptable for v1.

## Future work (out of scope)

- CloudFront in front of `logos/*` for cache + custom domain. Email code unchanged.
- `EmailService.send(EmailType, EmailRecipient, Map)` overload (no `tenantId`) when first system email type is added. At that time, the footer i18n key `layout.footer` also needs a tenant-less variant, since the current key takes `${tenantName}` and would render `"Sent by  via Klasio"` with a null tenant.
- Logo upload pipeline: validate dimensions / produce square thumbnail at upload time so `object-fit: cover` never crops awkwardly.
