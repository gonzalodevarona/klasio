"use client";

import React, { useState, useEffect } from "react";
import { AlertTriangle, ChevronDown, ChevronRight } from "lucide-react";
import { useMyClasses } from "@/hooks/useMyClasses";
import { useAvailableSessions } from "@/hooks/useAvailableSessions";
import { useRegisterForSession } from "@/hooks/useRegisterForSession";
import SessionCapacityBar from "@/components/attendance/SessionCapacityBar";
import { AvailableSession } from "@/lib/types/attendance";
import { ProgramClassSummary } from "@/lib/types/programClass";
import { AttendanceTimeConstants, todayInTenantZone, addDays, formatSessionDate } from "@/lib/attendanceConstants";

const LEVEL_COLORS: Record<string, string> = {
  BEGINNER:     "bg-blue-100 text-blue-700",
  INTERMEDIATE: "bg-yellow-100 text-yellow-700",
  ADVANCED:     "bg-red-100 text-red-700",
};

/** Parses "HH:mm:ss" strings and returns floor(durationMinutes / 60), minimum 1. */
function computeIntendedHours(start: string, end: string): number {
  const [sh, sm] = start.split(":").map(Number);
  const [eh, em] = end.split(":").map(Number);
  const durationMinutes = (eh * 60 + em) - (sh * 60 + sm);
  return Math.max(1, Math.floor(durationMinutes / 60));
}

// ── ClassSessionsPanel ──────────────────────────────────────────────────────

interface ClassSessionsPanelProps {
  programId: string;
  classId: string;
}

function ClassSessionsPanel({ programId, classId }: ClassSessionsPanelProps) {
  const today = todayInTenantZone();
  const twoWeeksOut = addDays(today, 14);

  const { sessions, loading, error, refetch } = useAvailableSessions(programId, {
    from: today,
    to: twoWeeksOut,
  });
  const { register } = useRegisterForSession();

  // Local copy of sessions — enables optimistic removal without touching the hook state.
  const [localSessions, setLocalSessions] = useState<AvailableSession[]>([]);
  const [registerError, setRegisterError] = useState<string | null>(null);

  // Keep local copy in sync whenever the hook fetches fresh data.
  useEffect(() => {
    setLocalSessions(sessions);
  }, [sessions]);

  const classSessions = localSessions.filter(
    (s) => s.classId === classId && s.status !== "CANCELLED"
  );

  async function handleRegister(s: AvailableSession) {
    setRegisterError(null);

    // Optimistic remove — row disappears before the backend responds.
    setLocalSessions((prev) =>
      prev.filter((x) => !(x.classId === s.classId && x.sessionDate === s.sessionDate))
    );

    try {
      const hours = computeIntendedHours(s.startTime, s.endTime);
      await register(classId, s.sessionDate, hours);
      // Silent background revalidation to stay in sync with the server.
      refetch();
    } catch (err) {
      // Rollback — put the session back in date order.
      setLocalSessions((prev) => {
        const exists = prev.some(
          (x) => x.classId === s.classId && x.sessionDate === s.sessionDate
        );
        if (exists) return prev;
        return [...prev, s].sort((a, b) => a.sessionDate.localeCompare(b.sessionDate));
      });
      setRegisterError(
        err instanceof Error ? err.message : "Failed to register. Please try again."
      );
    }
  }

  return (
    <div className="bg-gray-50 px-4 py-3 border-t border-gray-100">
      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
        Upcoming Sessions — next 2 weeks
      </p>

      {loading && (
        <p className="text-sm text-gray-400 py-2">Loading sessions…</p>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-xs text-red-700 mb-2">
          {error}
        </div>
      )}

      {registerError && (
        <div className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-xs text-red-700 mb-2">
          {registerError}
        </div>
      )}

      {!loading && !error && classSessions.length === 0 && (
        <p className="text-sm text-gray-400 py-1">
          No upcoming sessions in the next 2 weeks.
        </p>
      )}

      {classSessions.length > 0 && (
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-xs text-gray-500">
              <th className="py-1 pr-4 text-left font-medium">Date</th>
              <th className="py-1 pr-4 text-left font-medium">Time</th>
              <th className="py-1 pr-4 text-left font-medium">Capacity</th>
              <th className="py-1 text-left font-medium"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {classSessions.map((s) => {
              const isFull = s.currentCapacity >= s.maxStudents;
              const registrationOpen = s.registrationOpen !== false; // treat missing field as open
              return (
                <tr key={`${s.classId}-${s.sessionDate}`}>
                  <td className="py-2 pr-4 text-gray-900">
                    <div className="flex items-center gap-1.5">
                      <span>{formatSessionDate(s.sessionDate)}</span>
                      {s.status === "ALERTED" && (
                        <span
                          title={s.alertReason ?? "Alert issued for this session"}
                          className="inline-flex text-amber-600"
                        >
                          <AlertTriangle className="w-4 h-4" />
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="py-2 pr-4 text-gray-500 whitespace-nowrap">
                    {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                  </td>
                  <td className="py-2 pr-4">
                    <SessionCapacityBar
                      current={s.currentCapacity}
                      max={s.maxStudents}
                    />
                  </td>
                  <td className="py-2">
                    <button
                      onClick={() => handleRegister(s)}
                      disabled={isFull || !registrationOpen}
                      title={
                        !registrationOpen
                          ? `Registration closes ${AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES} min before class`
                          : isFull
                          ? "This session is full"
                          : undefined
                      }
                      className={[
                        "rounded px-3 py-1 text-xs font-medium transition-colors",
                        isFull || !registrationOpen
                          ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                          : "bg-indigo-600 text-white hover:bg-indigo-700",
                      ].join(" ")}
                    >
                      {isFull ? "Full" : !registrationOpen ? "Closed" : "Register"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}

// ── Page ────────────────────────────────────────────────────────────────────

export default function StudentClassesPage() {
  const { classes, loading, error } = useMyClasses();
  const [expandedClassId, setExpandedClassId] = useState<string | null>(null);

  function toggleExpand(c: ProgramClassSummary) {
    setExpandedClassId((prev) => (prev === c.id ? null : c.id));
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">My Classes</h1>
        <p className="mt-1 text-sm text-gray-500">
          Classes available to you based on your enrollment level.
        </p>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-gray-500">Loading…</p>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {!loading && !error && classes.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-400">
          No classes found. Make sure you have an active enrollment.
        </p>
      )}

      {classes.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Class
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Program
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Capacity
                </th>
                <th className="w-8" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {classes.map((c) => {
                const isExpanded = expandedClassId === c.id;
                return (
                  <React.Fragment key={c.id}>
                    <tr
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => toggleExpand(c)}
                    >
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">
                        {c.name}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">
                        {c.programName ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                            LEVEL_COLORS[c.level] ?? "bg-gray-100 text-gray-600"
                          }`}
                        >
                          {c.level}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">
                        {c.maxStudents}
                      </td>
                      <td className="px-2 py-3 text-gray-400">
                        {isExpanded ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan={5} className="p-0">
                          <ClassSessionsPanel
                            programId={c.programId}
                            classId={c.id}
                          />
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
