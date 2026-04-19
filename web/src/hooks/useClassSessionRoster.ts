"use client";

import { useState, useEffect, useCallback } from "react";
import { api } from "@/lib/api";
import { ClassSessionRoster } from "@/lib/types/attendance";

interface UseClassSessionRosterResult {
  sessions: ClassSessionRoster[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useClassSessionRoster(
  classId: string | null,
  from: string,
  to: string
): UseClassSessionRosterResult {
  const [sessions, setSessions] = useState<ClassSessionRoster[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const refetch = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    if (!classId) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    const params = new URLSearchParams({ from, to });
    api
      .get<ClassSessionRoster[]>(`/classes/${classId}/sessions/registrations?${params}`)
      .then((data) => {
        if (!cancelled) setSessions(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load roster");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [classId, from, to, tick]);

  return { sessions, loading, error, refetch };
}
