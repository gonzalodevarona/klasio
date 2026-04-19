"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  ClassLevel,
  ClassStatus,
  ProgramClassDetail,
  ProgramClassListResponse,
  ProgramClassSummary,
} from "@/lib/types/programClass";

export function useProgramClasses(
  programId: string,
  page = 0,
  size = 20,
  level?: ClassLevel,
  status?: ClassStatus
) {
  const [classes, setClasses] = useState<ProgramClassSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchClasses = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const levelParam = level ? `&level=${level}` : "";
      const statusParam = status ? `&status=${status}` : "";
      const data = await api.get<ProgramClassListResponse>(
        `/programs/${programId}/classes?page=${page}&size=${size}${levelParam}${statusParam}`
      );
      setClasses(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load classes."
      );
    } finally {
      setLoading(false);
    }
  }, [programId, page, size, level, status]);

  useEffect(() => {
    fetchClasses();
  }, [fetchClasses]);

  return { classes, totalPages, totalElements, loading, error, refetch: fetchClasses };
}

export function useAllClasses(
  page = 0,
  size = 20,
  level?: ClassLevel,
  status?: ClassStatus,
  programName?: string
) {
  const [classes, setClasses] = useState<ProgramClassSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchClasses = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (level) params.set("level", level);
      if (status) params.set("status", status);
      if (programName) params.set("programName", programName);
      const data = await api.get<ProgramClassListResponse>(`/classes?${params.toString()}`);
      setClasses(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load classes."
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, level, status, programName]);

  useEffect(() => {
    fetchClasses();
  }, [fetchClasses]);

  return { classes, totalPages, totalElements, loading, error, refetch: fetchClasses };
}

export function useProfessorClasses(professorId: string, page = 0, size = 20) {
  const [classes, setClasses] = useState<ProgramClassSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchClasses = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams({ page: String(page), size: String(size), professorId });
      const data = await api.get<ProgramClassListResponse>(`/classes?${params.toString()}`);
      setClasses(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load classes.");
    } finally {
      setLoading(false);
    }
  }, [professorId, page, size]);

  useEffect(() => {
    fetchClasses();
  }, [fetchClasses]);

  return { classes, totalPages, totalElements, loading, error, refetch: fetchClasses };
}

export function useProgramClassDetail(programId: string, classId: string) {
  const [programClass, setProgramClass] = useState<ProgramClassDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchClass = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<ProgramClassDetail>(
        `/programs/${programId}/classes/${classId}`
      );
      setProgramClass(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load class details."
      );
    } finally {
      setLoading(false);
    }
  }, [programId, classId]);

  useEffect(() => {
    fetchClass();
  }, [fetchClass]);

  return { programClass, loading, error, refetch: fetchClass };
}
