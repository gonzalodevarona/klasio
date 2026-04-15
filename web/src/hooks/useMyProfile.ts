"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { StudentDetail } from "@/lib/types/student";

export function useMyProfile() {
  const [profile, setProfile] = useState<StudentDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<StudentDetail>("/me/profile");
      setProfile(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your profile.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { profile, loading, error, refetch: fetch };
}
