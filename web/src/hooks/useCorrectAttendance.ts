"use client";

import { useState, useCallback } from "react";
import { api } from "@/lib/api";
import { CorrectMarkRequest, MarkedRegistration } from "@/lib/types/attendance";

interface UseCorrectAttendanceResult {
  correctMark: (
    classId: string,
    sessionDate: string,
    registrationId: string,
    request: CorrectMarkRequest
  ) => Promise<MarkedRegistration>;
  loading: boolean;
  error: string | null;
}

export function useCorrectAttendance(): UseCorrectAttendanceResult {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const correctMark = useCallback(
    async (
      classId: string,
      sessionDate: string,
      registrationId: string,
      request: CorrectMarkRequest
    ): Promise<MarkedRegistration> => {
      setLoading(true);
      setError(null);
      try {
        const result = await api.patch<MarkedRegistration>(
          `/classes/${classId}/sessions/${sessionDate}/marks/${registrationId}/correct`,
          request
        );
        return result;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "Failed to correct attendance mark";
        setError(msg);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return { correctMark, loading, error };
}
