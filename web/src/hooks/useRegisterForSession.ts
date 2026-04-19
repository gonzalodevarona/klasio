"use client";

import { useCallback, useState } from "react";
import { api } from "@/lib/api";
import { Registration } from "@/lib/types/attendance";

export function useRegisterForSession(): {
  register: (classId: string, sessionDate: string, intendedHours: number) => Promise<Registration>;
  loading: boolean;
  error: string | null;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const register = useCallback(
    async (classId: string, sessionDate: string, intendedHours: number): Promise<Registration> => {
      setLoading(true);
      setError(null);
      try {
        return await api.post<Registration>("/me/registrations", {
          classId,
          sessionDate,
          intendedHours,
        });
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to register for session.";
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return { register, loading, error };
}
