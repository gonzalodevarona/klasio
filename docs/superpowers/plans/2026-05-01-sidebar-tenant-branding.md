# Sidebar Tenant Branding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace generic Klasio brand in the sidebar with the user's tenant logo + name once authenticated, and remove the redundant role label from the sidebar footer.

**Architecture:** Backend extends `MeTenantController` to return a presigned `logoUrl` alongside tenant name. Frontend extends `useSidebarIdentity` to consume it, introduces a `TenantBrand` component, and rewrites the `Brand` block in `Sidebar.tsx` to switch between Klasio (SUPERADMIN / failed fetch) and tenant brand (everyone else). UserFooter loses its duplicate role line.

**Tech Stack:** Spring Boot 3 (Java 21), JUnit 5 + Mockito, Next.js 15 + TypeScript 5.9 + Tailwind, Jest + React Testing Library.

**Spec:** `docs/superpowers/specs/2026-05-01-sidebar-tenant-branding-design.md`

---

## File Map

### Backend
- **Modify:** `api/src/main/java/com/klasio/tenant/infrastructure/web/MeTenantController.java`
  - Inject `LogoStorage`, extend response record with `logoUrl`, generate presigned URL when `logoKey` non-null.
- **Create:** `api/src/test/java/com/klasio/tenant/infrastructure/web/MeTenantControllerIntegrationTest.java`
  - WebMvcTest covering tenant-with-logo, tenant-without-logo, and SUPERADMIN-forbidden cases.

### Frontend
- **Modify:** `web/src/hooks/useSidebarIdentity.ts`
  - Add `tenantLogoUrl` and `tenantFetchFailed` to the returned object.
- **Create:** `web/src/components/layout/TenantBrand.tsx`
  - Stateless presentational component: skeleton / logo+name / name-only.
- **Create:** `web/src/components/layout/__tests__/TenantBrand.test.tsx`
  - Three render states.
- **Create:** `web/src/hooks/__tests__/useSidebarIdentity.test.ts`
  - SUPERADMIN skip, tenant role fetch success, tenant role fetch failure.
- **Modify:** `web/src/components/layout/Sidebar.tsx`
  - Rewrite `Brand`, drop role from `UserFooter`, swap KLogo in mobile top bar and drawer header.

---

## Task 1: Backend — extend `MeTenantController` response with `logoUrl`

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/web/MeTenantController.java`
- Test (create): `api/src/test/java/com/klasio/tenant/infrastructure/web/MeTenantControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Create `api/src/test/java/com/klasio/tenant/infrastructure/web/MeTenantControllerIntegrationTest.java`:

```java
package com.klasio.tenant.infrastructure.web;

import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeTenantController.class)
@Import({GlobalExceptionHandler.class, MeTenantControllerIntegrationTest.TestSecurityConfig.class})
class MeTenantControllerIntegrationTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/me/tenant").authenticated()
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TenantRepository tenantRepository;
    @MockitoBean private LogoStorage logoStorage;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private UsernamePasswordAuthenticationToken auth(String role, UUID tenantId) {
        Map<String, Object> details = new HashMap<>();
        details.put("userId", USER_ID.toString());
        details.put("tenantId", tenantId == null ? null : tenantId.toString());
        details.put("role", role);
        var token = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        token.setDetails(details);
        return token;
    }

    private Tenant tenantWithLogoKey(String logoKey) {
        return Tenant.rehydrate(
                com.klasio.tenant.domain.model.TenantId.of(TENANT_ID),
                "Acme League",
                "BJJ",
                "en",
                "America/Bogota",
                logoKey,
                new TenantSlug("acme-league"),
                new ContactInfo(
                        "contact@acme.test",
                        "+57", "3000000000", false,
                        "Calle 1", "Bogota", "Cundinamarca", "Colombia"
                ),
                com.klasio.tenant.domain.model.TenantStatus.ACTIVE,
                USER_ID,
                Instant.now(),
                null, null
        );
    }

    @Test
    @DisplayName("ADMIN with tenant logo: response includes presigned logoUrl")
    void admin_with_logo_returns_presigned_url() throws Exception {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithLogoKey("logos/acme.png")));
        when(logoStorage.generatePresignedUrl("logos/acme.png"))
                .thenReturn("https://s3.example.com/logos/acme.png?signed");

        mockMvc.perform(get("/api/v1/me/tenant").with(authentication(auth("ADMIN", TENANT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Acme League"))
                .andExpect(jsonPath("$.logoUrl").value("https://s3.example.com/logos/acme.png?signed"));

        verify(logoStorage).generatePresignedUrl(eq("logos/acme.png"));
    }

    @Test
    @DisplayName("MANAGER with tenant having no logo: logoUrl is null and storage is not called")
    void manager_without_logo_returns_null_url() throws Exception {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenantWithLogoKey(null)));

        mockMvc.perform(get("/api/v1/me/tenant").with(authentication(auth("MANAGER", TENANT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme League"))
                .andExpect(jsonPath("$.logoUrl").doesNotExist());

        verifyNoInteractions(logoStorage);
    }

    @Test
    @DisplayName("SUPERADMIN: forbidden by RBAC")
    void superadmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/me/tenant").with(authentication(auth("SUPERADMIN", null))))
                .andExpect(status().isForbidden());
    }
}
```

> **Note on `Tenant.rehydrate`:** confirm the static factory signature in `api/src/main/java/com/klasio/tenant/domain/model/Tenant.java`. If the constructor differs (different field order, alternate static factory like `Tenant.fromPersistence(...)`), match the existing test patterns in `TenantControllerIntegrationTest.java`. Do not invent a new factory.

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
./mvnw -pl . -Dtest=MeTenantControllerIntegrationTest test
```

Expected: compilation failure on `logoUrl` JSON path (because `MeTenantController` does not yet inject `LogoStorage` or expose `logoUrl`), or test failure asserting missing `logoUrl` field.

- [ ] **Step 3: Implement — extend `MeTenantController`**

Replace `api/src/main/java/com/klasio/tenant/infrastructure/web/MeTenantController.java` with:

```java
package com.klasio.tenant.infrastructure.web;

import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes GET /api/v1/me/tenant for all tenant-scoped roles.
 * Returns the current user's tenant identity (id, name, discipline, language, logoUrl)
 * so the UI can render contextual league branding without a separate
 * tenant-list query (which is SUPERADMIN-only).
 */
@RestController
@RequestMapping("/api/v1/me/tenant")
public class MeTenantController {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TenantInfoResponse(
            UUID id,
            String name,
            String discipline,
            String language,
            String logoUrl
    ) {}

    private final TenantRepository tenantRepository;
    private final LogoStorage logoStorage;

    public MeTenantController(TenantRepository tenantRepository, LogoStorage logoStorage) {
        this.tenantRepository = tenantRepository;
        this.logoStorage = logoStorage;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT')")
    public ResponseEntity<TenantInfoResponse> getMyTenant() {
        UUID tenantId = extractTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

        String logoUrl = tenant.getLogoKey() != null
                ? logoStorage.generatePresignedUrl(tenant.getLogoKey())
                : null;

        return ResponseEntity.ok(new TenantInfoResponse(
                tenant.getId().value(),
                tenant.getName(),
                tenant.getDiscipline(),
                tenant.getLanguage(),
                logoUrl
        ));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String tenantId = (String) details.get("tenantId");
        if (tenantId == null) {
            throw new IllegalStateException("No tenantId in JWT claims");
        }
        return UUID.fromString(tenantId);
    }
}
```

The `@JsonInclude(NON_NULL)` annotation on the record makes the `logoUrl` field absent from the JSON when null (matches the test assertion `jsonPath("$.logoUrl").doesNotExist()`). The frontend's optional chaining handles both absent and null identically.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw -pl . -Dtest=MeTenantControllerIntegrationTest test
```

Expected: all 3 tests pass.

- [ ] **Step 5: Run the full backend test suite**

```bash
./mvnw test
```

Expected: green. No regression in other tenant tests.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/infrastructure/web/MeTenantController.java \
        api/src/test/java/com/klasio/tenant/infrastructure/web/MeTenantControllerIntegrationTest.java
git commit -m "feat(tenant): expose tenant logoUrl in /me/tenant for sidebar branding"
```

---

## Task 2: Frontend — extend `useSidebarIdentity` with logo URL and fetch-failure flag

**Files:**
- Modify: `web/src/hooks/useSidebarIdentity.ts`
- Test (create): `web/src/hooks/__tests__/useSidebarIdentity.test.ts`

- [ ] **Step 1: Write the failing test**

Create `web/src/hooks/__tests__/useSidebarIdentity.test.ts`:

```ts
import { renderHook, waitFor } from "@testing-library/react";
import { useSidebarIdentity } from "../useSidebarIdentity";

function mockFetchByPath(handlers: Record<string, () => Response | Promise<Response>>) {
  global.fetch = jest.fn((input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input.toString();
    for (const [path, handler] of Object.entries(handlers)) {
      if (url.includes(path)) return Promise.resolve(handler());
    }
    return Promise.reject(new Error(`Unmocked URL: ${url}`));
  }) as unknown as typeof fetch;
}

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response;
}

afterEach(() => jest.restoreAllMocks());

describe("useSidebarIdentity", () => {
  it("does not fetch /me/tenant for SUPERADMIN", async () => {
    mockFetchByPath({
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "Su",
          lastName: "Per",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() =>
      useSidebarIdentity("SUPERADMIN", "tenant-id-ignored")
    );

    await waitFor(() => expect(result.current.displayName).toBe("Su Per"));

    expect(global.fetch).not.toHaveBeenCalledWith(
      expect.stringContaining("/api/me/tenant"),
      expect.anything()
    );
    expect(result.current.tenantName).toBeNull();
    expect(result.current.tenantLogoUrl).toBeNull();
    expect(result.current.tenantFetchFailed).toBe(false);
  });

  it("populates tenantName and tenantLogoUrl on success", async () => {
    mockFetchByPath({
      "/api/me/tenant": () =>
        jsonResponse({
          id: "t1",
          name: "Acme League",
          discipline: "BJJ",
          language: "en",
          logoUrl: "https://s3/acme.png",
        }),
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantName).toBe("Acme League"));
    expect(result.current.tenantLogoUrl).toBe("https://s3/acme.png");
    expect(result.current.tenantFetchFailed).toBe(false);
  });

  it("flips tenantFetchFailed when /me/tenant returns non-OK", async () => {
    mockFetchByPath({
      "/api/me/tenant": () => jsonResponse({}, 500),
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantFetchFailed).toBe(true));
    expect(result.current.tenantName).toBeNull();
    expect(result.current.tenantLogoUrl).toBeNull();
  });

  it("flips tenantFetchFailed when /me/tenant rejects", async () => {
    global.fetch = jest.fn((input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/api/me/tenant")) return Promise.reject(new Error("net"));
      return Promise.resolve(
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        })
      );
    }) as unknown as typeof fetch;

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantFetchFailed).toBe(true));
  });

  it("returns null logoUrl when backend omits the field", async () => {
    mockFetchByPath({
      "/api/me/tenant": () =>
        jsonResponse({
          id: "t1",
          name: "Acme League",
          discipline: "BJJ",
          language: "en",
        }),
      "/api/me/user-profile": () =>
        jsonResponse({
          firstName: "A",
          lastName: "B",
          identityDocumentType: "CC",
          identityNumber: "1",
        }),
    });

    const { result } = renderHook(() => useSidebarIdentity("ADMIN", "t1"));

    await waitFor(() => expect(result.current.tenantName).toBe("Acme League"));
    expect(result.current.tenantLogoUrl).toBeNull();
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm test -- --testPathPattern useSidebarIdentity
```

Expected: failures referencing `tenantLogoUrl` and `tenantFetchFailed` not present on the returned object.

- [ ] **Step 3: Implement — extend the hook**

Replace `web/src/hooks/useSidebarIdentity.ts` with:

```ts
"use client";

import { useEffect, useState } from "react";
import type { Role } from "@/lib/types/auth";

interface TenantInfo {
  id: string;
  name: string;
  discipline: string;
  language: string;
  logoUrl: string | null;
}

interface PersonInfo {
  firstName: string;
  lastName: string;
  identityDocumentType: string;
  identityNumber: string;
}

export interface SidebarIdentity {
  tenantName: string | null;
  tenantLogoUrl: string | null;
  tenantFetchFailed: boolean;
  displayName: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
}

const DOCUMENT_TYPE_LABELS: Record<string, string> = {
  CC: "CC",
  TI: "TI",
  CE: "CE",
  PA: "Pasaporte",
  RC: "RC",
  NIT: "NIT",
  PP: "PP",
};

export function useSidebarIdentity(
  role: Role | undefined,
  tenantId: string | null | undefined
): SidebarIdentity {
  const [tenantName, setTenantName] = useState<string | null>(null);
  const [tenantLogoUrl, setTenantLogoUrl] = useState<string | null>(null);
  const [tenantFetchFailed, setTenantFetchFailed] = useState<boolean>(false);
  const [personInfo, setPersonInfo] = useState<PersonInfo | null>(null);

  // Tenant identity (name + logo) — skipped for SUPERADMIN.
  useEffect(() => {
    if (!tenantId || role === "SUPERADMIN") return;

    let cancelled = false;
    fetch("/api/me/tenant", { credentials: "include" })
      .then(async (r) => {
        if (cancelled) return;
        if (!r.ok) {
          setTenantFetchFailed(true);
          return;
        }
        const data: TenantInfo | null = await r.json().catch(() => null);
        if (cancelled) return;
        if (data?.name) setTenantName(data.name);
        if (data?.logoUrl) setTenantLogoUrl(data.logoUrl);
      })
      .catch(() => {
        if (!cancelled) setTenantFetchFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [tenantId, role]);

  useEffect(() => {
    if (role !== "STUDENT") return;
    fetch("/api/me/student-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PersonInfo | null) => {
        if (data?.firstName) setPersonInfo(data);
      })
      .catch(() => {});
  }, [role]);

  useEffect(() => {
    if (role !== "PROFESSOR") return;
    fetch("/api/me/professor-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PersonInfo | null) => {
        if (data?.firstName) setPersonInfo(data);
      })
      .catch(() => {});
  }, [role]);

  useEffect(() => {
    if (!role || role === "STUDENT" || role === "PROFESSOR") return;
    fetch("/api/me/user-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PersonInfo | null) => {
        if (data?.firstName) setPersonInfo(data);
      })
      .catch(() => {});
  }, [role]);

  const docTypeLabel = personInfo
    ? (DOCUMENT_TYPE_LABELS[personInfo.identityDocumentType] ?? personInfo.identityDocumentType)
    : null;

  const displayName = personInfo?.firstName
    ? [personInfo.firstName, personInfo.lastName].filter(Boolean).join(" ")
    : null;

  return {
    tenantName,
    tenantLogoUrl,
    tenantFetchFailed,
    displayName,
    identityDocumentType: docTypeLabel,
    identityNumber: personInfo?.identityNumber ?? null,
  };
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
npm test -- --testPathPattern useSidebarIdentity
```

Expected: all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add web/src/hooks/useSidebarIdentity.ts \
        web/src/hooks/__tests__/useSidebarIdentity.test.ts
git commit -m "feat(sidebar): extend useSidebarIdentity with tenantLogoUrl and tenantFetchFailed"
```

---

## Task 3: Frontend — create `TenantBrand` component

**Files:**
- Create: `web/src/components/layout/TenantBrand.tsx`
- Test (create): `web/src/components/layout/__tests__/TenantBrand.test.tsx`

- [ ] **Step 1: Write the failing test**

Create `web/src/components/layout/__tests__/TenantBrand.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import TenantBrand from "../TenantBrand";

describe("TenantBrand", () => {
  it("renders skeleton when loading", () => {
    const { container } = render(
      <TenantBrand tenantName={null} tenantLogoUrl={null} loading />
    );
    expect(container.querySelector(".animate-pulse")).not.toBeNull();
    expect(screen.queryByRole("img")).toBeNull();
  });

  it("renders logo and name when both are present", () => {
    render(
      <TenantBrand
        tenantName="Acme League"
        tenantLogoUrl="https://s3/acme.png"
        loading={false}
      />
    );
    expect(screen.getByText("Acme League")).toBeInTheDocument();
    const img = screen.getByRole("img");
    expect(img).toHaveAttribute("src", "https://s3/acme.png");
  });

  it("renders name only when logoUrl is null", () => {
    render(
      <TenantBrand tenantName="Acme League" tenantLogoUrl={null} loading={false} />
    );
    expect(screen.getByText("Acme League")).toBeInTheDocument();
    expect(screen.queryByRole("img")).toBeNull();
  });
});
```

- [ ] **Step 2: Run test to verify failure**

```bash
npm test -- --testPathPattern TenantBrand
```

Expected: module not found error (`TenantBrand` does not yet exist).

- [ ] **Step 3: Implement `TenantBrand`**

Create `web/src/components/layout/TenantBrand.tsx`:

```tsx
import { cn } from "@/lib/utils";

interface TenantBrandProps {
  tenantName: string | null;
  tenantLogoUrl: string | null;
  loading: boolean;
  className?: string;
}

export default function TenantBrand({
  tenantName,
  tenantLogoUrl,
  loading,
  className,
}: TenantBrandProps) {
  if (loading) {
    return (
      <div
        className={cn("h-6 w-32 bg-k-sidebar-active rounded animate-pulse", className)}
        aria-hidden="true"
      />
    );
  }

  return (
    <div className={cn("flex items-center gap-2 select-none min-w-0", className)}>
      {tenantLogoUrl && (
        <img
          src={tenantLogoUrl}
          alt=""
          width={24}
          height={24}
          className="shrink-0 rounded-sm object-contain"
        />
      )}
      <span className="text-[18px] font-extrabold text-white tracking-[-0.03em] leading-none truncate">
        {tenantName}
      </span>
    </div>
  );
}
```

- [ ] **Step 4: Run test to verify pass**

```bash
npm test -- --testPathPattern TenantBrand
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/layout/TenantBrand.tsx \
        web/src/components/layout/__tests__/TenantBrand.test.tsx
git commit -m "feat(sidebar): add TenantBrand component for tenant-scoped branding"
```

---

## Task 4: Frontend — wire `TenantBrand` into Sidebar and remove duplicate role line

**Files:**
- Modify: `web/src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Update imports and `Brand` component**

Open `web/src/components/layout/Sidebar.tsx`. After the `import KLogo from "@/components/layout/KLogo";` line, add:

```tsx
import TenantBrand from "@/components/layout/TenantBrand";
```

Replace the `Brand` component (currently at lines 179–201) with:

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
  const loading = useTenantBrand && tenantName === null;

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
      {role && (
        <p className="text-[11px] text-k-subtle truncate">{role}</p>
      )}
    </div>
  );
}
```

The previous `tenantName` `<p>` below the divider is removed because the name now lives inside `TenantBrand` (or is intentionally absent for SUPERADMIN / fetch-failure).

- [ ] **Step 2: Update `UserFooter` — drop the role line**

Replace the `UserFooter` component (currently lines 205–256) with:

```tsx
function UserFooter({
  displayName,
  identityDocumentType,
  identityNumber,
  collapsed,
  forceExpanded,
  onLogout,
  signOut,
}: {
  displayName: string | null;
  identityDocumentType: string | null;
  identityNumber: string | null;
  collapsed: boolean;
  forceExpanded?: boolean;
  onLogout: () => void;
  signOut: string;
}) {
  const expanded = forceExpanded || !collapsed;
  return (
    <div className="px-2 py-4 border-t border-k-sidebar-active shrink-0">
      {expanded && (displayName || (identityDocumentType && identityNumber)) && (
        <div className="px-3 mb-2 space-y-0.5">
          {displayName && (
            <p className="text-xs font-medium text-white truncate">
              {displayName}
            </p>
          )}
          {identityDocumentType && identityNumber && (
            <p className="text-xs text-k-subtle truncate">
              {identityDocumentType} {identityNumber}
            </p>
          )}
        </div>
      )}
      <button
        onClick={onLogout}
        title={!expanded ? signOut : undefined}
        className={[
          "flex items-center gap-3 w-full px-3 py-2 rounded-md text-sm text-k-subtle",
          "hover:text-white hover:bg-k-sidebar-active transition-colors",
          !expanded ? "justify-center" : "",
        ].join(" ")}
      >
        <LogOut className="h-5 w-5 shrink-0" />
        {expanded && <span>{signOut}</span>}
      </button>
    </div>
  );
}
```

The `role` prop is dropped from the signature, the role `<p>` line is gone, and the wrapper `<div>` is now skipped entirely if there is no name and no document.

- [ ] **Step 3: Update consumers — destructure new identity fields and rewire `Brand`/`UserFooter`**

In the `Sidebar` component body (around line 266), replace the `useSidebarIdentity` destructure with:

```tsx
const {
  tenantName,
  tenantLogoUrl,
  tenantFetchFailed,
  displayName,
  identityDocumentType,
  identityNumber,
} = useSidebarIdentity(primaryUserRole, user?.tenantId);
```

Replace the desktop `<Brand .../>` invocation (around line 411) with:

```tsx
<Brand
  role={primaryUserRole}
  tenantName={tenantName}
  tenantLogoUrl={tenantLogoUrl}
  tenantFetchFailed={tenantFetchFailed}
  collapsed={collapsed}
/>
```

Replace the desktop `<UserFooter .../>` invocation (around line 441) with:

```tsx
<UserFooter
  displayName={displayName}
  identityDocumentType={identityDocumentType}
  identityNumber={identityNumber}
  collapsed={collapsed}
  onLogout={logout}
  signOut={t("signOut")}
/>
```

Replace the mobile drawer `<UserFooter .../>` invocation (around line 381) with:

```tsx
<UserFooter
  displayName={displayName}
  identityDocumentType={identityDocumentType}
  identityNumber={identityNumber}
  collapsed={false}
  forceExpanded
  onLogout={logout}
  signOut={t("signOut")}
/>
```

- [ ] **Step 4: Update mobile top bar — swap KLogo for TenantBrand for tenant-scoped roles**

Replace the mobile top bar `<div className="min-w-0 flex-1">...</div>` block (lines 325–332) with:

```tsx
<div className="min-w-0 flex-1">
  {primaryUserRole && primaryUserRole !== "SUPERADMIN" && !tenantFetchFailed ? (
    <TenantBrand
      tenantName={tenantName}
      tenantLogoUrl={tenantLogoUrl}
      loading={tenantName === null}
    />
  ) : (
    <KLogo />
  )}
</div>
```

The legacy `<span>{tenantName}</span>` after `KLogo` is dropped — the tenant name is now inside `TenantBrand`.

- [ ] **Step 5: Update mobile drawer header — same role-aware swap**

Replace the mobile drawer header inner block (lines 348–357) with:

```tsx
<div className="min-w-0 flex-1">
  {primaryUserRole && primaryUserRole !== "SUPERADMIN" && !tenantFetchFailed ? (
    <TenantBrand
      tenantName={tenantName}
      tenantLogoUrl={tenantLogoUrl}
      loading={tenantName === null}
    />
  ) : (
    <KLogo />
  )}
  <hr className="border-k-sidebar-active my-2" />
  {primaryUserRole && (
    <p className="text-[11px] text-k-subtle truncate">{primaryUserRole}</p>
  )}
</div>
```

The redundant `tenantName` `<p>` below the divider is removed (now inside `TenantBrand`); the role line is preserved.

- [ ] **Step 6: Run frontend type check + lint + tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm run lint
npx tsc --noEmit
npm test -- --testPathPattern "TenantBrand|useSidebarIdentity"
```

Expected: lint clean, type check clean, 8 tests pass (3 TenantBrand + 5 useSidebarIdentity).

- [ ] **Step 7: Manual smoke test in browser**

Start dev stack and verify:

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm run dev
```

Then in the browser:
1. Log in as ADMIN of a tenant that has a logo → desktop sidebar header shows tenant logo + tenant name; role label "ADMIN" appears below the divider; bottom of sidebar shows full name + ID document but **no** role line.
2. Log in as STUDENT of the same tenant → same brand + "STUDENT" role label.
3. Log in as SUPERADMIN → Klasio wordmark unchanged in all three surfaces; role label "SUPERADMIN" still appears below the divider.
4. Resize to mobile width → top bar and drawer header show the tenant brand for tenant-scoped users.
5. Collapse the desktop sidebar → header is empty (existing behavior preserved).
6. Stop the backend, reload as ADMIN → after the failed `/me/tenant` call the header falls back to Klasio brand (graceful degrade).

Document any deviations as a separate fix task; do not bend the plan to broken state.

- [ ] **Step 8: Commit**

```bash
git add web/src/components/layout/Sidebar.tsx
git commit -m "feat(sidebar): swap Klasio for tenant brand and drop duplicate role label"
```

---

## Task 5: Final verification

- [ ] **Step 1: Full backend test suite**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
./mvnw test
```

Expected: green.

- [ ] **Step 2: Full frontend test suite + type check + lint**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web
npm run lint
npx tsc --noEmit
npm test
```

Expected: green.

- [ ] **Step 3: Confirm no follow-up commits needed**

```bash
git status
git log --oneline -5
```

Expected: working tree clean, last 4 commits cover backend, hook, component, sidebar wiring.

---

## Self-Review Notes

**Spec coverage:** Each spec section maps to a task —
- Backend `MeTenantController` extension → Task 1.
- `useSidebarIdentity` extension (`tenantLogoUrl` + `tenantFetchFailed`) → Task 2.
- `TenantBrand` component (3 render states) → Task 3.
- `Sidebar.tsx` `Brand` rewrite, `UserFooter` role removal, mobile top bar + drawer header swaps → Task 4.
- Edge cases (SUPERADMIN, no logo, fetch failure, loading) → exercised by Task 2/3 unit tests + Task 4 manual smoke list.

**Type consistency:** `tenantLogoUrl: string | null`, `tenantFetchFailed: boolean`, `tenantName: string | null` are the same identifiers used across the hook, the `TenantBrand` props, and the `Brand` props. `loading` is computed at each call site (`tenantName === null` for mobile surfaces, `useTenantBrand && tenantName === null` in desktop `Brand`).

**No placeholders:** All test code, controller code, hook code, and component code are written out in full.
