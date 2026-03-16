"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  TenantDetail,
  TenantListResponse,
  TenantStatus,
  TenantSummary,
} from "@/lib/types/tenant";

export function useTenants(page = 0, size = 10, status?: TenantStatus) {
  const [tenants, setTenants] = useState<TenantSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTenants = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const statusParam = status ? `&status=${status}` : "";
      const data = await api.get<TenantListResponse>(
        `/tenants?page=${page}&size=${size}${statusParam}`
      );
      setTenants(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load tenants."
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, status]);

  useEffect(() => {
    fetchTenants();
  }, [fetchTenants]);

  return { tenants, totalPages, totalElements, loading, error, refetch: fetchTenants };
}

export function useTenantDetail(slug: string) {
  const [tenant, setTenant] = useState<TenantDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTenant = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await api.get<TenantDetail>(`/tenants/${slug}`);
      setTenant(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load tenant details."
      );
    } finally {
      setLoading(false);
    }
  }, [slug]);

  useEffect(() => {
    fetchTenant();
  }, [fetchTenant]);

  return { tenant, loading, error, refetch: fetchTenant };
}
