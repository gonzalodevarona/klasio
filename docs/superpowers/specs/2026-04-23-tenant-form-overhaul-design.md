# Tenant Creation Form Overhaul

**Date:** 2026-04-23  
**Branch:** feature/012-unified-account-setup  
**Scope:** Backend schema + domain model + API + frontend form

---

## Goal

Replace the partial "Create New League" form with a complete "Create New Tenant" form where every field is required and structured address/phone data is stored in separate DB columns.

---

## Required Fields (all required)

| Field | Type | Validation |
|---|---|---|
| Name | text | non-empty |
| Sport Discipline | text | non-empty |
| Language | dropdown | `es` or `en` only |
| Slug | text | auto-fills from name, editable, non-empty |
| Contact Email | email | valid email format |
| Contact Phone | text | digits only (`/^\d+$/`), non-empty; disabled until country selected |
| Street Address | text | non-empty |
| City | text | non-empty |
| State / Department | text | non-empty |
| Country | searchable dropdown | must select from list; Colombia / Spain / USA pinned at top |
| Logo | file | JPEG or PNG, max 5 MB |

---

## Phone + Country Interaction

- Selecting country sets two values: `contactCountry` (name string) + `contactPhoneIndicator` (dial code, e.g. `"57"`)
- Phone input renders `+{dialCode}` as a non-editable prefix in the same input row
- Phone field is **disabled** until a country is selected
- Phone stored as digits only — indicator stored separately

---

## Language Field

- Dropdown with exactly two options: Spanish (`es`), English (`en`)
- Stored as ISO 639-1 code
- DB constraint enforces valid values

---

## Slug Behavior

- Auto-fills in real time from Name field using existing `slugify()` function
- User can override manually
- Validated non-empty on submit

---

## DB Migration (V060)

```sql
ALTER TABLE tenants RENAME COLUMN contact_address TO contact_street;
ALTER TABLE tenants ALTER COLUMN contact_street SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN contact_phone SET NOT NULL;
ALTER TABLE tenants ADD COLUMN contact_city            VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_state           VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_country         VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN contact_phone_indicator VARCHAR(10)  NOT NULL DEFAULT '';
ALTER TABLE tenants ADD COLUMN language                VARCHAR(5)   NOT NULL DEFAULT 'es';
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_language CHECK (language IN ('es', 'en'));
```

> Existing dev/test rows get empty-string defaults. No prod data affected.

---

## Backend Changes

### Domain

- `ContactInfo` record: `(email, phone, phoneIndicator, street, city, state, country)` — validates email not blank; validates all address fields not blank
- `Tenant` domain model: add `language` field

### Application

- `CreateTenantCommand`: replace `contactAddress` → `contactStreet, contactCity, contactState, contactCountry`; add `contactPhoneIndicator`, `language`
- `CreateTenantService`: update `ContactInfo` construction; pass `language` to `Tenant.create()`
- `TenantDetail` (app DTO): replace `contactAddress` with 4 address fields + `phoneIndicator` + `language`

### Infrastructure

- `TenantController`: add `contactStreet, contactCity, contactState, contactCountry, contactPhoneIndicator, language` as required `@RequestParam`; make `logo` required; remove `contactAddress`
- `TenantJpaEntity`: replace `contactAddress` with 4 columns + `phoneIndicator` + `language`
- `TenantMapper`: update `toDomain` / `toEntity` for all new fields
- `TenantResponseDto.TenantDetailResponse`: replace `contactAddress` with new fields

### Tests

Update fixtures in:
- `CreateTenantServiceTest`
- `TenantControllerIntegrationTest`
- `TenantTest`
- `JpaTenantRepositoryIntegrationTest`

---

## Frontend Changes

### New files

- `web/src/lib/countries.ts` — static list of ~195 countries, each `{ name, dialCode, flag }`. Pinned group: Colombia (`57`), Spain (`34`), United States (`1`).
- `web/src/components/tenants/CountrySelect.tsx` — searchable dropdown built with `useState`. Text input filters list. Click sets value + collapses. Closes on outside click via `useRef + useEffect`. Renders flag + name in list, selected value in trigger.

### Modified files

| File | Change |
|---|---|
| `tenants/new/page.tsx` | Title + h1 → "Create New Tenant" |
| `lib/types/tenant.ts` | Replace `contactAddress` with `contactStreet, contactCity, contactState, contactCountry`; add `contactPhoneIndicator`, `language` |
| `TenantForm.tsx` | Full rework — 4 address states, phone disabled until country set, `CountrySelect`, digits-only phone validation, logo required validation, slug required validation, slug auto-fills from name in real time, button → "Create Tenant" |
| `LogoUpload.tsx` | Add `*` required indicator to label |
| `TenantDetail.tsx` | Display new address fields; display phone as `+{indicator} {phone}` |

---

## What is NOT changing

- No phone format length enforcement — any digit string accepted
- Country stored as plain name string, not ISO code
- No API versioning — same endpoint, new required params
- `slugify()` function unchanged
