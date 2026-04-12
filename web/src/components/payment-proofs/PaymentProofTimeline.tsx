"use client";

import { useCallback, useEffect, useState } from "react";
import { usePaymentProofs } from "@/hooks/usePaymentProofs";
import { ProofStatusBadge } from "./ProofStatusBadge";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import type { PaymentProofResponse, ProofStatus } from "@/lib/types/paymentProof";
import type { MembershipStatus } from "@/lib/types/membership";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

// Statuses where the proof is the "current" one — show membership state instead of raw proof status
const LIVE_PROOF_STATUSES: ProofStatus[] = ["PENDING", "APPROVED"];

// Dot color based on membership status for the live proof row
const MEMBERSHIP_DOT_STYLES: Record<MembershipStatus, string> = {
  ACTIVE:                     "border-green-500 bg-green-100",
  INACTIVE:                   "border-orange-500 bg-orange-100",
  EXPIRED:                    "border-red-500 bg-red-100",
  PENDING_PAYMENT:            "border-gray-400 bg-gray-100",
  PENDING_PAYMENT_VALIDATION: "border-yellow-500 bg-yellow-100",
  PENDING_MANAGER_ACTIVATION: "border-amber-500 bg-amber-100",
};

interface Props {
  membershipId: string;
  membershipStatus?: MembershipStatus;
}

export function PaymentProofTimeline({ membershipId, membershipStatus }: Props) {
  const { listMembershipProofs, getDownloadUrl, loading, error } =
    usePaymentProofs();
  const [proofs, setProofs] = useState<PaymentProofResponse[]>([]);
  const [expandedProofId, setExpandedProofId] = useState<string | null>(null);
  const [downloadUrls, setDownloadUrls] = useState<Record<string, string>>({});
  const [fetchingUrl, setFetchingUrl] = useState<string | null>(null);

  useEffect(() => {
    listMembershipProofs(membershipId).then(setProofs);
  }, [membershipId, listMembershipProofs]);

  const handleViewProof = useCallback(
    async (proofId: string) => {
      // If URL already cached, open directly
      if (downloadUrls[proofId]) {
        window.open(downloadUrls[proofId], "_blank", "noopener,noreferrer");
        return;
      }
      // Lazy-load presigned URL
      setFetchingUrl(proofId);
      try {
        const url = await getDownloadUrl(proofId);
        if (url) {
          setDownloadUrls((prev) => ({ ...prev, [proofId]: url }));
          window.open(url, "_blank", "noopener,noreferrer");
        }
      } finally {
        setFetchingUrl(null);
      }
    },
    [downloadUrls, getDownloadUrl]
  );

  if (loading && proofs.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-2">
          Payment Proof History
        </h3>
        <p className="text-xs text-gray-400">Loading...</p>
      </div>
    );
  }

  if (proofs.length === 0) {
    return null;
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 space-y-4">
      <h3 className="text-sm font-semibold text-gray-700">
        Payment Proof History
      </h3>

      {error && (
        <p className="text-xs text-red-600">{error}</p>
      )}

      <div className="relative">
        {/* Vertical timeline line */}
        <div className="absolute left-3 top-2 bottom-2 w-px bg-gray-200" />

        <div className="space-y-4">
          {proofs.map((proof, index) => {
            const isExpanded = expandedProofId === proof.proofId;

            // The most recent proof (index 0) with a live status (PENDING or APPROVED)
            // should reflect the membership lifecycle state rather than the raw proof status.
            const isLiveProof =
              index === 0 &&
              membershipStatus != null &&
              LIVE_PROOF_STATUSES.includes(proof.status);

            const dotStyle = isLiveProof
              ? MEMBERSHIP_DOT_STYLES[membershipStatus!]
              : proof.status === "APPROVED"
                ? "border-green-500 bg-green-100"
                : proof.status === "REJECTED"
                  ? "border-red-500 bg-red-100"
                  : proof.status === "PENDING"
                    ? "border-yellow-500 bg-yellow-100"
                    : "border-gray-400 bg-gray-100";

            return (
              <div key={proof.proofId} className="relative pl-8">
                {/* Timeline dot */}
                <div
                  className={`absolute left-1.5 top-1.5 w-3 h-3 rounded-full border-2 ${dotStyle}`}
                />

                <div
                  className="rounded-md border border-gray-100 bg-gray-50 p-3 cursor-pointer hover:bg-gray-100 transition-colors"
                  onClick={() =>
                    setExpandedProofId(isExpanded ? null : proof.proofId)
                  }
                >
                  {/* Header row */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-gray-500">
                        {formatDate(proof.uploadedAt)}
                      </span>
                      {isLiveProof ? (
                        <MembershipStatusBadge status={membershipStatus!} />
                      ) : (
                        <ProofStatusBadge status={proof.status} />
                      )}
                    </div>
                    <svg
                      className={`w-4 h-4 text-gray-400 transition-transform ${
                        isExpanded ? "rotate-180" : ""
                      }`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 9l-7 7-7-7"
                      />
                    </svg>
                  </div>

                  {/* Expanded details */}
                  {isExpanded && (
                    <div className="mt-3 space-y-2">
                      <div className="flex items-center gap-2 text-xs text-gray-600">
                        <span className="truncate">
                          {proof.originalFileName}
                        </span>
                        <span className="text-gray-400">
                          ({formatBytes(proof.fileSizeBytes)})
                        </span>
                      </div>

                      {proof.validatedAt && (
                        <p className="text-xs text-gray-500">
                          Reviewed: {formatDate(proof.validatedAt)}
                        </p>
                      )}

                      {proof.status === "REJECTED" &&
                        proof.rejectionReason && (
                          <div className="rounded-md bg-red-50 border border-red-200 p-2">
                            <p className="text-xs text-red-600">
                              {proof.rejectionReason}
                            </p>
                          </div>
                        )}

                      {/* View document button (lazy presigned URL) */}
                      {(proof.status === "APPROVED" ||
                        proof.status === "PENDING") && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleViewProof(proof.proofId);
                          }}
                          disabled={fetchingUrl === proof.proofId}
                          className="inline-flex items-center gap-1.5 text-xs font-medium text-indigo-600
                            hover:text-indigo-800 hover:underline disabled:opacity-50 transition-colors"
                        >
                          <svg
                            className="w-3.5 h-3.5 shrink-0"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                            />
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={2}
                              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7
                                 -1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                            />
                          </svg>
                          {fetchingUrl === proof.proofId
                            ? "Getting link..."
                            : "View document"}
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
