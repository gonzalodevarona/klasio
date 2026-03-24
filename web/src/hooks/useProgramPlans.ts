"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { ProgramPlanDetail, ProgramPlanSummary } from "@/lib/types/programPlan";

export function useProgramPlans(programId: string) {
  const [plans, setPlans] = useState<ProgramPlanSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<ProgramPlanSummary[]>(
        `/programs/${programId}/plans`
      );
      setPlans(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load plans."
      );
    } finally {
      setLoading(false);
    }
  }, [programId]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  return { plans, loading, error, refetch: fetchPlans };
}

export function useProgramPlanDetail(programId: string, planId: string) {
  const [plan, setPlan] = useState<ProgramPlanDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPlan = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<ProgramPlanDetail>(
        `/programs/${programId}/plans/${planId}`
      );
      setPlan(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load plan details."
      );
    } finally {
      setLoading(false);
    }
  }, [programId, planId]);

  useEffect(() => {
    fetchPlan();
  }, [fetchPlan]);

  return { plan, loading, error, refetch: fetchPlan };
}
