"use client";

import { useState } from "react";
import { api } from "@/lib/api";

export function useCancelRegistration(): {
  cancel: (registrationId: string) => Promise<void>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cancel = async (registrationId: string) => {
    setLoading(true);
    setError(null);
    try {
      await api.delete(`/me/registrations/${registrationId}`);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Failed to cancel registration.";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { cancel, loading, error, clearError: () => setError(null) };
}
