"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { usePaymentProofs } from "@/hooks/usePaymentProofs";
import type { ProofQueueItem } from "@/lib/types/paymentProof";
import { IDENTITY_DOCUMENT_TYPES } from "@/lib/types/student";
import { Modal } from "@/components/ui";

interface Props {
  proof: ProofQueueItem;
  onClose: () => void;
  onDone: () => void;
}

function formatDocType(code: string): string {
  return IDENTITY_DOCUMENT_TYPES.find((t) => t.value === code)?.label ?? code;
}

export function ProofReviewModal({ proof, onClose, onDone }: Props) {
  const t = useTranslations("paymentProofs");
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
      setActionError(err instanceof Error ? err.message : t("approvingBtn"));
    }
  }

  async function handleReject() {
    if (!rejectionReason.trim()) {
      setActionError(t("rejectionReasonLabel") + " is required.");
      return;
    }
    setActionError(null);
    try {
      await rejectProof(proof.proofId, rejectionReason.trim());
      onDone();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("rejectingBtn"));
    }
  }

  const isPdf = proof.contentType === "application/pdf";

  return (
    <Modal open onClose={onClose} title={t("reviewModalTitle")} size="xl">
      {/* Student identity */}
      <p className="text-xs text-gray-500 mb-3">
        <span className="font-medium text-gray-800">{proof.studentName}</span>
        <span className="mx-1.5 text-gray-300">·</span>
        <span>{formatDocType(proof.studentIdentityDocumentType)} {proof.studentIdentityNumber}</span>
      </p>

      {/* Plan details card */}
      <div className="rounded-md bg-indigo-50 border border-indigo-100 px-3 py-2 space-y-1 mb-4">
        <p className="text-xs font-semibold text-indigo-800 uppercase tracking-wide">
          {t("planDetailsTitle")}
        </p>
        <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-xs">
          <div>
            <span className="text-gray-500">{t("labelProgram")}</span>
            <span className="font-medium text-gray-800">{proof.programName}</span>
          </div>
          <div>
            <span className="text-gray-500">{t("labelPlan")}</span>
            <span className="font-medium text-gray-800">{proof.planName}</span>
          </div>
          <div>
            <span className="text-gray-500">{t("labelHours")}</span>
            <span className="font-medium text-gray-800">{proof.purchasedHours} h</span>
          </div>
          <div>
            <span className="text-gray-500">{t("labelCost")}</span>
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

      {/* Preview */}
      <div className="bg-gray-50 rounded-md p-4 min-h-[300px] flex items-center justify-center mb-4">
        {urlError && (
          <p className="text-sm text-red-600">{urlError}</p>
        )}
        {!downloadUrl && !urlError && (
          <p className="text-sm text-gray-400">{t("previewLoading")}</p>
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
      <div className="space-y-3">
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
                {t("activateDirectlyLabel")}
              </label>
              <span className="text-xs text-gray-400">
                {t("activateDirectlyHint")}
              </span>
            </div>

            <div className="flex gap-2">
              <button
                onClick={handleApprove}
                disabled={loading || !downloadUrl}
                className="flex-1 rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
              >
                {loading ? t("approvingBtn") : t("approveBtn")}
              </button>
              <button
                onClick={() => { setMode("reject"); setActionError(null); }}
                disabled={loading}
                className="flex-1 rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
              >
                {t("rejectBtn")}
              </button>
            </div>
          </>
        )}

        {mode === "reject" && (
          <div className="space-y-2">
            <label className="block text-xs font-medium text-gray-700">
              {t("rejectionReasonLabel")} <span className="text-red-500">*</span>
            </label>
            <textarea
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              rows={3}
              placeholder={t("rejectionReasonPlaceholder")}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <div className="flex gap-2">
              <button
                onClick={handleReject}
                disabled={loading}
                className="flex-1 rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
              >
                {loading ? t("rejectingBtn") : t("confirmRejectBtn")}
              </button>
              <button
                onClick={() => { setMode("view"); setActionError(null); }}
                disabled={loading}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                {t("backBtn")}
              </button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
