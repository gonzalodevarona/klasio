"use client";
import { useEffect, useState } from "react";

export type UserSummary = { id: string; fullName: string; role: string };
export type UserSummaryMap = Record<string, UserSummary>;

export function useUsersByIds(ids: string[]) {
  const [users, setUsers] = useState<UserSummaryMap>({});
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const dedup = Array.from(new Set(ids)).sort();
  const key = dedup.join(",");

  useEffect(() => {
    if (dedup.length === 0) {
      setUsers({});
      setIsLoading(false);
      return;
    }
    let aborted = false;
    setIsLoading(true);
    setError(null);
    fetch(`/api/v1/users/by-ids?ids=${encodeURIComponent(key)}`, { credentials: "include" })
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        const arr: UserSummary[] = await r.json();
        if (aborted) return;
        const map: UserSummaryMap = {};
        for (const u of arr) map[u.id] = u;
        setUsers(map);
      })
      .catch((e) => { if (!aborted) setError(e as Error); })
      .finally(() => { if (!aborted) setIsLoading(false); });
    return () => { aborted = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return { users, isLoading, error };
}
