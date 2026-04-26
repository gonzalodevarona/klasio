"use client";

import React, { useState } from "react";
import { AlertTriangle, ChevronDown, ChevronRight } from "lucide-react";
import { useMyClasses } from "@/hooks/useMyClasses";
import { useAvailableSessions } from "@/hooks/useAvailableSessions";
import { useRegisterForSession } from "@/hooks/useRegisterForSession";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import SessionCapacityBar from "@/components/attendance/SessionCapacityBar";
import { Badge } from "@/components/ui";
import { AvailableSession } from "@/lib/types/attendance";
import { ProgramClassSummary } from "@/lib/types/programClass";
import { AttendanceTimeConstants, todayInTenantZone, addDays, formatSessionDate } from "@/lib/attendanceConstants";

function computeIntendedHours(start: string, end: string): number {
  const [sh, sm] = start.split(":").map(Number);
  const [eh, em] = end.split(":").map(Number);
  const durationMinutes = (eh * 60 + em) - (sh * 60 + sm);
  return Math.max(1, Math.floor(durationMinutes / 60));
}

/**
 * Returns true if the cancellation window is still open for the given session.
 * The window closes CANCELLATION_CUTOFF_MINUTES before the session starts.
 */
function isCancellableSession(sessionDate: string, startTime: string): boolean {
  const sessionStart = new Date(`${sessionDate}T${startTime}`);
  const cutoffMs = sessionStart.getTime() - AttendanceTimeConstants.CANCELLATION_CUTOFF_MINUTES * 60 * 1000;
  return Date.now() < cutoffMs;
}

// ── ClassSessionsPanel ──────────────────────────────────────────────────────

interface ClassSessionsPanelProps {
  programId: string;
  classId: string;
}

function ClassSessionsPanel({ programId, classId }: ClassSessionsPanelProps) {
  const today = todayInTenantZone();
  const oneWeekOut = addDays(today, 7);

  const { sessions, loading, error, refetch } = useAvailableSessions(programId, {
    from: today,
    to: oneWeekOut,
  });
  const { register } = useRegisterForSession();
  const { cancel } = useCancelRegistration();

  const [registerError, setRegisterError] = useState<string | null>(null);
  // Keyed by `classId-sessionDate` — stores per-row cancel errors.
  const [cancelErrors, setCancelErrors] = useState<Record<string, string>>({});

  const classSessions = sessions.filter(
    (s) => s.classId === classId && s.status !== "CANCELLED"
  );

  async function handleRegister(s: AvailableSession) {
    setRegisterError(null);
    try {
      const hours = computeIntendedHours(s.startTime, s.endTime);
      await register(classId, s.sessionDate, hours);
      refetch();
    } catch (err) {
      setRegisterError(
        err instanceof Error ? err.message : "Failed to register. Please try again."
      );
    }
  }

  async function handleCancel(s: AvailableSession) {
    const rowKey = `${s.classId}-${s.sessionDate}`;
    setCancelErrors((prev) => {
      const next = { ...prev };
      delete next[rowKey];
      return next;
    });
    try {
      await cancel(s.registrationId!);
      refetch();
    } catch (err) {
      setCancelErrors((prev) => ({
        ...prev,
        [rowKey]: err instanceof Error ? err.message : "Failed to cancel. Please try again.",
      }));
    }
  }

  return (
    <div className="bg-k-bg px-4 py-3 border-t border-k-line">
      <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
        Upcoming Sessions — next 7 days
      </p>

      {loading && (
        <p className="text-sm text-k-muted py-2">Loading sessions…</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-2 text-xs text-k-danger-text mb-2">
          {error}
        </div>
      )}

      {registerError && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 px-3 py-2 text-xs text-k-danger-text mb-2">
          {registerError}
        </div>
      )}

      {!loading && !error && classSessions.length === 0 && (
        <p className="text-sm text-k-muted py-1">
          No upcoming sessions in the next 7 days.
        </p>
      )}

      {classSessions.length > 0 && (
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-xs text-k-muted">
              <th className="py-1 pr-4 text-left font-medium">Date</th>
              <th className="py-1 pr-4 text-left font-medium">Time</th>
              <th className="py-1 pr-4 text-left font-medium">Capacity</th>
              <th className="py-1 text-left font-medium"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-k-line">
            {classSessions.map((s) => {
              const rowKey = `${s.classId}-${s.sessionDate}`;
              const isFull = s.currentCapacity >= s.maxStudents;
              const registrationOpen = s.registrationOpen !== false;
              const isRegistered = s.registrationStatus === "REGISTERED";
              const canCancel = isRegistered && isCancellableSession(s.sessionDate, s.startTime);
              const rowCancelErr = cancelErrors[rowKey] ?? null;

              return (
                <React.Fragment key={rowKey}>
                  <tr>
                    <td className="py-2 pr-4 text-k-dark">
                      <div className="flex items-center gap-1.5">
                        <span>{formatSessionDate(s.sessionDate)}</span>
                        {s.status === "ALERTED" && (
                          <span
                            title={s.alertReason ?? "Alert issued for this session"}
                            className="inline-flex text-k-warn-text"
                          >
                            <AlertTriangle className="w-4 h-4" />
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-2 pr-4 text-k-muted whitespace-nowrap">
                      {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                    </td>
                    <td className="py-2 pr-4">
                      <SessionCapacityBar
                        current={s.currentCapacity}
                        max={s.maxStudents}
                      />
                    </td>
                    <td className="py-2">
                      {isRegistered ? (
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-medium text-k-volt-text bg-k-volt/20 px-2 py-0.5 rounded">
                            Registered
                          </span>
                          {canCancel ? (
                            <button
                              onClick={() => handleCancel(s)}
                              className="text-xs text-k-danger-text hover:text-k-dark transition-colors"
                            >
                              Cancel
                            </button>
                          ) : (
                            <span
                              title="Cancellation window closed"
                              className="text-xs text-k-muted cursor-not-allowed"
                            >
                              Cancel
                            </span>
                          )}
                        </div>
                      ) : (
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
                              ? "bg-k-bg text-k-muted cursor-not-allowed"
                              : "bg-k-volt text-k-dark hover:bg-k-volt-hover",
                          ].join(" ")}
                        >
                          {isFull ? "Full" : !registrationOpen ? "Closed" : "Register"}
                        </button>
                      )}
                    </td>
                  </tr>
                  {rowCancelErr && (
                    <tr>
                      <td colSpan={4} className="pb-2 pt-0">
                        <div className="rounded bg-k-danger-bg border border-k-danger-text/30 px-3 py-1.5 text-xs text-k-danger-text">
                          {rowCancelErr}
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
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
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">My Classes</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
          Classes available to you based on your enrollment level.
        </p>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">Loading…</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && classes.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          No classes found. Make sure you have an active enrollment.
        </p>
      )}

      {classes.length > 0 && (
        <div className="overflow-hidden rounded-k-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Class
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Program
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  Capacity
                </th>
                <th className="w-8" />
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {classes.map((c) => {
                const isExpanded = expandedClassId === c.id;
                return (
                  <React.Fragment key={c.id}>
                    <tr
                      className="hover:bg-k-bg cursor-pointer"
                      onClick={() => toggleExpand(c)}
                    >
                      <td className="px-4 py-3 text-sm font-medium text-k-dark">
                        {c.name}
                      </td>
                      <td className="px-4 py-3 text-sm text-k-muted">
                        {c.programName ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        <Badge
                          variant={
                            c.level === "BEGINNER"
                              ? "beginner"
                              : c.level === "INTERMEDIATE"
                              ? "intermediate"
                              : c.level === "ADVANCED"
                              ? "advanced"
                              : "info"
                          }
                          label={c.level}
                          small
                        />
                      </td>
                      <td className="px-4 py-3 text-sm text-k-muted">
                        {c.maxStudents}
                      </td>
                      <td className="px-2 py-3 text-k-muted">
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
