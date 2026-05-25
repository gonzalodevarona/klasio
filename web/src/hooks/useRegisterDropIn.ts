"use client";

import { useState, useCallback } from "react";
import type { RegisterDropInInput, RegisterDropInResponse } from "@/lib/dropIn";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export class DropInPhoneConflictError extends Error {
  constructor(
    public readonly existingAttendeeId: string,
    public readonly fullName: string,
    public readonly totalVisits: number
  ) {
    super("Phone already registered to " + fullName);
    this.name = "DropInPhoneConflictError";
  }
}

export function useRegisterDropIn(classId: string, sessionDate: string) {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const mutate = useCallback(
    async (input: RegisterDropInInput): Promise<RegisterDropInResponse> => {
      setIsPending(true);
      setError(null);
      try {
        const res = await fetch(
          `${API_BASE}/classes/${classId}/sessions/${sessionDate}/drop-in`,
          {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(input),
          }
        );
        if (!res.ok) {
          const body = await res.json().catch(() => null);
          if (res.status === 409 && body?.error?.code === "DROP_IN_PHONE_EXISTS") {
            const err = new DropInPhoneConflictError(
              body.existingAttendeeId,
              body.fullName,
              body.totalVisits
            );
            setError(err);
            throw err;
          }
          const code = body?.error?.code ?? "UNKNOWN";
          const message = body?.error?.message ?? res.statusText;
          const err = Object.assign(new Error(message), { code, status: res.status });
          setError(err);
          throw err;
        }
        return res.json();
      } finally {
        setIsPending(false);
      }
    },
    [classId, sessionDate]
  );

  return { mutate, isPending, error };
}
