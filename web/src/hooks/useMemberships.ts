"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  AdjustHoursRequest,
  CreateMembershipRequest,
  CreateSelfMembershipRequest,
  MembershipDetail,
  MembershipHistoryEntry,
  MembershipListResponse,
  MembershipStatus,
  MembershipSummary,
  ValidatePaymentRequest,
} from "@/lib/types/membership";

export function useMemberships(
  studentId: string,
  programId?: string,
  status?: MembershipStatus,
  page = 0,
  size = 20
) {
  const [memberships, setMemberships] = useState<MembershipSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMemberships = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        studentId,
        page: String(page),
        size: String(size),
      });
      if (programId) params.set("programId", programId);
      if (status) params.set("status", status);

      const data = await api.get<MembershipListResponse>(
        `/memberships?${params.toString()}`
      );
      setMemberships(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load memberships.");
    } finally {
      setLoading(false);
    }
  }, [studentId, programId, status, page, size]);

  useEffect(() => {
    fetchMemberships();
  }, [fetchMemberships]);

  return { memberships, totalPages, totalElements, loading, error, refetch: fetchMemberships };
}

export function useMyMemberships() {
  const [memberships, setMemberships] = useState<MembershipSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<MembershipSummary[]>("/me/memberships");
      setMemberships(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your memberships.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetch(); }, [fetch]);

  return { memberships, loading, error, refetch: fetch };
}

export function useMembershipDetail(membershipId: string) {
  const [membership, setMembership] = useState<MembershipDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMembership = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<MembershipDetail>(`/memberships/${membershipId}`);
      setMembership(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load membership.");
    } finally {
      setLoading(false);
    }
  }, [membershipId]);

  useEffect(() => {
    fetchMembership();
  }, [fetchMembership]);

  return { membership, loading, error, refetch: fetchMembership };
}

export function useMembershipActions() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createMembership = useCallback(async (request: CreateMembershipRequest) => {
    setLoading(true);
    setError(null);
    try {
      return await api.post<MembershipDetail>("/memberships", request);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to create membership.";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const validatePayment = useCallback(
    async (membershipId: string, request: ValidatePaymentRequest) => {
      setLoading(true);
      setError(null);
      try {
        return await api.patch<MembershipDetail>(
          `/memberships/${membershipId}/validate-payment`,
          request
        );
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to validate payment.";
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  const activateMembership = useCallback(async (membershipId: string) => {
    setLoading(true);
    setError(null);
    try {
      return await api.patch<MembershipDetail>(
        `/memberships/${membershipId}/activate`,
        {}
      );
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to activate membership.";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const adjustHours = useCallback(
    async (membershipId: string, request: AdjustHoursRequest) => {
      setLoading(true);
      setError(null);
      try {
        return await api.post<MembershipDetail>(
          `/memberships/${membershipId}/adjust-hours`,
          request
        );
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to adjust hours.";
        setError(message);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  const createSelfMembership = useCallback(async (request: CreateSelfMembershipRequest, file: File) => {
    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append("planId", request.planId);
      formData.append("file", file);
      return await api.post<MembershipDetail>("/me/memberships", formData);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to create membership.";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const renewMembership = useCallback(async (membershipId: string, file: File) => {
    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      return await api.post<MembershipDetail>(
        `/me/memberships/${membershipId}/renew`,
        formData
      );
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to renew membership.";
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { createMembership, validatePayment, activateMembership, adjustHours, createSelfMembership, renewMembership, loading, error };
}

export function useMembershipHistory(studentId: string, programId: string) {
  const [history, setHistory] = useState<MembershipHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<MembershipHistoryEntry[]>(
        `/students/${studentId}/programs/${programId}/membership-history`
      );
      setHistory(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load membership history.");
    } finally {
      setLoading(false);
    }
  }, [studentId, programId]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const exportCsv = useCallback(async () => {
    const token =
      typeof window !== "undefined" ? localStorage.getItem("auth_token") : null;
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/students/${studentId}/programs/${programId}/membership-history`,
      {
        headers: {
          Accept: "text/csv",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      }
    );
    if (!response.ok) throw new Error("Failed to export CSV");
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "membership-history.csv";
    a.click();
    URL.revokeObjectURL(url);
  }, [studentId, programId]);

  return { history, loading, error, refetch: fetchHistory, exportCsv };
}
