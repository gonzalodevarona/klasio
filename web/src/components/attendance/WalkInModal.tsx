"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { X, Loader2, Search } from "lucide-react";
import { useWalkInEligibleStudents } from "@/hooks/useWalkInEligibleStudents";
import { useWalkInRegistration } from "@/hooks/useWalkInRegistration";

type Props = {
  classId: string;
  sessionDate: string;
  startTime: string;
  durationMinutes: number;
  onClose: () => void;
  onSuccess: () => void;
};

function translateError(t: ReturnType<typeof useTranslations>, code: string): string {
  const map: Record<string, string> = {
    CLASS_NOT_FOUND: t("errors.classNotFound"),
    CLASS_INACTIVE: t("errors.classInactive"),
    FORBIDDEN: t("errors.forbidden"),
    INVALID_DATE: t("errors.invalidDate"),
    MARKING_WINDOW: t("errors.outsideWindow"),
    SESSION_CANCELLED: t("errors.sessionCancelled"),
    ENROLLMENT_NOT_FOUND: t("errors.notEnrolled"),
    CLASS_LEVEL_MISMATCH: t("errors.levelMismatch"),
    MEMBERSHIP_NOT_ACTIVE: t("errors.noActiveMembership"),
    INSUFFICIENT_HOURS: t("errors.insufficientHours"),
    INVALID_HOURS: t("errors.invalidHours"),
    SESSION_FULL: t("errors.sessionFull"),
    ALREADY_MARKED: t("errors.alreadyMarked"),
  };
  return map[code] ?? t("errors.forbidden");
}

export function WalkInModal({
  classId,
  sessionDate,
  startTime,
  durationMinutes,
  onClose,
  onSuccess,
}: Props) {
  const t = useTranslations("attendance.walkIn");
  const tCommon = useTranslations("common");

  const maxHours = Math.max(1, Math.floor(durationMinutes / 60));

  const [q, setQ] = useState("");
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);
  const [hoursToCharge, setHoursToCharge] = useState<number>(maxHours);

  const { students, isLoading } = useWalkInEligibleStudents(classId, sessionDate, startTime, q);
  const { mutate, isPending, error } = useWalkInRegistration(classId, sessionDate);

  const showSearch = students.length >= 50 || q.length > 0;

  const hourOptions = Array.from({ length: maxHours }, (_, i) => i + 1);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStudentId) return;

    await mutate({ startTime, studentId: selectedStudentId, hoursToCharge });
    onSuccess();
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">{t("modalTitle")}</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:text-gray-600 focus:outline-none"
            aria-label={tCommon("close")}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Search input — shown only when needed */}
          {showSearch && (
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder={t("searchPlaceholder")}
                className="block w-full rounded-md border border-gray-300 pl-9 pr-3 py-2 text-sm placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          )}

          {/* Student list */}
          <div className="border border-gray-200 rounded-md overflow-hidden max-h-64 overflow-y-auto">
            {isLoading && (
              <div className="flex items-center justify-center py-6 text-sm text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin mr-2" />
                {tCommon("loading")}
              </div>
            )}
            {!isLoading && students.length === 0 && (
              <p className="px-4 py-6 text-sm text-gray-400 italic text-center">
                {t("noResults")}
              </p>
            )}
            {!isLoading && students.length > 0 && (
              <ul className="divide-y divide-gray-100">
                {students.map((s) => (
                  <li key={s.studentId}>
                    <button
                      type="button"
                      onClick={() => setSelectedStudentId(s.studentId)}
                      className={`w-full text-left px-4 py-3 text-sm transition-colors ${
                        selectedStudentId === s.studentId
                          ? "bg-indigo-50 text-indigo-900"
                          : "text-gray-900 hover:bg-gray-50"
                      }`}
                    >
                      <span className="font-medium">{s.fullName}</span>
                      <span className="ml-2 text-gray-400 text-xs">{s.idDocument}</span>
                      <span className="ml-auto float-right text-xs text-gray-500">
                        {s.availableHours}h
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Hours to charge */}
          <div>
            <label
              htmlFor="walk-in-hours"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              {t("hoursLabel")}
            </label>
            <select
              id="walk-in-hours"
              value={hoursToCharge}
              onChange={(e) => setHoursToCharge(Number(e.target.value))}
              className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {hourOptions.map((h) => (
                <option key={h} value={h}>
                  {h}
                </option>
              ))}
            </select>
          </div>

          {/* Error */}
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
              {translateError(t, error.code)}
            </p>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              {t("cancelButton")}
            </button>
            <button
              type="submit"
              disabled={!selectedStudentId || isPending}
              className="flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              {t("submitButton")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
