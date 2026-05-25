"use client";

import { useState, useMemo } from "react";
import { ChevronLeft, ChevronRight, Users } from "lucide-react";
import { useTranslations, useLocale } from "next-intl";
import { useClassSessionRoster } from "@/hooks/useClassSessionRoster";
import RegistrationStatusBadge, { DropInTag } from "./RegistrationStatusBadge";
import AttendanceMarkingPanel from "./AttendanceMarkingPanel";
import SessionStatusBadge from "./SessionStatusBadge";
import SessionActionsPanel from "./SessionActionsPanel";
import { WalkInButton } from "./WalkInButton";
import { DropInButton } from "./DropInButton";
import { RegistrarBadge } from "./RegistrarBadge";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
import { ClassLevel } from "@/lib/types/programClass";

interface ClassRosterPanelProps {
  classId: string;
  /** Level of the class (BEGINNER, INTERMEDIATE, ADVANCED, OPEN). Used to decide whether to show the level filter in the walk-in modal. */
  classLevel?: string;
  /** When provided, enables interactive marking for the given role (PROFESSOR, MANAGER, ADMIN, SUPERADMIN). */
  userRole?: string;
  /** Program the class belongs to — used for manager scope check. */
  programId?: string;
  /** Program IDs managed by the current user (MANAGER role). */
  managedProgramIds?: string[];
  /** Class IDs assigned to the current user (PROFESSOR role). */
  professorClassIds?: string[];
  /** Drop-in price for this class's program. When non-null, the DropInButton is shown. */
  programDropInPrice?: string | null;
}

/** Returns the Monday of the week that contains `date`. */
function weekStart(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay(); // 0 = Sun
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, n: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

function toISO(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function formatDisplayDate(isoDate: string, locale: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(locale, {
    weekday: "short",
    month: "short",
    day: "numeric",
  });
}

function formatTime(timeStr: string, locale: string): string {
  const [h, m] = timeStr.split(":").map(Number);
  return new Intl.DateTimeFormat(locale, {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(2000, 0, 1, h, m));
}

function computeDurationMinutes(start: string, end: string): number {
  const [sh, sm] = start.split(":").map(Number);
  const [eh, em] = end.split(":").map(Number);
  return (eh * 60 + em) - (sh * 60 + sm);
}


export default function ClassRosterPanel({
  classId,
  classLevel = "OPEN",
  userRole,
  programId,
  managedProgramIds,
  professorClassIds,
  programDropInPrice,
}: ClassRosterPanelProps) {
  const t = useTranslations("classes");
  const locale = useLocale();

  const [monday, setMonday] = useState<Date>(() => weekStart(new Date()));

  const from = useMemo(() => toISO(monday), [monday]);
  const to   = useMemo(() => toISO(addDays(monday, 6)), [monday]);

  const { sessions, loading, error, refetch } = useClassSessionRoster(classId, from, to);

  const weekLabel = useMemo(() => {
    const sun = addDays(monday, 6);
    return `${monday.toLocaleDateString(locale, { month: "short", day: "numeric" })} – ${sun.toLocaleDateString(locale, { month: "short", day: "numeric", year: "numeric" })}`;
  }, [monday, locale]);

  const now = new Date();

  return (
    <div className="bg-blue-50 border-t border-blue-100 px-6 py-4">
      {/* Week navigator */}
      <div className="flex items-center gap-3 mb-4">
        <span className="text-sm font-medium text-blue-800 flex items-center gap-1.5">
          <Users className="w-4 h-4" />
          {t("rosterTitle")}
        </span>
        <div className="flex items-center gap-1 ml-auto">
          <button
            type="button"
            onClick={() => setMonday((d) => addDays(d, -7))}
            className="rounded p-1 text-blue-600 hover:bg-blue-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label={t("rosterPrevWeekAriaLabel")}
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="text-sm font-medium text-blue-800 min-w-[220px] text-center">
            {weekLabel}
          </span>
          <button
            type="button"
            onClick={() => setMonday((d) => addDays(d, 7))}
            className="rounded p-1 text-blue-600 hover:bg-blue-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label={t("rosterNextWeekAriaLabel")}
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {loading && (
        <p className="text-sm text-blue-600 py-2">{t("rosterLoading")}</p>
      )}

      {error && (
        <p className="text-sm text-red-600 py-2">{error}</p>
      )}

      {!loading && !error && sessions.length === 0 && (
        <p className="text-sm text-blue-600 py-2">{t("rosterNoSessions")}</p>
      )}

      {!loading && !error && sessions.length > 0 && (
        <div className="space-y-4">
          {sessions.map((session) => {
            const sessionStart = new Date(`${session.sessionDate}T${session.startTime}`);
            const isFuture = sessionStart.getTime() > now.getTime(); // now hoisted above map — computed once per render
            const role = (userRole ?? "").toUpperCase();
            const canManage =
              role === "ADMIN" || role === "SUPERADMIN" ||
              (role === "MANAGER" && !!programId && (managedProgramIds ?? []).includes(programId)) ||
              (role === "PROFESSOR" && (professorClassIds ?? []).includes(classId));

            const sessionStatus = session.status ?? "SCHEDULED";
            const sessionReason = session.alertReason ?? session.cancellationReason ?? null;

            return (
            <div
              key={`${session.sessionDate}-${session.startTime}`}
              className="bg-white rounded-lg border border-blue-200 overflow-hidden"
            >
              {/* Session header */}
              <div className="flex items-center flex-wrap px-4 py-2 bg-blue-100 border-b border-blue-200 gap-4">
                <span className="text-sm font-semibold text-blue-900">
                  {formatDisplayDate(session.sessionDate, locale)}
                </span>
                <span className="text-sm text-blue-700">
                  {formatTime(session.startTime, locale)} – {formatTime(session.endTime, locale)}
                </span>
                {session.location && (
                  <span className="text-sm text-blue-700">· {session.location}</span>
                )}
                <SessionStatusBadge status={sessionStatus} reason={sessionReason} />
                <SessionActionsPanel
                  classId={classId}
                  sessionDate={session.sessionDate}
                  status={sessionStatus}
                  alertReason={session.alertReason}
                  isFuture={isFuture}
                  canManage={canManage}
                  onActionCompleted={refetch}
                />
                {canManage && sessionStatus !== "CANCELLED" && (
                  <WalkInButton
                    classId={classId}
                    sessionDate={session.sessionDate}
                    startTime={session.startTime}
                    endTime={session.endTime}
                    durationMinutes={computeDurationMinutes(session.startTime, session.endTime)}
                    classLevel={classLevel}
                    onRegistered={refetch}
                  />
                )}
                {canManage && sessionStatus !== "CANCELLED" && programDropInPrice != null && (
                  <DropInButton
                    classId={classId}
                    sessionDate={session.sessionDate}
                    startTime={session.startTime}
                    programDropInPrice={programDropInPrice}
                    onRegistered={refetch}
                  />
                )}
                <span className="text-xs font-medium text-blue-600 ml-auto">
                  {t("rosterRegistrantCount", { count: session.registrantCount })}
                </span>
              </div>

              {/* Registrants — interactive marking panel or read-only table */}
              {session.registrants.length === 0 ? (
                <p className="text-sm text-gray-400 italic px-4 py-3">{t("rosterNoRegistrants")}</p>
              ) : userRole ? (
                <div className="px-4 py-3">
                  <AttendanceMarkingPanel
                    classId={classId}
                    session={session}
                    userRole={userRole}
                    onMarked={refetch}
                  />
                </div>
              ) : (
                <table className="min-w-full text-sm">
                  <thead>
                    <tr className="text-xs text-gray-500 uppercase tracking-wider border-b border-gray-100">
                      <th className="px-4 py-2 text-left font-medium">{t("rosterColStudent")}</th>
                      <th className="px-4 py-2 text-left font-medium">{t("rosterColLevel")}</th>
                      <th className="px-4 py-2 text-left font-medium">{t("rosterColHours")}</th>
                      <th className="px-4 py-2 text-left font-medium">{t("rosterColStatus")}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {session.registrants.map((r) => {
                      const isDropIn = r.dropInAttendeeId != null;
                      const displayName = isDropIn ? (r.dropInAttendeeName ?? "") : r.studentName;
                      return (
                        <tr key={r.registrationId} className="hover:bg-gray-50">
                          <td className="px-4 py-2 text-gray-900 font-medium">
                            {displayName}
                            {isDropIn && <DropInTag />}
                            {!isDropIn && r.createdBy && ["ADMIN", "SUPERADMIN", "MANAGER"].includes((userRole ?? "").toUpperCase()) && (
                              <RegistrarBadge createdBy={r.createdBy} />
                            )}
                          </td>
                          <td className="px-4 py-2">
                            {!isDropIn && <ClassLevelBadge level={r.level as ClassLevel} />}
                          </td>
                          <td className="px-4 py-2 text-gray-600">
                            {!isDropIn && `${r.intendedHours}h`}
                          </td>
                          <td className="px-4 py-2">
                            <RegistrationStatusBadge status={r.status} />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          );
          })}
        </div>
      )}
    </div>
  );
}
