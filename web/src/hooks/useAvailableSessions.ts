"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { AvailableSession } from "@/lib/types/attendance";

interface UseAvailableSessionsParams {
  from: string;
  to: string;
  includeFull?: boolean;
}

export function useAvailableSessions(
  programId: string,
  params: UseAvailableSessionsParams
): { sessions: AvailableSession[]; loading: boolean; error: string | null; refetch: () => void } {
  const [sessions, setSessions] = useState<AvailableSession[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    if (!programId) return;
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({ from: params.from, to: params.to });
      if (params.includeFull !== undefined) {
        query.set("includeFull", String(params.includeFull));
      }
      const data = await api.get<AvailableSession[]>(
        `/me/programs/${programId}/available-sessions?${query.toString()}`
      );
      setSessions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load available sessions.");
    } finally {
      setLoading(false);
    }
  }, [programId, params.from, params.to, params.includeFull]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { sessions, loading, error, refetch: fetch };
}
