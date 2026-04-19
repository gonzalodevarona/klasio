"use client";

import { useEffect, useState } from "react";
import { usePaymentProofs } from "@/hooks/usePaymentProofs";
import type { ProofQueueItem } from "@/lib/types/paymentProof";
import { IDENTITY_DOCUMENT_TYPES } from "@/lib/types/student";

interface Props {
  proof: ProofQueueItem;
  onClose: () => void;
  onDone: () => void;
}

function formatDocType(code: string): string {
  return IDENTITY_DOCUMENT_TYPES.find((t) => t.value === code)?.label ?? code;
}

export function ProofReviewModal({ proof, onClose, onDone }: Props) {
  const { getDownloadUrl, approveProof, rejectProof, loading } = usePaymentProofs();
  const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
  const [urlError, setUrlError] = useState<string | null>(null);
  const [mode, setMode] = useState<"view" | "reject">("view");
  const [rejectionReason, setRejectionReason] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);
  const [activateDirectly, setActivateDirectly] = useState(true);

  useEffect(() => {
    getDownloadUrl(proof.proofId).then((url) => {
      if (url) setDownloadUrl(url);
      else setUrlError("Could not load preview.");
    });
  }, [proof.proofId, getDownloadUrl]);

  async function handleApprove() {
    setActionError(null);
    try {
      await approveProof(proof.proofId, activateDirectly);
      onDone();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Approval failed.");
    }
  }

  async function handleReject() {
    if (!rejectionReason.trim()) {
      setActionError("Rejection reason is required.");
      return;
    }
    setActionError(null);
    try {
      await rejectProof(proof.proofId, rejectionReason.trim());
      onDone();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Rejection failed.");
    }
  }

  const isPdf = proof.contentType === "application/pdf";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="flex flex-col bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-start justify-between px-5 py-4 border-b border-gray-200 gap-4">
          <div className="min-w-0 space-y-2">
            <h2 className="text-base font-semibold text-gray-900">Review Payment Proof</h2>

            {/* Student identity */}
            <p className="text-xs text-gray-500">
              <span className="font-medium text-gray-800">{proof.studentName}</span>
              <span className="mx-1.5 text-gray-300">·</span>
              <span>{formatDocType(proof.studentIdentityDocumentType)} {proof.studentIdentityNumber}</span>
            </p>

            {/* Plan details card */}
            <div className="rounded-md bg-indigo-50 border border-indigo-100 px-3 py-2 space-y-1">
              <p className="text-xs font-semibold text-indigo-800 uppercase tracking-wide">
                Plan Details
              </p>
              <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-xs">
                <div>
                  <span className="text-gray-500">Program: </span>
                  <span className="font-medium text-gray-800">{proof.programName}</span>
                </div>
                <div>
                  <span className="text-gray-500">Plan: </span>
                  <span className="font-medium text-gray-800">{proof.planName}</span>
                </div>
                <div>
                  <span className="text-gray-500">Hours: </span>
                  <span className="font-medium text-gray-800">{proof.purchasedHours} h</span>
                </div>
                <div>
                  <span className="text-gray-500">Cost: </span>
                  <span className="font-medium text-gray-800">
                    {new Intl.NumberFormat("es-CO", {
                      style: "currency",
                      currency: "COP",
                      maximumFractionDigits: 0,
                    }).format(proof.planCost)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none shrink-0"
            aria-label="Close"
          >
            ×
          </button>
        </div>

        {/* Preview */}
        <div className="flex-1 overflow-auto bg-gray-50 p-4 min-h-[300px] flex items-center justify-center">
          {urlError && (
            <p className="text-sm text-red-600">{urlError}</p>
          )}
          {!downloadUrl && !urlError && (
            <p className="text-sm text-gray-400">Loading preview…</p>
          )}
          {downloadUrl && isPdf && (
            <iframe
              src={downloadUrl}
              title="Payment proof PDF"
              className="w-full h-[400px] rounded border border-gray-200"
            />
          )}
          {downloadUrl && !isPdf && (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={downloadUrl}
              alt="Payment proof"
              className="max-h-[400px] max-w-full rounded border border-gray-200 object-contain"
            />
          )}
        </div>

        {/* Actions */}
        <div className="border-t border-gray-200 px-5 py-4 space-y-3">
          {actionError && (
            <p className="text-xs text-red-600">{actionError}</p>
          )}

          {mode === "view" && (
            <>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={activateDirectly}
                    onChange={(e) => setActivateDirectly(e.target.checked)}
                    className="rounded border-gray-300"
                  />
                  Activate membership directly
                </label>
                <span className="text-xs text-gray-400">
                  (uncheck to delegate to manager)
                </span>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={handleApprove}
                  disabled={loading || !downloadUrl}
                  className="flex-1 rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                >
                  {loading ? "Processing…" : "Approve"}
                </button>
                <button
                  onClick={() => { setMode("reject"); setActionError(null); }}
                  disabled={loading}
                  className="flex-1 rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
                >
                  Reject
                </button>
              </div>
            </>
          )}

          {mode === "reject" && (
            <div className="space-y-2">
              <label className="block text-xs font-medium text-gray-700">
                Rejection reason <span className="text-red-500">*</span>
              </label>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={3}
                placeholder="Explain why this proof is being rejected…"
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <div className="flex gap-2">
                <button
                  onClick={handleReject}
                  disabled={loading}
                  className="flex-1 rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                >
                  {loading ? "Processing…" : "Confirm Reject"}
                </button>
                <button
                  onClick={() => { setMode("view"); setActionError(null); }}
                  disabled={loading}
                  className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                >
                  Back
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
