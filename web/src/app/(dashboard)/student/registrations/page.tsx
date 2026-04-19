"use client";

import { useState } from "react";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import RegistrationStatusBadge from "@/components/attendance/RegistrationStatusBadge";
import { Registration } from "@/lib/types/attendance";
import { AttendanceTimeConstants, formatSessionDate } from "@/lib/attendanceConstants";

const LEVEL_COLORS: Record<string, string> = {
  BEGINNER:     "bg-blue-100 text-blue-700",
  INTERMEDIATE: "bg-yellow-100 text-yellow-700",
  ADVANCED:     "bg-red-100 text-red-700",
};

function isCancellable(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  const cutoff = new Date(sessionStart.getTime() - AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES * 60 * 1000);
  return new Date() < cutoff;
}

function isWithinWarningZone(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  return new Date() >= sessionStart;
}

const STATUS_OPTIONS = [
  { value: "REGISTERED",           label: "Registered" },
  { value: "CANCELLED_BY_STUDENT", label: "Cancelled" },
  { value: "CANCELLED_BY_SYSTEM",  label: "Schedule Changed" },
] as const;

type StatusFilter = typeof STATUS_OPTIONS[number]["value"];

export default function StudentRegistrationsPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("REGISTERED");

  const { registrations, loading, error, refetch } = useMyRegistrations({ status: statusFilter });
  const { cancel, loading: cancelling, error: cancelError, clearError } = useCancelRegistration();

  const [confirmTarget, setConfirmTarget] = useState<Registration | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleCancelClick = (reg: Registration) => {
    clearError();
    setSuccessMessage(null);
    setConfirmTarget(reg);
  };

  const handleConfirmCancel = async () => {
    if (!confirmTarget) return;
    try {
      await cancel(confirmTarget.id);
      setSuccessMessage(
        `Registration for "${confirmTarget.className}" on ${formatSessionDate(confirmTarget.sessionDate)} cancelled.`
      );
      setConfirmTarget(null);
      refetch();
    } catch {
      // error is surfaced via cancelError from the hook
    }
  };

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">My Registrations</h1>
        <p className="mt-1 text-sm text-gray-500">
          Your upcoming and past class session registrations.
        </p>
      </div>

      <div className="mb-4 flex gap-2">
        {STATUS_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => setStatusFilter(opt.value)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
              statusFilter === opt.value
                ? "bg-indigo-600 text-white border-indigo-600"
                : "bg-white text-gray-600 border-gray-300 hover:border-indigo-400 hover:text-indigo-600"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {successMessage && (
        <div className="mb-4 rounded-md bg-green-50 border border-green-200 p-4 text-sm text-green-700">
          {successMessage}
        </div>
      )}

      {cancelError && (
        <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {cancelError}
        </div>
      )}

      {loading && (
        <p className="py-8 text-center text-sm text-gray-500">Loading…</p>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {statusFilter === "CANCELLED_BY_SYSTEM" && (
        <div className="mb-4 rounded-md bg-amber-50 border border-amber-200 p-4 text-sm text-amber-800">
          These registrations were cancelled because the class schedule was changed.
          Go to <strong>My Classes</strong> to register for the new sessions.
        </div>
      )}

      {!loading && !error && registrations.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-400">
          {statusFilter === "REGISTERED"
            ? "You have no upcoming registrations."
            : statusFilter === "CANCELLED_BY_SYSTEM"
            ? "No registrations were cancelled by a schedule change."
            : "You have no cancelled registrations."}
        </p>
      )}

      {registrations.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Time
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Class
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Hours
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {registrations.map((r) => {
                const cancellable = isCancellable(r);
                const inPast = isWithinWarningZone(r);
                return (
                  <tr key={r.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {formatSessionDate(r.sessionDate)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500 whitespace-nowrap">
                      {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {r.className}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                          LEVEL_COLORS[r.level] ?? "bg-gray-100 text-gray-600"
                        }`}
                      >
                        {r.level}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {r.intendedHours}h
                    </td>
                    <td className="px-4 py-3">
                      <RegistrationStatusBadge status={r.status} />
                    </td>
                    <td className="px-4 py-3">
                      {r.status === "REGISTERED" && (
                        cancellable ? (
                          <button
                            onClick={() => handleCancelClick(r)}
                            className="text-xs font-medium text-red-600 hover:text-red-800 transition-colors"
                          >
                            Cancel
                          </button>
                        ) : (
                          <span
                            title={
                              inPast
                                ? "Session has already started"
                                : `Cancellation window closed (${AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES} min before class)`
                            }
                            className="text-xs text-gray-400 cursor-not-allowed"
                          >
                            Cancel
                          </span>
                        )
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Confirmation modal */}
      {confirmTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-sm w-full mx-4">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">
              Cancel registration?
            </h2>
            <p className="text-sm text-gray-600 mb-4">
              Cancel your registration for{" "}
              <span className="font-medium">{confirmTarget.className}</span> on{" "}
              <span className="font-medium">
                {formatSessionDate(confirmTarget.sessionDate)}
              </span>{" "}
              at {confirmTarget.sessionStartTime.slice(0, 5)}? Your spot will be
              released.
            </p>

            {cancelError && (
              <p className="mb-3 text-sm text-red-600">{cancelError}</p>
            )}

            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setConfirmTarget(null); clearError(); }}
                disabled={cancelling}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
              >
                Keep
              </button>
              <button
                onClick={handleConfirmCancel}
                disabled={cancelling}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md hover:bg-red-700 disabled:opacity-50"
              >
                {cancelling ? "Cancelling…" : "Yes, cancel"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
