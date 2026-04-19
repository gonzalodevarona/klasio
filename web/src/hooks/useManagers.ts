"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { ManagerSummary, ManagerListResponse, CreateManagerRequest } from "@/lib/types/manager";

interface UseManagersParams {
  page?: number;
  size?: number;
  tenantId?: string;
  status?: string;
}

export function useManagers(params?: UseManagersParams): {
  managers: ManagerSummary[];
  totalPages: number;
  totalElements: number;
  loading: boolean;
  error: string | null;
  refetch: () => void;
} {
  const [managers, setManagers] = useState<ManagerSummary[]>([]);
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
      if (params?.status)   query.set("status", params.status);

      const data = await api.get<ManagerListResponse>(`/manager-users?${query.toString()}`);
      setManagers(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load managers.");
    } finally {
      setLoading(false);
    }
  }, [params?.page, params?.size, params?.tenantId, params?.status]);

  useEffect(() => { fetch(); }, [fetch]);

  return { managers, totalPages, totalElements, loading, error, refetch: fetch };
}

export function useManagerTenantOptions(): {
  options: Record<string, string>;
  loading: boolean;
} {
  const [options, setOptions] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<Record<string, string>>("/manager-users/tenant-options")
      .then(setOptions)
      .catch(() => setOptions({}))
      .finally(() => setLoading(false));
  }, []);

  return { options, loading };
}

export function useCreateManager(): {
  create: (req: CreateManagerRequest) => Promise<ManagerSummary>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const create = useCallback(async (req: CreateManagerRequest): Promise<ManagerSummary> => {
    setLoading(true);
    setError(null);
    try {
      return await api.post<ManagerSummary>("/manager-users", req);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to create manager.";
      setError(msg);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);
  return { create, loading, error, clearError };
}

export function useUpdateManager(): {
  update: (id: string, req: Partial<Omit<CreateManagerRequest, "tenantId" | "password">>) => Promise<ManagerSummary>;
  loading: boolean;
  error: string | null;
  clearError: () => void;
} {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const update = useCallback(
    async (id: string, req: Partial<Omit<CreateManagerRequest, "tenantId" | "password">>): Promise<ManagerSummary> => {
      setLoading(true);
      setError(null);
      try {
        return await api.patch<ManagerSummary>(`/manager-users/${id}`, req);
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Failed to update manager.";
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

export function useDeactivateManager(): {
  deactivate: (id: string) => Promise<void>;
  loading: boolean;
} {
  const [loading, setLoading] = useState(false);

  const deactivate = useCallback(async (id: string): Promise<void> => {
    setLoading(true);
    try {
      await api.patch<void>(`/manager-users/${id}/deactivate`);
    } finally {
      setLoading(false);
    }
  }, []);

  return { deactivate, loading };
}

export function useActivateManager(): {
  activate: (id: string) => Promise<void>;
} {
  const activate = useCallback(async (id: string): Promise<void> => {
    await api.patch<void>(`/manager-users/${id}/activate`);
  }, []);

  return { activate };
}
