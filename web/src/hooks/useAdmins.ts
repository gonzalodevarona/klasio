"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { AdminSummary, AdminListResponse, CreateAdminRequest } from "@/lib/types/admin";

interface UseAdminsParams {
  page?: number;
  size?: number;
  tenantId?: string;
  status?: string; // "ACTIVE" | "INACTIVE" | undefined (all)
}

export function useAdmins(params?: UseAdminsParams): {
  admins: AdminSummary[];
  totalPages: number;
  totalElements: number;
  loading: boolean;
  error: string | null;
  refetch: () => void;
} {
  const [admins, setAdmins] = useState<AdminSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(params?.page ?? 0),
        size: String(params?.size ?? 20),
      });
      if (params?.tenantId) query.set("tenantId", params.tenantId);
      if (params?.status) query.set("status", params.status);

      const data = await api.get<AdminListResponse>(
        `/admin-users?${query.toString()}`
      );
      setAdmins(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load admins.");
    } finally {
      setLoading(false);
    }
  }, [params?.page, params?.size, params?.tenantId, params?.status]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { admins, totalPages, totalElements, loading, error, refetch: fetch };
}

export function useTenantOptions(): {
  options: Record<string, string>;
  loading: boolean;
} {
  const [options, setOptions] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<Record<string, string>>("/admin-users/tenant-options")
      .then(setOptions)
      .catch(() => setOptions({}))
      .finally(() => setLoading(false));
  }, []);

  return { options, loading };
}

export function useUpdateAdmin(): {
  update: (id: string, req: Partial<Omit<CreateAdminRequest, "tenantId" | "password">>) => Promise<AdminSummary>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const update = useCallback(
    async (id: string, req: Partial<Omit<CreateAdminRequest, "tenantId" | "password">>): Promise<AdminSummary> => {
      setLoading(true);
      setError(null);
      try {
        return await api.patch<AdminSummary>(`/admin-users/${id}`, req);
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Failed to update admin.";
        setError(msg);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  const clearError = useCallback(() => setError(null), []);
  return { update, loading, error, clearError };
}

export function useDeactivateAdmin(): {
  deactivate: (id: string) => Promise<void>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const deactivate = useCallback(async (id: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await api.patch<void>(`/admin-users/${id}/deactivate`);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to deactivate admin.";
      setError(msg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);
  return { deactivate, loading, error, clearError };
}

export function useActivateAdmin(): {
  activate: (id: string) => Promise<void>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activate = useCallback(async (id: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await api.patch<void>(`/admin-users/${id}/activate`);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to activate admin.";
      setError(msg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);
  return { activate, loading, error, clearError };
}

export function useCreateAdmin(): {
  create: (req: CreateAdminRequest) => Promise<AdminSummary>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const create = useCallback(async (req: CreateAdminRequest): Promise<AdminSummary> => {
    setLoading(true);
    setError(null);
    try {
      return await api.post<AdminSummary>("/admin-users", req);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to create admin.";
      setError(msg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return { create, loading, error, clearError };
}
