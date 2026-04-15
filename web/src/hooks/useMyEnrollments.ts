"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { EnrollmentSummary, EnrollmentListResponse } from "@/lib/types/enrollment";

export function useMyEnrollments(status?: string) {
  const [enrollments, setEnrollments] = useState<EnrollmentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const url = status ? `/me/enrollments?status=${status}` : "/me/enrollments";
      const data = await api.get<EnrollmentListResponse>(url);
      setEnrollments(data.content ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your enrollments.");
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { enrollments, loading, error, refetch: fetch };
}
