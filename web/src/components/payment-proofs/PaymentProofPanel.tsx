"use client";

import { useEffect, useRef, useState } from "react";
import { usePaymentProofs } from "@/hooks/usePaymentProofs";
import { ProofStatusBadge } from "./ProofStatusBadge";
import type { PaymentProofDto } from "@/lib/types/paymentProof";
import type { MembershipStatus } from "@/lib/types/membership";

const ACCEPTED_MIME = "application/pdf,image/jpeg,image/png";
const MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

// Statuses where uploading a new proof makes no sense:
// - PENDING_PAYMENT_VALIDATION: proof already submitted, awaiting admin review
// - PENDING_MANAGER_ACTIVATION: payment approved, awaiting manager activation
// - ACTIVE / INACTIVE: membership is live or depleted — payment already validated
// PENDING_PAYMENT and EXPIRED are intentionally excluded so the upload form shows.
const UPLOAD_BLOCKED_STATUSES: MembershipStatus[] = [
  "PENDING_PAYMENT_VALIDATION",
  "PENDING_MANAGER_ACTIVATION",
  "ACTIVE",
  "INACTIVE",
];

interface Props {
  membershipId: string;
  membershipStatus?: MembershipStatus;
}

export function PaymentProofPanel({ membershipId, membershipStatus }: Props) {
  const { uploadProof, getProof, getDownloadUrl, loading, error } = usePaymentProofs();
  const [proof, setProof] = useState<PaymentProofDto | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploading, setUploading] = useState(false);

  // Preview stage
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  // Lazy download link
  const [downloadUrl, setDownloadUrl] = useState<string | null>(null);
  const [fetchingUrl, setFetchingUrl] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    getProof(membershipId).then(setProof);
  }, [membershipId, getProof]);

  // Revoke object URL when preview is cleared to avoid memory leaks
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  async function handleViewProof() {
    if (!proof?.proofId) return;
    if (downloadUrl) {
      window.open(downloadUrl, "_blank", "noopener,noreferrer");
      return;
    }
    setFetchingUrl(true);
    try {
      const url = await getDownloadUrl(proof.proofId);
      if (url) {
        setDownloadUrl(url);
        window.open(url, "_blank", "noopener,noreferrer");
      }
    } finally {
      setFetchingUrl(false);
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadError(null);

    if (file.size > MAX_SIZE_BYTES) {
      setUploadError("File exceeds the 5 MB size limit.");
      return;
    }

    if (!["application/pdf", "image/jpeg", "image/png"].includes(file.type)) {
      setUploadError("Unsupported file type. Please upload a PDF, JPG, or PNG.");
      return;
    }

    // Enter preview stage — don't upload yet
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  }

  function handleCancel() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingFile(null);
    setPreviewUrl(null);
    setUploadError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  async function handleConfirmUpload() {
    if (!pendingFile) return;

    setUploading(true);
    setProgress(0);
    setUploadError(null);

    try {
      const result = await uploadProof(membershipId, pendingFile, setProgress);
      setProof({
        proofId: result.proofId,
        membershipId: result.membershipId,
        status: result.status,
        originalFileName: result.originalFileName,
        uploadedAt: result.uploadedAt,
        rejectionReason: result.rejectionReason,
        validatedAt: result.validatedAt,
        validatedBy: result.validatedBy,
      });
      // Clear preview after successful upload
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPendingFile(null);
      setPreviewUrl(null);
    } catch (err) {
      setUploadError(err instanceof Error ? err.message : "Upload failed. Please try again.");
    } finally {
      setUploading(false);
      setProgress(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  const proofApproved = membershipStatus
    ? UPLOAD_BLOCKED_STATUSES.includes(membershipStatus)
    : proof?.status === "APPROVED";
  const showUploadSection = !proofApproved && (!proof || proof.status === "REJECTED");

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 space-y-4">
      <h3 className="text-sm font-semibold text-gray-700">Payment Proof</h3>

      {/* Current proof status */}
      {proof ? (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500 truncate">{proof.originalFileName}</span>
            <ProofStatusBadge status={proof.status} />
          </div>

          {/* Approved: lazy link to open the file in a new tab */}
          {proofApproved && (
            <button
              onClick={handleViewProof}
              disabled={fetchingUrl}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-indigo-600
                hover:text-indigo-800 hover:underline disabled:opacity-50 transition-colors"
            >
              <svg className="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7
                     -1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              {fetchingUrl ? "Getting link…" : "View approved proof"}
            </button>
          )}

          {proof.status === "REJECTED" && proof.rejectionReason && (
            <div className="rounded-md bg-red-50 border border-red-200 p-3">
              <p className="text-xs font-medium text-red-700">Rejection reason:</p>
              <p className="text-xs text-red-600 mt-1">{proof.rejectionReason}</p>
            </div>
          )}
        </div>
      ) : (
        !loading && (
          <p className="text-xs text-gray-400">No proof uploaded yet.</p>
        )
      )}

      {/* Upload section: visible when no proof, or when rejected (re-upload) */}
      {showUploadSection && !pendingFile && (
        <div className="space-y-2">
          <label className="block">
            <span className="text-xs font-medium text-gray-600">
              {proof?.status === "REJECTED" ? "Upload a new proof" : "Upload proof"}
            </span>
            <input
              ref={fileInputRef}
              type="file"
              accept={ACCEPTED_MIME}
              onChange={handleFileChange}
              disabled={uploading}
              className="mt-1 block w-full text-xs text-gray-500
                file:mr-3 file:py-1.5 file:px-3
                file:rounded file:border-0
                file:text-xs file:font-medium
                file:bg-indigo-50 file:text-indigo-700
                hover:file:bg-indigo-100
                disabled:opacity-50"
            />
          </label>
          <p className="text-xs text-gray-400">PDF, JPG, or PNG · Max 5 MB</p>
          {uploadError && (
            <p className="text-xs text-red-600">{uploadError}</p>
          )}
        </div>
      )}

      {/* Preview stage */}
      {pendingFile && previewUrl && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-gray-700">Review your file before submitting</p>
          </div>

          {/* File meta */}
          <div className="flex items-center gap-2 rounded-md bg-gray-50 border border-gray-200 px-3 py-2">
            <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span className="text-xs text-gray-700 truncate flex-1">{pendingFile.name}</span>
            <span className="text-xs text-gray-400 shrink-0">{formatBytes(pendingFile.size)}</span>
          </div>

          {/* Preview */}
          <div className="rounded-md overflow-hidden border border-gray-200 bg-gray-50">
            {pendingFile.type === "application/pdf" ? (
              <iframe
                src={previewUrl}
                title="PDF preview"
                className="w-full h-64"
              />
            ) : (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={previewUrl}
                alt="Proof preview"
                className="w-full max-h-64 object-contain"
              />
            )}
          </div>

          {/* Progress bar (shown during upload) */}
          {uploading && progress !== null && (
            <div className="w-full bg-gray-200 rounded-full h-1.5">
              <div
                className="bg-indigo-500 h-1.5 rounded-full transition-all duration-150"
                style={{ width: `${progress}%` }}
              />
            </div>
          )}

          {uploadError && (
            <p className="text-xs text-red-600">{uploadError}</p>
          )}

          {/* Action buttons */}
          <div className="flex gap-2">
            <button
              onClick={handleCancel}
              disabled={uploading}
              className="flex-1 text-xs font-medium px-3 py-2 rounded-md border border-gray-300
                text-gray-600 hover:bg-gray-50 transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmUpload}
              disabled={uploading}
              className="flex-1 text-xs font-medium px-3 py-2 rounded-md
                bg-indigo-600 text-white hover:bg-indigo-700 transition-colors disabled:opacity-50"
            >
              {uploading ? "Uploading…" : "Submit proof"}
            </button>
          </div>
        </div>
      )}

      {/* Generic error from hook */}
      {error && !uploadError && (
        <p className="text-xs text-red-600">{error}</p>
      )}
    </div>
  );
}
