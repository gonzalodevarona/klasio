"use client";
import { useState, useCallback } from "react";

export type WalkInPayload = { startTime: string; studentId: string; hoursToCharge: number };
export type WalkInResponse = { registrationId: string; status: string; intendedHours: number };
export type WalkInError = { code: string; message: string };

export function useWalkInRegistration(classId: string, sessionDate: string) {
  const [isPending, setPending] = useState(false);
  const [error, setError] = useState<WalkInError | null>(null);

  const mutate = useCallback(
    async (payload: WalkInPayload): Promise<WalkInResponse> => {
      setPending(true);
      setError(null);
      try {
        const res = await fetch(
          `/api/v1/classes/${classId}/sessions/${sessionDate}/walk-in`,
          {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          }
        );
        if (!res.ok) {
          const err = (await res.json()) as WalkInError;
          setError(err);
          throw err;
        }
        return (await res.json()) as WalkInResponse;
      } finally {
        setPending(false);
      }
    },
    [classId, sessionDate]
  );

  return { mutate, isPending, error };
}
