"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  StudentDetail,
  StudentListResponse,
  StudentStatus,
  StudentSummary,
} from "@/lib/types/student";

export function useStudents(
  page = 0,
  size = 20,
  status?: StudentStatus,
  search?: string
) {
  const [students, setStudents] = useState<StudentSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStudents = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const statusParam = status ? `&status=${status}` : "";
      const searchParam = search ? `&search=${encodeURIComponent(search)}` : "";
      const data = await api.get<StudentListResponse>(
        `/students?page=${page}&size=${size}${statusParam}${searchParam}`
      );
      setStudents(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load students."
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, status, search]);

  useEffect(() => {
    fetchStudents();
  }, [fetchStudents]);

  return { students, totalPages, totalElements, loading, error, refetch: fetchStudents };
}

export function useAllActiveStudents() {
  const [students, setStudents] = useState<StudentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await api.get<StudentListResponse>(
          `/students?page=0&size=200&status=ACTIVE`
        );
        if (!cancelled) setStudents(data.content);
      } catch (err) {
        if (!cancelled)
          setError(err instanceof Error ? err.message : "Failed to load students.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  return { students, loading, error };
}

export function useStudentDetail(id: string) {
  const [student, setStudent] = useState<StudentDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStudent = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<StudentDetail>(`/students/${id}`);
      setStudent(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load student details."
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchStudent();
  }, [fetchStudent]);

  return { student, loading, error, refetch: fetchStudent };
}
