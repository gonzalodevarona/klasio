"use client";

import { useState } from "react";
import { useTranslations, useLocale } from "next-intl";
import { useMyRegistrations } from "@/hooks/useMyRegistrations";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import { useAttendanceStats } from "@/hooks/useAttendanceStats";
import AttendanceStatsBar from "@/components/attendance/AttendanceStatsBar";
import RegistrationStatusBadge from "@/components/attendance/RegistrationStatusBadge";
import { Button } from "@/components/ui";
import { Registration } from "@/lib/types/attendance";
import { ClassLevel } from "@/lib/types/programClass";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
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
  labelKey: string;
  status: string | undefined;
}

const FILTER_DEFS: FilterOption[] = [
  { key: "ALL",                  labelKey: "filterAll",        status: undefined },
  { key: "REGISTERED",           labelKey: "filterRegistered", status: "REGISTERED" },
  { key: "PRESENT",              labelKey: "filterAttended",   status: "PRESENT" },
  { key: "CANCELLED_BY_STUDENT", labelKey: "filterCancelled",  status: "CANCELLED_BY_STUDENT" },
  { key: "ABSENT",               labelKey: "filterAbsent",     status: "ABSENT" },
];

export default function StudentAttendancePage() {
  const t = useTranslations("studentAttendance");
  const locale = useLocale();
  const [activeFilter, setActiveFilter] = useState<FilterOption>(FILTER_DEFS[0]);

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
        t("successMessage", {
          className: confirmTarget.className,
          date: formatSessionDate(confirmTarget.sessionDate, locale),
        })
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
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          {t("subtitle")}
        </p>
      </div>

      <AttendanceStatsBar stats={stats} loading={statsLoading} />

      <div className="mb-4 flex flex-wrap gap-2">
        {FILTER_DEFS.map((opt) => (
          <button
            key={opt.key}
            onClick={() => setActiveFilter(opt)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
              activeFilter.key === opt.key
                ? "bg-k-dark text-white border-k-dark"
                : "bg-k-surface text-k-subtle border-k-border hover:border-k-subtle hover:text-k-dark"
            }`}
          >
            {t(opt.labelKey as Parameters<typeof t>[0])}
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
        <p className="py-8 text-center text-sm text-k-muted">{t("loading")}</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && registrations.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          {t("emptyFilter")}
        </p>
      )}

      {registrations.length > 0 && (
        <div className="overflow-hidden rounded-k-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colDate")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colTime")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colClass")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colLevel")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colHours")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colStatus")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colActions")}
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
                      {formatSessionDate(r.sessionDate, locale)}
                    </td>
                    <td className="px-4 py-3 text-sm text-k-muted whitespace-nowrap">
                      {r.sessionStartTime.slice(0, 5)} – {r.sessionEndTime.slice(0, 5)}
                    </td>
                    <td className="px-4 py-3 text-sm text-k-dark">
                      {r.className}
                      {r.status === "SESSION_CANCELLED" && r.sessionCancellationReason && (
                        <div className="mt-0.5 text-xs italic text-k-danger-text">
                          {t("reason")} {r.sessionCancellationReason}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <ClassLevelBadge level={r.level as ClassLevel} />
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
                            {t("cancelButton")}
                          </button>
                        ) : (
                          <span
                            title={
                              inPast
                                ? t("sessionStarted")
                                : t("cancelCutoffClosed", { minutes: AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES })
                            }
                            className="text-xs text-k-muted cursor-not-allowed"
                          >
                            {t("cancelButton")}
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

      {confirmTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-k-surface rounded-k-lg shadow-k-modal p-6 max-w-sm w-full mx-4">
            <h2 className="text-lg font-semibold text-k-dark mb-2">
              {t("modalTitle")}
            </h2>
            <p className="text-sm text-k-muted mb-4">
              {t.rich("modalBody", {
                className: confirmTarget.className,
                date: formatSessionDate(confirmTarget.sessionDate, locale),
                time: confirmTarget.sessionStartTime.slice(0, 5),
                b: (chunks) => <span className="font-medium text-k-dark">{chunks}</span>,
              })}
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
                {t("modalKeep")}
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmCancel}
                disabled={cancelling}
              >
                {cancelling ? t("modalConfirming") : t("modalConfirm")}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
