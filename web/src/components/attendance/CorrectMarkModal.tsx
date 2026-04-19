"use client";

import { useState } from "react";
import { X, Loader2 } from "lucide-react";
import { RosterRegistrantView } from "@/lib/types/attendance";
import { useCorrectAttendance } from "@/hooks/useCorrectAttendance";
import RegistrationStatusBadge from "./RegistrationStatusBadge";

interface CorrectMarkModalProps {
  classId: string;
  sessionDate: string;
  registration: RosterRegistrantView;
  onClose: () => void;
  onCorrected: () => void;
}

export default function CorrectMarkModal({
  classId,
  sessionDate,
  registration,
  onClose,
  onCorrected,
}: CorrectMarkModalProps) {
  const { correctMark, loading } = useCorrectAttendance();

  const [newMark, setNewMark] = useState<"PRESENT" | "ABSENT">(
    registration.status === "ABSENT" ? "PRESENT" : "ABSENT"
  );
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (reason.trim().length < 5) {
      setError("Reason must be at least 5 characters.");
      return;
    }
    setError(null);
    try {
      await correctMark(classId, sessionDate, registration.registrationId, {
        newMark,
        reason: reason.trim(),
      });
      onCorrected();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to correct mark");
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Correct Attendance Mark</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:text-gray-600 focus:outline-none"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Student info */}
        <div className="bg-gray-50 rounded-lg px-4 py-3 mb-4 text-sm">
          <p className="font-medium text-gray-900">{registration.studentName}</p>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-gray-500">Current status:</span>
            <RegistrationStatusBadge status={registration.status} />
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* New mark selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Correct to
            </label>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setNewMark("PRESENT")}
                className={`flex-1 rounded-md border px-3 py-2 text-sm font-medium transition-colors ${
                  newMark === "PRESENT"
                    ? "bg-blue-600 text-white border-blue-600"
                    : "bg-white text-blue-600 border-blue-300 hover:bg-blue-50"
                }`}
              >
                Present
              </button>
              <button
                type="button"
                onClick={() => setNewMark("ABSENT")}
                className={`flex-1 rounded-md border px-3 py-2 text-sm font-medium transition-colors ${
                  newMark === "ABSENT"
                    ? "bg-red-600 text-white border-red-600"
                    : "bg-white text-red-600 border-red-300 hover:bg-red-50"
                }`}
              >
                Absent
              </button>
            </div>
          </div>

          {/* Reason */}
          <div>
            <label
              htmlFor="correction-reason"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Reason <span className="text-gray-400 font-normal">(5–500 characters)</span>
            </label>
            <textarea
              id="correction-reason"
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              maxLength={500}
              placeholder="Explain why this mark is being corrected…"
              className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <p className="mt-1 text-xs text-gray-400 text-right">{reason.length}/500</p>
          </div>

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
              className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || reason.trim().length < 5}
              className="flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" />}
              Save Correction
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
