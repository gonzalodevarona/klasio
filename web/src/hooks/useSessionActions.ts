"use client";

import { useState, useCallback } from "react";
import { api } from "@/lib/api";

// ---------------------------------------------------------------------------
// Shared action input
// ---------------------------------------------------------------------------

interface SessionActionInput {
  classId: string;
  sessionDate: string;
  reason: string;
}

// ---------------------------------------------------------------------------
// Internal helper — shared loading/error lifecycle
// ---------------------------------------------------------------------------

function useAction<TInput>(
  perform: (input: TInput) => Promise<void>
): {
  execute: (input: TInput) => Promise<void>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

  const execute = useCallback(
    async (input: TInput) => {
      setLoading(true);
      setError(null);
      try {
        await perform(input);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : "An unexpected error occurred.";
        setError(msg);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [perform] // honest dependency — callers must memoize perform
  );

  return { execute, loading, error, clearError };
}

// ---------------------------------------------------------------------------
// useRaiseSessionAlert
// ---------------------------------------------------------------------------

export function useRaiseSessionAlert() {
  const perform = useCallback(async ({ classId, sessionDate, reason }: SessionActionInput) => {
    await api.post(`/classes/${classId}/sessions/${sessionDate}/alert`, { reason });
  }, []);

  const { execute, loading, error, clearError } = useAction(perform);

  return { raiseAlert: execute, loading, error, clearError };
}

// ---------------------------------------------------------------------------
// useUpdateSessionAlert
// ---------------------------------------------------------------------------

export function useUpdateSessionAlert() {
  const perform = useCallback(async ({ classId, sessionDate, reason }: SessionActionInput) => {
    await api.patch(`/classes/${classId}/sessions/${sessionDate}/alert`, { reason });
  }, []);

  const { execute, loading, error, clearError } = useAction(perform);

  return { updateAlert: execute, loading, error, clearError };
}

// ---------------------------------------------------------------------------
// useCancelSession
// ---------------------------------------------------------------------------

export function useCancelSession() {
  const perform = useCallback(async ({ classId, sessionDate, reason }: SessionActionInput) => {
    await api.post(`/classes/${classId}/sessions/${sessionDate}/cancel`, { reason });
  }, []);

  const { execute, loading, error, clearError } = useAction(perform);

  return { cancelSession: execute, loading, error, clearError };
}
