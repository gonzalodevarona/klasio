"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

interface HourAdjustmentFormProps {
  membershipId: string;
  onSubmit: (delta: number, reason: string) => Promise<void>;
  onCancel: () => void;
}

export default function HourAdjustmentForm({
  membershipId: _membershipId,
  onSubmit,
  onCancel,
}: HourAdjustmentFormProps) {
  const t = useTranslations("memberships");
  const tCommon = useTranslations("common");

  const [delta, setDelta] = useState<string>("");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const deltaNum = parseInt(delta, 10);
  const isValid =
    delta !== "" &&
    !isNaN(deltaNum) &&
    deltaNum !== 0 &&
    reason.trim().length >= 5 &&
    reason.trim().length <= 500;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValid) return;
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit(deltaNum, reason.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message : tCommon("unexpectedError"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white shadow-xl p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t("adjustTitle")}</h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("adjustDeltaLabel")}
            </label>
            <input
              type="number"
              value={delta}
              onChange={(e) => setDelta(e.target.value)}
              placeholder={t("adjustDeltaPlaceholder")}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
            {delta !== "" && deltaNum === 0 && (
              <p className="mt-1 text-xs text-red-600">{t("adjustZeroError")}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("adjustReasonLabel")} <span className="text-gray-400">{t("adjustReasonHint")}</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder={t("adjustReasonPlaceholder")}
              required
            />
            <p className="mt-1 text-xs text-gray-400">{reason.trim().length} / 500</p>
            {reason.trim().length > 0 && reason.trim().length < 5 && (
              <p className="text-xs text-red-600">{t("adjustReasonMinError")}</p>
            )}
          </div>

          {error && (
            <div className="rounded-md bg-red-50 p-3 text-sm text-red-700 border border-red-200">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onCancel}
              disabled={submitting}
              className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              {tCommon("cancel")}
            </button>
            <button
              type="submit"
              disabled={!isValid || submitting}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? t("adjustSavingBtn") : t("adjustSaveBtn")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
