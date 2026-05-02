"use client";

import React, { useState } from "react";
import { useTranslations, useLocale } from "next-intl";
import { AlertTriangle, ChevronDown, ChevronRight } from "lucide-react";
import { useMyClasses } from "@/hooks/useMyClasses";
import { useAvailableSessions } from "@/hooks/useAvailableSessions";
import { useRegisterForSession } from "@/hooks/useRegisterForSession";
import { useCancelRegistration } from "@/hooks/useCancelRegistration";
import SessionCapacityBar from "@/components/attendance/SessionCapacityBar";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
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
  const t = useTranslations("studentClasses");
  const locale = useLocale();
  const today = todayInTenantZone();
  const oneWeekOut = addDays(today, 7);

  const { sessions, loading, error, refetch } = useAvailableSessions(programId, {
    from: today,
    to: oneWeekOut,
  });
  const { register } = useRegisterForSession();
  const { cancel } = useCancelRegistration();

  const [registerError, setRegisterError] = useState<string | null>(null);
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
        err instanceof Error ? err.message : t("registerError")
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
        [rowKey]: err instanceof Error ? err.message : t("cancelError"),
      }));
    }
  }

  return (
    <div className="bg-k-bg px-3 py-3 border-t border-k-border">
      <p
        className="text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2"
        style={{ fontFamily: "var(--font-mono)" }}
      >
        Próximas sesiones — 1 semana
      </p>

      {loading && (
        <p className="text-sm text-k-muted py-2">{t("sessionsLoading")}</p>
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
          {t("sessionsEmpty")}
        </p>
      )}

      {classSessions.length > 0 && (
        <div className="flex flex-col gap-2 p-3">
          {classSessions.map((s) => {
            const isFull = s.currentCapacity >= s.maxStudents;
            const registrationOpen = s.registrationOpen !== false;
            const dateParts = formatSessionDate(s.sessionDate, locale).split(" ");
            const [sy, sm, sd] = s.sessionDate.split("-").map(Number);
            const weekday = new Date(sy, sm - 1, sd).toLocaleDateString(locale, { weekday: "long" });
            return (
              <div
                key={`${s.classId}-${s.sessionDate}`}
                className="flex items-center justify-between rounded-[10px] bg-k-surface border border-k-border px-4 py-3"
              >
                {/* Date badge + time */}
                <div className="flex items-center gap-3">
                  <div className="w-11 h-11 rounded-[10px] bg-k-bg flex flex-col items-center justify-center shrink-0">
                    <span
                      className="text-[9px] uppercase tracking-[0.06em]"
                      style={{ fontFamily: "var(--font-mono)" }}
                    >
                      {dateParts[0] ?? ""}
                    </span>
                    <span className="text-base font-extrabold text-k-dark leading-none">
                      {dateParts[1] ?? ""}
                    </span>
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-sm font-semibold text-k-dark capitalize">
                        {weekday} {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                      </span>
                      {s.status === "ALERTED" && (
                        <span title={s.alertReason ?? "Alerta en esta sesión"}>
                          <AlertTriangle className="w-3.5 h-3.5 text-k-warn-text" />
                        </span>
                      )}
                    </div>
                    <div className="mt-1">
                      <SessionCapacityBar
                        current={s.currentCapacity}
                        max={s.maxStudents}
                      />
                    </div>
                  </div>
                </div>

                {/* Cancel / Register button */}
                {s.registrationId && s.registrationStatus === "REGISTERED" ? (
                  (() => {
                    const rowKey = `${s.classId}-${s.sessionDate}`;
                    const cancellable = isCancellableSession(s.sessionDate, s.startTime);
                    return (
                      <div className="flex flex-col items-end gap-1">
                        <button
                          onClick={() => handleCancel(s)}
                          disabled={!cancellable}
                          title={!cancellable ? `El plazo de cancelación ya cerró` : undefined}
                          className={[
                            "rounded-[8px] px-4 py-1.5 text-xs font-semibold transition-colors",
                            cancellable
                              ? "bg-k-danger-bg text-k-danger-text hover:bg-red-100 border border-k-danger-text/30"
                              : "bg-k-bg text-k-muted cursor-not-allowed",
                          ].join(" ")}
                        >
                          {cancellable ? "Cancelar" : "Plazo cerrado"}
                        </button>
                        {cancelErrors[rowKey] && (
                          <span className="text-[10px] text-k-danger-text">{cancelErrors[rowKey]}</span>
                        )}
                      </div>
                    );
                  })()
                ) : (
                  <button
                    onClick={() => handleRegister(s)}
                    disabled={isFull || !registrationOpen}
                    title={
                      !registrationOpen
                        ? `El registro cierra ${AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES} min antes`
                        : isFull
                        ? "Sesión sin cupo"
                        : undefined
                    }
                    className={[
                      "rounded-[8px] px-4 py-1.5 text-xs font-semibold transition-colors",
                      isFull || !registrationOpen
                        ? "bg-k-bg text-k-muted cursor-not-allowed"
                        : "bg-k-volt text-k-dark hover:bg-[#B8EE3A]",
                    ].join(" ")}
                  >
                    {isFull ? "Sin cupo" : !registrationOpen ? "Cerrado" : "Registrarme"}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ── Page ────────────────────────────────────────────────────────────────────

export default function StudentClassesPage() {
  const t = useTranslations("studentClasses");
  const { classes, loading, error } = useMyClasses();
  const [expandedClassId, setExpandedClassId] = useState<string | null>(null);

  function toggleExpand(c: ProgramClassSummary) {
    setExpandedClassId((prev) => (prev === c.id ? null : c.id));
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">
          Mis clases
        </h1>
        <p
          className="text-xs text-k-muted mt-1"
          style={{ fontFamily: "var(--font-mono)" }}
        >
          Próximas sesiones disponibles
        </p>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">Cargando…</p>
      )}

      {error && (
        <div className="rounded-[8px] bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && classes.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          No hay clases disponibles. Verificá que tenés una inscripción activa.
        </p>
      )}

      {classes.length > 0 && (
        <div className="flex flex-col gap-2.5">
          {classes.map((c) => {
            const isExpanded = expandedClassId === c.id;
            return (
              <div
                key={c.id}
                className={[
                  "overflow-hidden rounded-k-md border bg-k-surface transition-colors",
                  isExpanded ? "border-k-volt" : "border-k-border",
                ].join(" ")}
              >
                {/* Card header — click to expand */}
                <div
                  className="flex items-center justify-between px-5 py-4 cursor-pointer hover:bg-k-bg transition-colors"
                  onClick={() => toggleExpand(c)}
                >
                  <div className="flex items-center gap-4">
                    {/* Accent block */}
                    <div
                      className={[
                        "w-12 h-12 rounded-[10px] flex flex-col items-center justify-center shrink-0",
                        isExpanded ? "bg-k-volt" : "bg-k-bg",
                      ].join(" ")}
                    >
                      <span
                        className="text-[9px] uppercase tracking-[0.06em]"
                        style={{
                          fontFamily: "var(--font-mono)",
                          color: isExpanded ? "#2A4A00" : "#9A9A98",
                        }}
                      >
                        {c.level?.slice(0, 3) ?? "CLS"}
                      </span>
                      <span
                        className="text-lg font-extrabold leading-none"
                        style={{
                          color: isExpanded ? "#0A0A0A" : "#4A4A48",
                        }}
                      >
                        {c.maxStudents}
                      </span>
                    </div>

                    <div>
                      <div className="text-[15px] font-bold text-k-dark">
                        {c.name}
                      </div>
                      <div className="text-sm text-k-subtle mt-0.5">
                        {c.programName ?? "—"}
                      </div>
                      <div className="mt-1.5">
                        <Badge
                          variant={
                            c.level === "BEGINNER"
                              ? "beginner"
                              : c.level === "INTERMEDIATE"
                              ? "intermediate"
                              : c.level === "ADVANCED"
                              ? "advanced"
                              : "inactive"
                          }
                          label={c.level ?? ""}
                          small
                        />
                      </div>
                    </div>
                  </div>

                  {isExpanded ? (
                    <ChevronDown className="h-4 w-4 text-k-muted" />
                  ) : (
                    <ChevronRight className="h-4 w-4 text-k-muted" />
                  )}
                </div>

                {/* Sessions panel (existing component, untouched) */}
                {isExpanded && (
                  <ClassSessionsPanel
                    programId={c.programId}
                    classId={c.id}
                  />
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
