"use client";

import { useCallback, useEffect, useState } from "react";
import type { Role } from "@/lib/types/auth";
import { primaryRole } from "@/lib/types/auth";

interface AuthUser {
  userId: string;
  roles: Role[];
  tenantId: string | null;
}

function getCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(^| )${name}=([^;]+)`));
  return match ? decodeURIComponent(match[2]) : null;
}

export function useAuth() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // userInfo is a non-HttpOnly cookie set by the Next.js auth proxy at login.
    // It carries {userId, roles: string[], tenantId} — public claims only, no JWT secret.
    const raw = getCookie("userInfo");
    if (!raw) {
      setUser(null);
      setLoading(false);
      return;
    }

    try {
      const parsed = JSON.parse(raw) as { userId: string; roles: Role[]; tenantId: string | null };
      if (!parsed.userId || !parsed.roles?.length) {
        setUser(null);
      } else {
        setUser({ userId: parsed.userId, roles: parsed.roles, tenantId: parsed.tenantId ?? null });
      }
    } catch {
      setUser(null);
    }

    setLoading(false);
  }, []);

  const logout = useCallback(async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    setUser(null);
    window.location.href = "/login";
  }, []);

  const hasRole = useCallback(
    (r: Role) => user?.roles.includes(r) ?? false,
    [user]
  );

  return { user, loading, logout, hasRole };
}
