"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { DelegatedMembership } from "@/lib/types/paymentProof";

// Fetches delegated memberships for the currently authenticated manager.
// The backend infers programId from the JWT claim — no path param needed.
export function useDelegatedMemberships() {
  const [memberships, setMemberships] = useState<DelegatedMembership[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const listDelegated = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<DelegatedMembership[]>("/payment-proofs/delegated");
      setMemberships(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load delegated memberships.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    listDelegated();
  }, [listDelegated]);

  return { memberships, loading, error, refetch: listDelegated };
}
