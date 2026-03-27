"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  EnrollmentListResponse,
  EnrollmentSummary,
  LevelHistoryEntry,
  LevelHistoryListResponse,
} from "@/lib/types/enrollment";

export function useStudentEnrollments(studentId: string, status: string = "ACTIVE") {
  const [enrollments, setEnrollments] = useState<EnrollmentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEnrollments = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const statusParam = status ? `&status=${status}` : "";
      const data = await api.get<EnrollmentListResponse>(
        `/students/${studentId}/enrollments?page=0&size=100${statusParam}`
      );
      setEnrollments(data.content);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load enrollments."
      );
    } finally {
      setLoading(false);
    }
  }, [studentId, status]);

  useEffect(() => {
    fetchEnrollments();
  }, [fetchEnrollments]);

  return { enrollments, loading, error, refetch: fetchEnrollments };
}

export function useProgramEnrollments(
  programId: string,
  page = 0,
  size = 20,
  level?: string,
  status?: string
) {
  const [enrollments, setEnrollments] = useState<EnrollmentSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchEnrollments = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const levelParam = level ? `&level=${level}` : "";
      const statusParam = status ? `&status=${status}` : "";
      const data = await api.get<EnrollmentListResponse>(
        `/programs/${programId}/enrollments?page=${page}&size=${size}${levelParam}${statusParam}`
      );
      setEnrollments(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load enrollments."
      );
    } finally {
      setLoading(false);
    }
  }, [programId, page, size, level, status]);

  useEffect(() => {
    fetchEnrollments();
  }, [fetchEnrollments]);

  return { enrollments, totalPages, totalElements, loading, error, refetch: fetchEnrollments };
}

export function useLevelHistory(enrollmentId: string) {
  const [history, setHistory] = useState<LevelHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<LevelHistoryListResponse>(
        `/enrollments/${enrollmentId}/level-history`
      );
      setHistory(data.content);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load level history."
      );
    } finally {
      setLoading(false);
    }
  }, [enrollmentId]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  return { history, loading, error, refetch: fetchHistory };
}
