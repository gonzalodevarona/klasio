"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  ProgramDetail,
  ProgramListResponse,
  ProgramModality,
  ProgramPlanSummary,
  ProgramStatus,
  ProgramSummary,
} from "@/lib/types/program";

export function usePrograms(page = 0, size = 20, status?: ProgramStatus) {
  const [programs, setPrograms] = useState<ProgramSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPrograms = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const statusParam = status ? `&status=${status}` : "";
      const data = await api.get<ProgramListResponse>(
        `/programs?page=${page}&size=${size}${statusParam}`
      );
      setPrograms(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load programs."
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, status]);

  useEffect(() => {
    fetchPrograms();
  }, [fetchPrograms]);

  return { programs, totalPages, totalElements, loading, error, refetch: fetchPrograms };
}

export function useProgramDetail(id: string) {
  const [program, setProgram] = useState<ProgramDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProgram = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<ProgramDetail>(`/programs/${id}`);
      setProgram(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load program details."
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchProgram();
  }, [fetchProgram]);

  return { program, loading, error, refetch: fetchProgram };
}

/** Fetches all active HOURS_BASED plans — used when creating a membership. */
export function useProgramPlans(modality?: ProgramModality) {
  const [plans, setPlans] = useState<ProgramPlanSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<ProgramPlanSummary[]>("/plans?status=ACTIVE");
      setPlans(modality ? data.filter((p) => p.modality === modality) : data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load plans.");
    } finally {
      setLoading(false);
    }
  }, [modality]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  return { plans, loading, error, refetch: fetchPlans };
}

/** Fetches active plans scoped to a specific program — used by students to discover plans for their enrollment. */
export function useProgramPlansByProgram(programId: string | null, modality?: ProgramModality) {
  const [plans, setPlans] = useState<ProgramPlanSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchPlans = useCallback(async () => {
    if (!programId) {
      setPlans([]);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<ProgramPlanSummary[]>(`/programs/${programId}/plans`);
      const activePlans = data.filter((p) => p.status === "ACTIVE");
      setPlans(modality ? activePlans.filter((p) => p.modality === modality) : activePlans);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load plans.");
    } finally {
      setLoading(false);
    }
  }, [programId, modality]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  return { plans, loading, error, refetch: fetchPlans };
}
