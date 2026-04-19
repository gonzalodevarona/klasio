"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  DelegatedMembership,
  PaymentProofDto,
  PaymentProofResponse,
  ProofQueueItem,
} from "@/lib/types/paymentProof";

/** Polls the pending proof queue count every 60 s. Returns null while loading. */
export function usePendingProofsCount(enabled: boolean): number | null {
  const [count, setCount] = useState<number | null>(null);

  useEffect(() => {
    if (!enabled) return;

    let cancelled = false;

    async function fetch() {
      try {
        const proofs = await api.get<ProofQueueItem[]>("/payment-proofs");
        if (!cancelled) setCount(proofs.length);
      } catch {
        // silently ignore — badge just won't appear
      }
    }

    fetch();
    const interval = setInterval(fetch, 60_000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [enabled]);

  return count;
}

export function usePaymentProofs() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Upload a proof file for a membership.
   * Uses XHR to track upload progress.
   */
  const uploadProof = useCallback(
    (
      membershipId: string,
      file: File,
      onProgress?: (percent: number) => void
    ): Promise<PaymentProofResponse> => {
      return new Promise((resolve, reject) => {
        const formData = new FormData();
        formData.append("file", file);

        const xhr = new XMLHttpRequest();
        xhr.open("POST", `/api/payment-proofs/memberships/${membershipId}/proof`);
        xhr.withCredentials = true;

        if (onProgress) {
          xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
              onProgress(Math.round((e.loaded / e.total) * 100));
            }
          };
        }

        xhr.onload = () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            try {
              resolve(JSON.parse(xhr.responseText) as PaymentProofResponse);
            } catch {
              reject(new Error("Invalid response from server"));
            }
          } else {
            try {
              const err = JSON.parse(xhr.responseText);
              reject(new Error(err?.error?.message ?? `Upload failed (${xhr.status})`));
            } catch {
              reject(new Error(`Upload failed (${xhr.status})`));
            }
          }
        };

        xhr.onerror = () => reject(new Error("Network error during upload"));
        xhr.send(formData);
      });
    },
    []
  );

  /** Get the active proof for a membership. */
  const getProof = useCallback(
    async (membershipId: string): Promise<PaymentProofDto | null> => {
      setLoading(true);
      setError(null);
      try {
        const data = await api.get<PaymentProofDto>(
          `/memberships/${membershipId}/payment-proof`
        );
        return data;
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Failed to load proof.";
        // 404 means no proof yet — not an error for display
        if (msg.includes("404") || msg.includes("PAYMENT_PROOF_NOT_FOUND")) {
          return null;
        }
        setError(msg);
        return null;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  /** List all proofs for a membership (timeline). */
  const listMembershipProofs = useCallback(
    async (membershipId: string): Promise<PaymentProofResponse[]> => {
      setLoading(true);
      setError(null);
      try {
        return await api.get<PaymentProofResponse[]>(
          `/memberships/${membershipId}/payment-proofs`
        );
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load proof history.");
        return [];
      } finally {
        setLoading(false);
      }
    },
    []
  );

  /** List all PENDING proofs for admin queue. */
  const listPendingProofs = useCallback(async (): Promise<ProofQueueItem[]> => {
    setLoading(true);
    setError(null);
    try {
      return await api.get<ProofQueueItem[]>("/payment-proofs");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load proof queue.");
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  /** Get a presigned download URL for a proof. */
  const getDownloadUrl = useCallback(async (proofId: string): Promise<string | null> => {
    try {
      const data = await api.get<{ downloadUrl: string }>(
        `/payment-proofs/${proofId}/download-url`
      );
      return data.downloadUrl;
    } catch {
      return null;
    }
  }, []);

  /** Approve a proof. */
  const approveProof = useCallback(
    async (proofId: string, activateDirectly: boolean): Promise<PaymentProofResponse> => {
      setLoading(true);
      setError(null);
      try {
        return await api.post<PaymentProofResponse>(
          `/payment-proofs/${proofId}/approve`,
          { activateDirectly }
        );
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Failed to approve proof.";
        setError(msg);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  /** Reject a proof with a mandatory reason. */
  const rejectProof = useCallback(
    async (proofId: string, rejectionReason: string): Promise<PaymentProofResponse> => {
      setLoading(true);
      setError(null);
      try {
        return await api.post<PaymentProofResponse>(
          `/payment-proofs/${proofId}/reject`,
          { rejectionReason }
        );
      } catch (err) {
        const msg = err instanceof Error ? err.message : "Failed to reject proof.";
        setError(msg);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return {
    loading,
    error,
    uploadProof,
    getProof,
    listMembershipProofs,
    listPendingProofs,
    getDownloadUrl,
    approveProof,
    rejectProof,
  };
}
