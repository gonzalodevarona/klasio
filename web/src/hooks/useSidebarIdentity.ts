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

/**
 * Fetches the contextual identity data shown in the sidebar footer:
 * - Tenant name + logo (for all tenant-scoped roles)
 * - Full name + document info (for ALL roles that have it)
 *
 * Fetch strategy by role:
 *   STUDENT   → /api/me/student-profile   (firstName, lastName, identityDocumentType, identityNumber)
 *   PROFESSOR → /api/me/professor-profile  (same shape)
 *   ADMIN     → /api/me/user-profile       (same shape, now includes name+document)
 *   MANAGER   → /api/me/user-profile       (same shape)
 *   SUPERADMIN→ /api/me/user-profile       (no tenant, but shows name+document)
 *
 * Failures degrade gracefully to null — they never block navigation.
 * tenantFetchFailed is set to true when /me/tenant returns non-OK or rejects,
 * allowing callers to fall back to Klasio brand defaults.
 */
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

  // Fetch full name + document for STUDENT.
  useEffect(() => {
    if (role !== "STUDENT") return;

    fetch("/api/me/student-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PersonInfo | null) => {
        if (data?.firstName) setPersonInfo(data);
      })
      .catch(() => {/* degrade gracefully */});
  }, [role]);

  // Fetch full name + document for PROFESSOR.
  useEffect(() => {
    if (role !== "PROFESSOR") return;

    fetch("/api/me/professor-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PersonInfo | null) => {
        if (data?.firstName) setPersonInfo(data);
      })
      .catch(() => {/* degrade gracefully */});
  }, [role]);

  // Fetch full name + document for ADMIN, MANAGER, and SUPERADMIN.
  // /api/me/user-profile proxies to /auth/me which now returns firstName, lastName,
  // identityDocumentType, identityNumber, and email for all authenticated users.
  useEffect(() => {
    if (!role || role === "STUDENT" || role === "PROFESSOR") return;

    fetch("/api/me/user-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: PersonInfo | null) => {
        if (data?.firstName) setPersonInfo(data);
      })
      .catch(() => {/* degrade gracefully */});
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
