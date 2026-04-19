"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  CreateProfessorRequest,
  ProfessorDetail,
  ProfessorListResponse,
  ProfessorStatus,
  ProfessorSummary,
  UpdateProfessorRequest,
} from "@/lib/types/professor";

export function useProfessors(page = 0, size = 20, status?: ProfessorStatus) {
  const [professors, setProfessors] = useState<ProfessorSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProfessors = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const statusParam = status ? `&status=${status}` : "";
      const data = await api.get<ProfessorListResponse>(
        `/professors?page=${page}&size=${size}${statusParam}`
      );
      setProfessors(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load professors."
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, status]);

  useEffect(() => {
    fetchProfessors();
  }, [fetchProfessors]);

  return { professors, totalPages, totalElements, loading, error, refetch: fetchProfessors };
}

export function useAllActiveProfessors() {
  const [professors, setProfessors] = useState<ProfessorSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await api.get<ProfessorListResponse>(
          `/professors?page=0&size=200&status=ACTIVE`
        );
        if (!cancelled) setProfessors(data.content);
      } catch (err) {
        if (!cancelled)
          setError(err instanceof Error ? err.message : "Failed to load professors.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  return { professors, loading, error };
}

export function useProfessorDetail(id: string) {
  const [professor, setProfessor] = useState<ProfessorDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProfessor = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<ProfessorDetail>(`/professors/${id}`);
      setProfessor(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load professor details."
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchProfessor();
  }, [fetchProfessor]);

  return { professor, loading, error, refetch: fetchProfessor };
}

export function useCreateProfessor() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const create = useCallback(async (req: CreateProfessorRequest): Promise<ProfessorSummary> => {
    setLoading(true);
    setError(null);
    try {
      return await api.post<ProfessorSummary>("/professors", req);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to create professor.";
      setError(msg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);
  return { create, loading, error, clearError };
}

export function useUpdateProfessor() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const update = useCallback(async (id: string, req: UpdateProfessorRequest): Promise<ProfessorSummary> => {
    setLoading(true);
    setError(null);
    try {
      return await api.put<ProfessorSummary>(`/professors/${id}`, req);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to update professor.";
      setError(msg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);
  return { update, loading, error, clearError };
}

export function useDeactivateProfessor() {
  const [loading, setLoading] = useState(false);

  const deactivate = useCallback(async (id: string): Promise<void> => {
    setLoading(true);
    try {
      await api.post<void>(`/professors/${id}/deactivate`);
    } finally {
      setLoading(false);
    }
  }, []);

  return { deactivate, loading };
}

export function useReactivateProfessor() {
  const reactivate = useCallback(async (id: string): Promise<void> => {
    await api.post<void>(`/professors/${id}/reactivate`);
  }, []);

  return { reactivate };
}
