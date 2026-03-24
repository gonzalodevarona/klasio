"use client";

import { useState, useEffect, useCallback } from "react";
import { api } from "@/lib/api";
import { ProgramPlanSummary } from "@/lib/types/programPlan";

export function useAllPlans(status?: string) {
  const [plans, setPlans] = useState<ProgramPlanSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (status) params.append("status", status);
      const query = params.toString();
      const data = await api.get<ProgramPlanSummary[]>(`/plans${query ? `?${query}` : ""}`);
      setPlans(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load plans");
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  return { plans, loading, error, refetch: fetchPlans };
}
