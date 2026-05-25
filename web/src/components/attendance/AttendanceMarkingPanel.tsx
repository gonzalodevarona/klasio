"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { CheckCircle, XCircle, Loader2, AlertTriangle, Clock, Bell } from "lucide-react";
import {
  ClassSessionRoster,
  RosterRegistrantView,
  MarkEntry,
  MarkedRegistration,
} from "@/lib/types/attendance";
import { useMarkAttendance } from "@/hooks/useMarkAttendance";
import RegistrationStatusBadge, { DropInTag } from "./RegistrationStatusBadge";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
import { ClassLevel } from "@/lib/types/programClass";
import CorrectMarkModal from "./CorrectMarkModal";
import { isWithinMarkingWindow, msUntilWindowOpen } from "@/lib/attendanceTimeWindow";
import { AttendanceTimeConstants } from "@/lib/attendanceConstants";

interface AttendanceMarkingPanelProps {
  classId: string;
  session: ClassSessionRoster;
  userRole: string;
  onMarked?: () => void;
}

type LocalMark = "PRESENT" | "ABSENT" | null;


const CAN_MARK_ROLES = ["PROFESSOR", "ADMIN", "SUPERADMIN", "MANAGER"];

export default function AttendanceMarkingPanel({
  classId,
  session,
  userRole,
  onMarked,
}: AttendanceMarkingPanelProps) {
  const t = useTranslations("classes");
  const { markAttendance, loading } = useMarkAttendance();

  // Re-evaluate the window every 30 s so the UI unlocks automatically when the time comes.
  const [withinWindow, setWithinWindow] = useState(() =>
    isWithinMarkingWindow(session.sessionDate, session.startTime, session.endTime)
  );

  // Toast state: shown briefly when the window opens.
  const [showWindowOpenToast, setShowWindowOpenToast] = useState(false);
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    // Tick every 30 s to refresh the window status.
    const intervalId = setInterval(() => {
      setWithinWindow(
        isWithinMarkingWindow(session.sessionDate, session.startTime, session.endTime)
      );
    }, 30_000);

    // Schedule a one-shot timer to fire exactly when the window opens (if it hasn't yet).
    const msUntilOpen = msUntilWindowOpen(
      session.sessionDate,
      session.startTime,
      session.endTime
    );

    let openTimerId: ReturnType<typeof setTimeout> | null = null;
    if (msUntilOpen > 0) {
      openTimerId = setTimeout(() => {
        setWithinWindow(true);
        setShowWindowOpenToast(true);
        // Auto-dismiss the toast after 8 s.
        toastTimerRef.current = setTimeout(() => setShowWindowOpenToast(false), 8_000);
      }, msUntilOpen);
    }

    return () => {
      clearInterval(intervalId);
      if (openTimerId) clearTimeout(openTimerId);
      if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    };
  }, [session.sessionDate, session.startTime, session.endTime]);

  const canMark = CAN_MARK_ROLES.includes(userRole) && withinWindow;
  const canCorrect = ["ADMIN", "SUPERADMIN", "MANAGER"].includes(userRole);

  // Local mark selections (registrationId → mark)
  const [localMarks, setLocalMarks] = useState<Record<string, LocalMark>>({});
  // Results returned from the API after submitting
  const [results, setResults] = useState<MarkedRegistration[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Correction modal state
  const [correctingReg, setCorrectingReg] = useState<RosterRegistrantView | null>(null);

  const setMark = useCallback((regId: string, mark: LocalMark) => {
    setLocalMarks((prev) => ({ ...prev, [regId]: mark }));
  }, []);

  const handleSubmit = useCallback(async () => {
    const entries: MarkEntry[] = Object.entries(localMarks)
      .filter(([, m]) => m !== null)
      .map(([registrationId, mark]) => ({
        registrationId,
        mark: mark as "PRESENT" | "ABSENT",
      }));

    if (entries.length === 0) return;

    setSubmitError(null);
    try {
      const response = await markAttendance(classId, session.sessionDate, {
        startTime: session.startTime,
        marks: entries,
      });
      setResults(response.results);
      onMarked?.();
    } catch (err: unknown) {
      setSubmitError(err instanceof Error ? err.message : t("markingSubmitError"));
    }
  }, [classId, session, localMarks, markAttendance, onMarked]);

  // Build a map of results by registrationId for quick lookup
  const resultMap = Object.fromEntries(results.map((r) => [r.registrationId, r]));

  const allMarked = session.registrants.every(
    (r) => localMarks[r.registrationId] !== undefined
  );

  return (
    <div className="mt-2">
      {/* Window-open toast — fires like a birthday notification */}
      {showWindowOpenToast && (
        <div className="flex items-center gap-2 text-sm text-green-800 bg-green-50 border border-green-300 rounded-md px-3 py-2 mb-3 shadow-sm">
          <Bell className="w-4 h-4 shrink-0 text-green-600" />
          <span className="font-medium">
            {t("markingWindowOpenToast")}
          </span>
          <button
            type="button"
            onClick={() => setShowWindowOpenToast(false)}
            className="ml-auto text-green-700 hover:text-green-900 text-xs underline"
          >
            {t("markingWindowOpenDismiss")}
          </button>
        </div>
      )}

      {/* Locked banner — shown when the user has the right role but the window is closed */}
      {CAN_MARK_ROLES.includes(userRole) && !withinWindow && (
        <div className="flex items-center gap-2 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-md px-3 py-2 mb-3">
          <Clock className="w-4 h-4 shrink-0" />
          <span>
            {t("markingWindowLockedBanner", {
              before: AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE,
              after: AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER,
            })}
          </span>
        </div>
      )}

      <table className="min-w-full text-sm">
        <thead>
          <tr className="text-xs text-gray-500 uppercase tracking-wider border-b border-gray-100">
            <th className="px-4 py-2 text-left font-medium">{t("rosterColStudent")}</th>
            <th className="px-4 py-2 text-left font-medium">{t("rosterColLevel")}</th>
            <th className="px-4 py-2 text-left font-medium">{t("rosterColHours")}</th>
            <th className="px-4 py-2 text-left font-medium">{t("rosterColStatus")}</th>
            {canMark && (
              <th className="px-4 py-2 text-left font-medium">{t("markingColMark")}</th>
            )}
            {canCorrect && (
              <th className="px-4 py-2 text-left font-medium">{t("markingColCorrect")}</th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {session.registrants.map((r) => {
            const apiResult = resultMap[r.registrationId];
            const currentMark = localMarks[r.registrationId];
            const isAlreadyMarked =
              r.status !== "REGISTERED" &&
              r.status !== "CANCELLED_BY_STUDENT" &&
              r.status !== "CANCELLED_BY_SYSTEM";
            const isDropIn = r.dropInAttendeeId != null;
            const displayName = isDropIn ? (r.dropInAttendeeName ?? "") : r.studentName;

            return (
              <tr key={r.registrationId} className="hover:bg-gray-50">
                <td className="px-4 py-2 text-gray-900 font-medium">
                  {displayName}
                  {isDropIn && <DropInTag />}
                </td>
                <td className="px-4 py-2">
                  {!isDropIn && <ClassLevelBadge level={r.level as ClassLevel} />}
                </td>
                <td className="px-4 py-2 text-gray-600">
                  {!isDropIn && `${r.intendedHours}h`}
                </td>
                <td className="px-4 py-2">
                  {apiResult ? (
                    <div className="flex items-center gap-1.5">
                      <RegistrationStatusBadge status={apiResult.status} />
                      {apiResult.noHoursWarning && (
                        <span
                          title={t("markingNoHoursWarning")}
                          className="text-amber-500"
                        >
                          <AlertTriangle className="w-3.5 h-3.5" />
                        </span>
                      )}
                    </div>
                  ) : (
                    <RegistrationStatusBadge status={r.status} />
                  )}
                </td>
                {/* Drop-in rows have no mark/correct actions */}
                {canMark && (
                  <td className="px-4 py-2">
                    {!isDropIn && (
                      isAlreadyMarked ? (
                        <span className="text-xs text-gray-400 italic">{t("markingAlreadyMarked")}</span>
                      ) : (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() =>
                              setMark(
                                r.registrationId,
                                currentMark === "PRESENT" ? null : "PRESENT"
                              )
                            }
                            disabled={loading}
                            className={`flex items-center gap-1 rounded px-2 py-1 text-xs font-medium border transition-colors ${
                              currentMark === "PRESENT"
                                ? "bg-blue-600 text-white border-blue-600"
                                : "bg-white text-blue-600 border-blue-300 hover:bg-blue-50"
                            }`}
                          >
                            <CheckCircle className="w-3.5 h-3.5" />
                            {t("markingPresent")}
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              setMark(
                                r.registrationId,
                                currentMark === "ABSENT" ? null : "ABSENT"
                              )
                            }
                            disabled={loading}
                            className={`flex items-center gap-1 rounded px-2 py-1 text-xs font-medium border transition-colors ${
                              currentMark === "ABSENT"
                                ? "bg-red-600 text-white border-red-600"
                                : "bg-white text-red-600 border-red-300 hover:bg-red-50"
                            }`}
                          >
                            <XCircle className="w-3.5 h-3.5" />
                            {t("markingAbsent")}
                          </button>
                        </div>
                      )
                    )}
                  </td>
                )}
                {canCorrect && (
                  <td className="px-4 py-2">
                    {!isDropIn && isAlreadyMarked && (
                      <button
                        type="button"
                        onClick={() => setCorrectingReg(r)}
                        className="text-xs text-indigo-600 hover:underline"
                      >
                        {t("markingColCorrect")}
                      </button>
                    )}
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>

      {canMark && (
        <div className="mt-3 flex items-center gap-3 px-4">
          <button
            type="button"
            onClick={handleSubmit}
            disabled={loading || !allMarked}
            className="flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading && <Loader2 className="w-4 h-4 animate-spin" />}
            {t("markingSubmitButton")}
          </button>
          {submitError && (
            <span className="text-sm text-red-600">{submitError}</span>
          )}
        </div>
      )}

      {correctingReg && (
        <CorrectMarkModal
          classId={classId}
          sessionDate={session.sessionDate}
          registration={correctingReg}
          onClose={() => setCorrectingReg(null)}
          onCorrected={() => {
            setCorrectingReg(null);
            onMarked?.();
          }}
        />
      )}
    </div>
  );
}
