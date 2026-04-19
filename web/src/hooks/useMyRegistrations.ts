"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Registration, RegistrationListResponse } from "@/lib/types/attendance";

interface UseMyRegistrationsParams {
  from?: string;
  to?: string;
  status?: string;
  programId?: string;
}

export function useMyRegistrations(params?: UseMyRegistrationsParams): {
  registrations: Registration[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
} {
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({ page: "0", size: "20" });
      if (params?.from) query.set("from", params.from);
      if (params?.to) query.set("to", params.to);
      if (params?.status) query.set("status", params.status);
      if (params?.programId) query.set("programId", params.programId);

      const data = await api.get<RegistrationListResponse>(
        `/me/registrations?${query.toString()}`
      );
      setRegistrations(data.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your registrations.");
    } finally {
      setLoading(false);
    }
  }, [params?.from, params?.to, params?.status, params?.programId]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { registrations, loading, error, refetch: fetch };
}
