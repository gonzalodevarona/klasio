"use client";

import { useEffect, useRef, useState } from "react";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import { useProgramPlansByProgram } from "@/hooks/usePrograms";
import { ProgramPlanSummary } from "@/lib/types/program";

const ACCEPTED_MIME = "application/pdf,image/jpeg,image/png";
const MAX_SIZE_BYTES = 5 * 1024 * 1024;

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

interface Props {
  /** Pre-selected program when renewing — overrides the dropdown */
  initialProgramId?: string;
  /** Pre-selected plan when renewing — overrides the dropdown */
  initialPlanId?: string;
  /** Banner text shown when renewing */
  renewBanner?: string;
  onSubmit: (planId: string, file: File) => Promise<void>;
  onCancel: () => void;
}

export default function StudentMembershipCreationForm({
  initialProgramId,
  initialPlanId,
  renewBanner,
  onSubmit,
  onCancel,
}: Props) {
  const { enrollments, loading: enrollmentsLoading } = useMyEnrollments();

  const [selectedProgramId, setSelectedProgramId] = useState<string>(initialProgramId ?? "");
  const { plans, loading: plansLoading } = useProgramPlansByProgram(
    selectedProgramId || null,
    "HOURS_BASED"
  );

  const [selectedPlanId, setSelectedPlanId] = useState<string>(initialPlanId ?? "");

  // File upload state
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Auto-select program if student has exactly one enrollment
  useEffect(() => {
    if (!initialProgramId && enrollments.length === 1) {
      setSelectedProgramId(enrollments[0].programId);
    }
  }, [enrollments, initialProgramId]);

  // Reset plan selection when program changes (unless pre-filled)
  useEffect(() => {
    if (!initialPlanId) {
      setSelectedPlanId("");
    }
  }, [selectedProgramId, initialPlanId]);

  // Auto-select pre-filled plan once plans load
  useEffect(() => {
    if (initialPlanId && plans.length > 0) {
      const found = plans.find((p) => p.id === initialPlanId);
      if (found) setSelectedPlanId(found.id);
    }
  }, [initialPlanId, plans]);

  const selectedPlan: ProgramPlanSummary | undefined = plans.find((p) => p.id === selectedPlanId);
  const activeEnrollments = enrollments.filter((e) => e.status === "ACTIVE");
  const isValid = selectedPlanId !== "" && pendingFile !== null;

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileError(null);

    if (file.size > MAX_SIZE_BYTES) {
      setFileError("File exceeds the 5 MB size limit.");
      return;
    }
    if (!["application/pdf", "image/jpeg", "image/png"].includes(file.type)) {
      setFileError("Unsupported type. Upload a PDF, JPG, or PNG.");
      return;
    }

    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  }

  function handleRemoveFile() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingFile(null);
    setPreviewUrl(null);
    setFileError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValid) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      await onSubmit(selectedPlanId, pendingFile!);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "Something went wrong. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (enrollmentsLoading) {
    return <p className="py-8 text-center text-sm text-gray-500">Loading your enrollments…</p>;
  }

  if (activeEnrollments.length === 0) {
    return (
      <div className="rounded-md bg-yellow-50 border border-yellow-200 p-4 text-sm text-yellow-800">
        You are not enrolled in any program. Please contact your league administrator to get enrolled before creating a membership.
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {renewBanner && (
        <div className="rounded-md bg-indigo-50 border border-indigo-200 px-4 py-3 text-sm text-indigo-800">
          {renewBanner}
        </div>
      )}

      {/* Program selector */}
      {activeEnrollments.length > 1 && !initialProgramId && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Program</label>
          <select
            value={selectedProgramId}
            onChange={(e) => setSelectedProgramId(e.target.value)}
            required
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">— Select a program —</option>
            {activeEnrollments.map((e) => (
              <option key={e.programId} value={e.programId}>
                {e.programName} ({e.level})
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Plan selector */}
      {selectedProgramId && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Plan</label>
          {plansLoading ? (
            <p className="text-sm text-gray-400">Loading plans…</p>
          ) : plans.length === 0 ? (
            <p className="text-sm text-gray-400">No active plans available for this program.</p>
          ) : (
            <select
              value={selectedPlanId}
              onChange={(e) => setSelectedPlanId(e.target.value)}
              required
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">— Select a plan —</option>
              {plans.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          )}
        </div>
      )}

      {/* Plan detail card */}
      {selectedPlan && (
        <div className="rounded-lg border border-indigo-100 bg-indigo-50 px-4 py-3 space-y-2">
          <p className="text-sm font-semibold text-indigo-900">{selectedPlan.name}</p>
          <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
            <div>
              <span className="text-indigo-600 font-medium">Modality: </span>
              <span className="text-indigo-800">
                {selectedPlan.modality === "HOURS_BASED" ? "Hours-based" : "Classes per week"}
              </span>
            </div>
            {selectedPlan.modality === "HOURS_BASED" && selectedPlan.hours != null && (
              <div>
                <span className="text-indigo-600 font-medium">Hours included: </span>
                <span className="text-indigo-800">{selectedPlan.hours}h</span>
              </div>
            )}
            <div>
              <span className="text-indigo-600 font-medium">Cost: </span>
              <span className="text-indigo-800">${selectedPlan.cost.toLocaleString()}</span>
            </div>
          </div>
        </div>
      )}

      {/* Payment proof upload */}
      {selectedPlanId && (
        <div className="space-y-3">
          <div>
            <p className="text-sm font-medium text-gray-700">Payment proof <span className="text-red-500">*</span></p>
            <p className="text-xs text-gray-400 mt-0.5">PDF, JPG, or PNG · Max 5 MB</p>
          </div>

          {!pendingFile ? (
            <div>
              <input
                ref={fileInputRef}
                type="file"
                accept={ACCEPTED_MIME}
                onChange={handleFileChange}
                className="block w-full text-xs text-gray-500
                  file:mr-3 file:py-1.5 file:px-3
                  file:rounded file:border-0
                  file:text-xs file:font-medium
                  file:bg-indigo-50 file:text-indigo-700
                  hover:file:bg-indigo-100"
              />
              {fileError && <p className="mt-1 text-xs text-red-600">{fileError}</p>}
            </div>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center gap-2 rounded-md bg-gray-50 border border-gray-200 px-3 py-2">
                <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span className="text-xs text-gray-700 truncate flex-1">{pendingFile.name}</span>
                <span className="text-xs text-gray-400 shrink-0">{formatBytes(pendingFile.size)}</span>
                <button
                  type="button"
                  onClick={handleRemoveFile}
                  className="text-xs text-red-500 hover:text-red-700 shrink-0"
                >
                  Remove
                </button>
              </div>

              {previewUrl && (
                <div className="rounded-md overflow-hidden border border-gray-200 bg-gray-50">
                  {pendingFile.type === "application/pdf" ? (
                    <iframe src={previewUrl} title="PDF preview" className="w-full h-48" />
                  ) : (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={previewUrl} alt="Proof preview" className="w-full max-h-48 object-contain" />
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {submitError && (
        <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
          {submitError}
        </div>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={!isValid || submitting}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {submitting
            ? pendingFile
              ? "Creating & uploading…"
              : "Creating…"
            : "Subscribe to plan"}
        </button>
      </div>
    </form>
  );
}
