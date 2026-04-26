"use client";

import { useState } from "react";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import { useAttendanceStats } from "@/hooks/useAttendanceStats";
import AttendanceStatsBar from "@/components/attendance/AttendanceStatsBar";
import RegistrationStatusBadge from "@/components/attendance/RegistrationStatusBadge";
import { Badge, Button } from "@/components/ui";
import { Registration } from "@/lib/types/attendance";
import { AttendanceTimeConstants, formatSessionDate } from "@/lib/attendanceConstants";

function isCancellable(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  const cutoffMs = sessionStart.getTime() - AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES * 60 * 1000;
  return Date.now() < cutoffMs;
}

function isWithinWarningZone(reg: Registration): boolean {
  if (reg.status !== "REGISTERED") return false;
  const sessionStart = new Date(`${reg.sessionDate}T${reg.sessionStartTime}`);
  return new Date() >= sessionStart;
}

type FilterKey = "ALL" | "REGISTERED" | "PRESENT" | "CANCELLED_BY_STUDENT" | "ABSENT";

interface FilterOption {
  key: FilterKey;
  label: string;
  status: string | undefined;
}

const FILTER_OPTIONS: FilterOption[] = [
  { key: "ALL",                label: "All",       status: undefined },
  { key: "REGISTERED",         label: "Registered", status: "REGISTERED" },
  { key: "PRESENT",            label: "Attended",   status: "PRESENT" },
  { key: "CANCELLED_BY_STUDENT", label: "Cancelled", status: "CANCELLED_BY_STUDENT" },
  { key: "ABSENT",             label: "Absent",     status: "ABSENT" },
];

export default function StudentAttendancePage() {
  const [activeFilter, setActiveFilter] = useState<FilterOption>(FILTER_OPTIONS[0]);

  const { registrations, loading, error, refetch } = useMyRegistrations(
    activeFilter.status ? { status: activeFilter.status } : {}
  );
  const { cancel, loading: cancelling, error: cancelError, clearError } = useCancelRegistration();
  const { stats, loading: statsLoading } = useAttendanceStats();

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
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">Attendance</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          Your complete class session history.
        </p>
      </div>

      <AttendanceStatsBar stats={stats} loading={statsLoading} />

      <div className="mb-4 flex flex-wrap gap-2">
        {FILTER_OPTIONS.map((opt) => (
          <button
            key={opt.key}
            onClick={() => setActiveFilter(opt)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
              activeFilter.key === opt.key
                ? "bg-k-dark text-white border-k-dark"
                : "bg-k-surface text-k-subtle border-k-border hover:border-k-subtle hover:text-k-dark"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {successMessage && (
        <div className="mb-4 rounded-k-sm bg-k-volt/10 border border-k-volt/30 p-4 text-sm text-k-volt-text">
          {successMessage}
        </div>
      )}

      {cancelError && (
        <div className="mb-4 rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {cancelError}
        </div>
      )}

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">Loading…</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && registrations.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          No records found for the selected filter.
        </p>
      )}

      {registrations.length > 0 && (
        <div className="overflow-hidden rounded-k-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Time
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Class
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Hours
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {registrations.map((r) => {
                const cancellable = isCancellable(r);
                const inPast = isWithinWarningZone(r);
                return (
                  <tr key={r.id} className="hover:bg-k-bg">
                    <td className="px-4 py-3 text-sm text-k-dark">
                      {formatSessionDate(r.sessionDate)}
                    </td>
                    <td className="px-4 py-3 text-sm text-k-muted whitespace-nowrap">
                      {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                    </td>
                    <td className="px-4 py-3 text-sm text-k-dark">
                      {r.className}
                      {r.status === "SESSION_CANCELLED" && r.sessionCancellationReason && (
                        <div className="mt-0.5 text-xs italic text-k-danger-text">
                          Reason: {r.sessionCancellationReason}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <Badge
                        variant={
                          r.level === "BEGINNER"
                            ? "beginner"
                            : r.level === "INTERMEDIATE"
                            ? "intermediate"
                            : r.level === "ADVANCED"
                            ? "advanced"
                            : "info"
                        }
                        label={r.level}
                        small
                      />
                    </td>
                    <td className="px-4 py-3 text-sm text-k-muted">
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
                            className="text-xs font-medium text-k-danger-text hover:text-k-dark transition-colors"
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
                            className="text-xs text-k-muted cursor-not-allowed"
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
          <div className="bg-k-surface rounded-k-lg shadow-k-modal p-6 max-w-sm w-full mx-4">
            <h2 className="text-lg font-semibold text-k-dark mb-2">
              Cancel registration?
            </h2>
            <p className="text-sm text-k-muted mb-4">
              Cancel your registration for{" "}
              <span className="font-medium text-k-dark">{confirmTarget.className}</span> on{" "}
              <span className="font-medium text-k-dark">
                {formatSessionDate(confirmTarget.sessionDate)}
              </span>{" "}
              at {confirmTarget.sessionStartTime.slice(0, 5)}? Your spot will be released.
            </p>

            {cancelError && (
              <p className="mb-3 text-sm text-k-danger-text">{cancelError}</p>
            )}

            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => { setConfirmTarget(null); clearError(); }}
                disabled={cancelling}
              >
                Keep
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmCancel}
                disabled={cancelling}
              >
                {cancelling ? "Cancelling…" : "Yes, cancel"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
