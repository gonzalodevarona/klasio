"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { AvailableSession } from "@/lib/types/attendance";

interface UseMyAvailableSessionsResult {
  sessions: AvailableSession[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useMyAvailableSessions(params: {
  from: string;
  to: string;
  includeFull?: boolean;
}): UseMyAvailableSessionsResult {
  const [sessions, setSessions] = useState<AvailableSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSessions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        from: params.from,
        to: params.to,
        includeFull: String(params.includeFull ?? false),
      });
      const data = await api.get<AvailableSession[]>(
        `/me/available-sessions?${query.toString()}`
      );
      setSessions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load sessions.");
    } finally {
      setLoading(false);
    }
  }, [params.from, params.to, params.includeFull]);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  return { sessions, loading, error, refetch: fetchSessions };
}
