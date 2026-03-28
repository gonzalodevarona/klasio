"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  HourTransactionListResponse,
  HourTransactionSummary,
} from "@/lib/types/membership";

export function useHourTransactions(
  membershipId: string,
  page = 0,
  size = 20
) {
  const [transactions, setTransactions] = useState<HourTransactionSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<HourTransactionListResponse>(
        `/memberships/${membershipId}/transactions?page=${page}&size=${size}`
      );
      setTransactions(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load transactions.");
    } finally {
      setLoading(false);
    }
  }, [membershipId, page, size]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  return { transactions, totalPages, totalElements, loading, error, refetch: fetchTransactions };
}
