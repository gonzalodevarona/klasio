"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { ProgramClassSummary } from "@/lib/types/programClass";

export function useMyClasses() {
  const [classes, setClasses] = useState<ProgramClassSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<ProgramClassSummary[]>("/me/classes");
      setClasses(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your classes.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { classes, loading, error, refetch: fetch };
}
