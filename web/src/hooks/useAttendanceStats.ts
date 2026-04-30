"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AttendanceStats } from "@/lib/types/attendance";

export function useAttendanceStats(): {
  stats: AttendanceStats | null;
  loading: boolean;
  error: string | null;
} {
  const [stats, setStats] = useState<AttendanceStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .get<AttendanceStats>("/me/attendance/stats")
      .then((data) => {
        if (!cancelled) setStats(data);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err instanceof Error ? err.message : "Failed to load attendance stats.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { stats, loading, error };
}
