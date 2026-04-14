"use client";

import { useEffect, useState } from "react";
import type { Role } from "@/lib/types/auth";

interface TenantInfo {
  id: string;
  name: string;
  sportDiscipline: string;
}

interface StudentInfo {
  firstName: string;
  lastName: string;
  identityDocumentType: string;
  identityNumber: string;
}

interface UserProfile {
  email: string;
}

export interface SidebarIdentity {
  tenantName: string | null;
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
};

/**
 * Fetches the contextual identity data shown in the sidebar:
 * - Tenant name (for all tenant-scoped roles)
 * - Student name + document info (only for STUDENT role)
 *
 * Both fetches are fire-and-forget on mount; failures surface as nulls
 * so the sidebar degrades gracefully without blocking navigation.
 */
export function useSidebarIdentity(
  role: Role | undefined,
  tenantId: string | null | undefined
): SidebarIdentity {
  const [tenantName, setTenantName] = useState<string | null>(null);
  const [studentInfo, setStudentInfo] = useState<StudentInfo | null>(null);
  const [userEmail, setUserEmail] = useState<string | null>(null);

  // Fetch tenant name for all non-SUPERADMIN roles.
  useEffect(() => {
    if (!tenantId || role === "SUPERADMIN") return;

    fetch("/api/me/tenant", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: TenantInfo | null) => {
        if (data?.name) setTenantName(data.name);
      })
      .catch(() => {/* degrade gracefully */});
  }, [tenantId, role]);

  // Fetch full student profile (name + document) for STUDENT role.
  useEffect(() => {
    if (role !== "STUDENT") return;

    fetch("/api/me/student-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: StudentInfo | null) => {
        if (data?.firstName) setStudentInfo(data);
      })
      .catch(() => {/* degrade gracefully */});
  }, [role]);

  // Fetch email for all other authenticated roles.
  useEffect(() => {
    if (!role || role === "STUDENT") return;

    fetch("/api/me/user-profile", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data: UserProfile | null) => {
        if (data?.email) setUserEmail(data.email);
      })
      .catch(() => {/* degrade gracefully */});
  }, [role]);

  const isStudent = role === "STUDENT";

  return {
    tenantName,
    displayName: isStudent
      ? (studentInfo ? `${studentInfo.firstName} ${studentInfo.lastName}` : null)
      : userEmail,
    identityDocumentType: isStudent && studentInfo
      ? (DOCUMENT_TYPE_LABELS[studentInfo.identityDocumentType] ??
          studentInfo.identityDocumentType)
      : null,
    identityNumber: isStudent ? (studentInfo?.identityNumber ?? null) : null,
  };
}
