"use client";
import { useCallback, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export type BulkPayload = {
  startTime: string;
  studentIds: string[];
  hoursToCharge: number;
};

export type BulkResultRow = {
  studentId: string;
  outcome: "SUCCESS" | "FAILED";
  registrationId?: string;
  status?: string;
  intendedHours?: number;
  errorCode?: string;
  errorMessage?: string;
};

export type BulkResult = {
  results: BulkResultRow[];
  summary: { total: number; succeeded: number; failed: number };
};

export type BulkError = { code: string; message: string };

export function useWalkInBulkRegistration(classId: string, sessionDate: string) {
  const [isPending, setPending] = useState(false);
  const [error, setError] = useState<BulkError | null>(null);

  const mutate = useCallback(
    async (payload: BulkPayload): Promise<BulkResult> => {
      setPending(true);
      setError(null);
      try {
        const res = await fetch(
          `${API_BASE}/classes/${classId}/sessions/${sessionDate}/walk-in/bulk`,
          {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          }
        );
        if (!res.ok) {
          const err = (await res.json()) as BulkError;
          setError(err);
          throw err;
        }
        return (await res.json()) as BulkResult;
      } finally {
        setPending(false);
      }
    },
    [classId, sessionDate]
  );

  return { mutate, isPending, error };
}
