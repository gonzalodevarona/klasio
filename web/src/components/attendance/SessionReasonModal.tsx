"use client";

import { useState, useEffect } from "react";

interface Props {
  open: boolean;
  title: string;
  description?: string;
  submitLabel: string;
  submitVariant: "amber" | "red";
  initialReason?: string;
  onClose: () => void;
  onSubmit: (reason: string) => Promise<void>;
  submitting: boolean;
  error: string | null;
}

const SUBMIT_VARIANT_CLASSES: Record<"amber" | "red", string> = {
  amber: "bg-amber-600 hover:bg-amber-700 focus:ring-amber-500",
  red:   "bg-red-600 hover:bg-red-700 focus:ring-red-500",
};

export default function SessionReasonModal({
  open,
  title,
  description,
  submitLabel,
  submitVariant,
  initialReason = "",
  onClose,
  onSubmit,
  submitting,
  error,
}: Props) {
  const [reason, setReason] = useState(initialReason);

  // Reset reason whenever the modal opens or initialReason changes.
  useEffect(() => {
    if (open) {
      setReason(initialReason);
    }
  }, [open, initialReason]);

  if (!open) return null;

  const trimmedLength = reason.trim().length;
  const canSubmit = trimmedLength >= 20 && !submitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;
    await onSubmit(reason.trim());
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-reason-modal-title"
      >
        {/* Header */}
        <div className="mb-4">
          <h2 id="session-reason-modal-title" className="text-lg font-semibold text-gray-900">{title}</h2>
          {description && (
            <p className="mt-1 text-sm text-gray-500">{description}</p>
          )}
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Reason textarea */}
          <div>
            <label
              htmlFor="session-reason"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Reason
            </label>
            <textarea
              id="session-reason"
              rows={4}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={500}
              placeholder="Describe the reason (at least 20 characters)…"
              className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <p className="mt-1 text-xs text-gray-400">
              {trimmedLength} / 20 minimum
            </p>
          </div>

          {/* Backend error */}
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
              {error}
            </p>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!canSubmit}
              className={`flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium text-white focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${SUBMIT_VARIANT_CLASSES[submitVariant]}`}
            >
              {submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
