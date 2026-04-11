"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { EnrollmentSummary, EnrollmentListResponse } from "@/lib/types/enrollment";

export function useMyEnrollments() {
  const [enrollments, setEnrollments] = useState<EnrollmentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<EnrollmentListResponse>("/me/enrollments");
      setEnrollments(data.content ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your enrollments.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { enrollments, loading, error, refetch: fetch };
}
