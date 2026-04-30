"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { X, Loader2, Search } from "lucide-react";
import { useWalkInEligibleStudents } from "@/hooks/useWalkInEligibleStudents";
import { useWalkInBulkRegistration, type BulkResult } from "@/hooks/useWalkInBulkRegistration";

type Props = {
  classId: string;
  sessionDate: string;
  startTime: string;
  durationMinutes: number;
  classLevel: string;
  onClose: () => void;
  onSuccess: () => void;
};

const LEVELS = ["BEGINNER", "INTERMEDIATE", "ADVANCED"] as const;

export function WalkInModal({
  classId,
  sessionDate,
  startTime,
  durationMinutes,
  classLevel,
  onClose,
  onSuccess,
}: Props) {
  const t = useTranslations("attendance.walkIn");
  const tCommon = useTranslations("common");

  const maxHours = Math.max(1, Math.floor(durationMinutes / 60));

  const [q, setQ] = useState("");
  const [levelFilter, setLevelFilter] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [hoursToCharge, setHoursToCharge] = useState<number>(maxHours);
  const [results, setResults] = useState<BulkResult | null>(null);

  const serverLevel = classLevel === "OPEN" ? levelFilter : null;
  const { students, isLoading, error: eligibleError } =
    useWalkInEligibleStudents(classId, sessionDate, startTime, serverLevel);

  const { mutate, isPending, error: submitError } = useWalkInBulkRegistration(classId, sessionDate);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return students;
    return students.filter(
      (s) => s.fullName.toLowerCase().includes(needle) || s.idDocument.startsWith(q.trim())
    );
  }, [students, q]);

  const allFilteredSelected =
    filtered.length > 0 && filtered.every((s) => selectedIds.has(s.studentId));

  const toggleStudent = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (allFilteredSelected) {
        for (const s of filtered) next.delete(s.studentId);
      } else {
        for (const s of filtered) next.add(s.studentId);
      }
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedIds.size === 0) return;
    try {
      const result = await mutate({
        startTime,
        studentIds: Array.from(selectedIds),
        hoursToCharge,
      });
      setResults(result);
      if (result.summary.failed === 0) {
        onSuccess();
      }
    } catch {
      // submitError captured by hook
    }
  };

  const handleRetryFailed = () => {
    if (!results) return;
    const failedIds = results.results
      .filter((r) => r.outcome === "FAILED")
      .map((r) => r.studentId);
    setSelectedIds(new Set(failedIds));
    setResults(null);
  };

  const handleDone = () => {
    onSuccess();
    onClose();
  };

  const hourOptions = Array.from({ length: maxHours }, (_, i) => i + 1);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl mx-4 p-6 max-h-[90vh] flex flex-col">
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

        {/* Results panel */}
        {results && (
          <div className="space-y-4">
            <p className="text-sm text-green-700 bg-green-50 border border-green-200 rounded px-3 py-2">
              {t("resultsSucceeded", { count: results.summary.succeeded })}
            </p>
            {results.summary.failed > 0 && (
              <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded px-3 py-2">
                <p className="font-medium">{t("resultsFailed", { count: results.summary.failed })}</p>
                <ul className="mt-2 list-disc list-inside">
                  {results.results
                    .filter((r) => r.outcome === "FAILED")
                    .map((r) => {
                      const student = students.find((s) => s.studentId === r.studentId);
                      return (
                        <li key={r.studentId}>
                          {student?.fullName ?? r.studentId} — {r.errorCode}
                        </li>
                      );
                    })}
                </ul>
              </div>
            )}
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={handleDone}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
              >
                {t("done")}
              </button>
              {results.summary.failed > 0 && (
                <button
                  type="button"
                  onClick={handleRetryFailed}
                  className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  {t("retryFailed")}
                </button>
              )}
            </div>
          </div>
        )}

        {/* List view */}
        {!results && (
          <form onSubmit={handleSubmit} className="space-y-4 flex flex-col flex-1 min-h-0">
            {/* Search + Level filter */}
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  value={q}
                  onChange={(e) => setQ(e.target.value)}
                  placeholder={t("searchPlaceholder")}
                  className="block w-full rounded-md border border-gray-300 pl-9 pr-3 py-2 text-sm placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
              {classLevel === "OPEN" && (
                <select
                  aria-label={t("levelFilterLabel")}
                  value={levelFilter ?? ""}
                  onChange={(e) => setLevelFilter(e.target.value || null)}
                  className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">{t("levelFilterAll")}</option>
                  {LEVELS.map((lv) => (
                    <option key={lv} value={lv}>{lv}</option>
                  ))}
                </select>
              )}
            </div>

            {/* Select-all bar */}
            <div className="flex items-center justify-between text-sm text-gray-700">
              <label htmlFor="walk-in-select-all" className="flex items-center gap-2">
                <input
                  id="walk-in-select-all"
                  type="checkbox"
                  checked={allFilteredSelected}
                  onChange={toggleSelectAll}
                  aria-label={t("selectAll")}
                />
                {t("selectAll")}
              </label>
              <span>{t("studentCount", { count: filtered.length })}</span>
            </div>

            {/* Student list */}
            <div className="border border-gray-200 rounded-md overflow-hidden flex-1 min-h-0 overflow-y-auto">
              {isLoading && (
                <div className="flex items-center justify-center py-6 text-sm text-gray-500">
                  <Loader2 className="w-4 h-4 animate-spin mr-2" />
                  {tCommon("loading")}
                </div>
              )}
              {!isLoading && eligibleError && (
                <p className="px-4 py-6 text-sm text-red-500 italic text-center">
                  {t("fetchError")}
                </p>
              )}
              {!isLoading && !eligibleError && filtered.length === 0 && students.length > 0 && (
                <p className="px-4 py-6 text-sm text-gray-400 italic text-center">
                  {t("noFilterResults")}
                </p>
              )}
              {!isLoading && !eligibleError && students.length === 0 && (
                <p className="px-4 py-6 text-sm text-gray-400 italic text-center">
                  {t("noResults")}
                </p>
              )}
              {!isLoading && filtered.length > 0 && (
                <ul className="divide-y divide-gray-100">
                  {filtered.map((s) => {
                    const checked = selectedIds.has(s.studentId);
                    return (
                      <li key={s.studentId}>
                        <button
                          type="button"
                          onClick={() => toggleStudent(s.studentId)}
                          className={`w-full text-left px-4 py-3 text-sm flex items-center gap-3 ${
                            checked ? "bg-indigo-50 text-indigo-900" : "text-gray-900 hover:bg-gray-50"
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            readOnly
                            className="pointer-events-none"
                            aria-label={`${s.fullName} ${s.idDocument}`}
                          />
                          <span className="font-medium">{s.fullName}</span>
                          <span className="text-gray-400 text-xs">{s.idDocument}</span>
                          <span className="ml-auto text-xs text-gray-500 uppercase tracking-wide">
                            {s.level}
                          </span>
                          <span className="text-xs text-gray-500">
                            {s.availableHours === -1 ? "∞" : `${s.availableHours}h`}
                          </span>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>

            {/* Submit error banner */}
            {submitError && (
              <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">
                {submitError.message}
              </p>
            )}

            {/* Footer */}
            <div className="flex items-center justify-between gap-3 pt-2 border-t">
              <span className="text-sm text-gray-700">
                {t("selectionCount", { count: selectedIds.size })}
              </span>
              <div className="flex items-center gap-3">
                <label className="text-sm text-gray-700 flex items-center gap-2">
                  {t("hoursLabel")}:
                  <select
                    value={hoursToCharge}
                    onChange={(e) => setHoursToCharge(Number(e.target.value))}
                    className="rounded-md border border-gray-300 px-2 py-1 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  >
                    {hourOptions.map((h) => (
                      <option key={h} value={h}>{h}</option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={onClose}
                  className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  {t("cancelButton")}
                </button>
                <button
                  type="submit"
                  disabled={selectedIds.size === 0 || isPending}
                  className="flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  {t("bulkSubmitButton", { count: selectedIds.size || 1 })}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
