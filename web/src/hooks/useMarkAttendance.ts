"use client";

import { useState, useCallback } from "react";
import { api } from "@/lib/api";
import {
  MarkAttendanceRequest,
  MarkAttendanceResponse,
} from "@/lib/types/attendance";

interface UseMarkAttendanceResult {
  markAttendance: (
    classId: string,
    sessionDate: string,
    request: MarkAttendanceRequest
  ) => Promise<MarkAttendanceResponse>;
  loading: boolean;
  error: string | null;
}

export function useMarkAttendance(): UseMarkAttendanceResult {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const markAttendance = useCallback(
    async (
      classId: string,
      sessionDate: string,
      request: MarkAttendanceRequest
    ): Promise<MarkAttendanceResponse> => {
      setLoading(true);
      setError(null);
      try {
        const result = await api.post<MarkAttendanceResponse>(
          `/classes/${classId}/sessions/${sessionDate}/marks`,
          request
        );
        return result;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "Failed to mark attendance";
        setError(msg);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return { markAttendance, loading, error };
}
