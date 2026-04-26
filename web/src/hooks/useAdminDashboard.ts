"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";

export interface DashboardStudent {
  id: string;
  name: string;
  programName: string | null;
  membershipStatus: string | null;
  availableHours: number | null;
  purchasedHours: number | null;
}

export interface AdminDashboardData {
  studentCount: number;
  newStudentsThisMonth: number;
  totalHoursConsumed: number;
  pendingPaymentProofs: number;
  activeProgramCount: number;
  students: DashboardStudent[];
}

export function useAdminDashboard() {
  const [data, setData] = useState<AdminDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.get<AdminDashboardData>("/admin/dashboard");
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load dashboard.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, refetch: load };
}
