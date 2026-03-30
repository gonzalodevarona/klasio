"use client";

import { useCallback, useEffect, useState } from "react";
import type { Role } from "@/lib/types/auth";

interface AuthUser {
  userId: string;
  role: Role;
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
    // It carries {userId, role, tenantId} — public claims only, no JWT secret.
    const raw = getCookie("userInfo");
    if (!raw) {
      setUser(null);
      setLoading(false);
      return;
    }

    try {
      const parsed = JSON.parse(raw) as { userId: string; role: Role; tenantId: string | null };
      if (!parsed.userId || !parsed.role) {
        setUser(null);
      } else {
        setUser({ userId: parsed.userId, role: parsed.role, tenantId: parsed.tenantId ?? null });
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

  return { user, loading, logout };
}
