"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import LevelBadge from "@/components/enrollments/LevelBadge";
import { Badge, Select } from "@/components/ui";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentEnrollmentsPage() {
  const t = useTranslations("studentEnrollmentsPage");
  const tCommon = useTranslations("common");

  const STATUS_OPTIONS = [
    { value: "ACTIVE", label: tCommon("active") },
    { value: "INACTIVE", label: tCommon("inactive") },
    { value: "", label: tCommon("all") },
  ];

  const [statusFilter, setStatusFilter] = useState<string>("ACTIVE");
  const { enrollments, loading, error } = useMyEnrollments(statusFilter || undefined);

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
          <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">
            {t("subtitle")}
          </p>
        </div>
        <div className="flex items-center gap-2 pt-1">
          <label htmlFor="statusFilter" className="text-sm text-k-muted whitespace-nowrap">
            {t("statusLabel")}
          </label>
          <Select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-auto"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-k-muted">{t("loading")}</p>
      )}

      {error && (
        <div className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text">
          {error}
        </div>
      )}

      {!loading && !error && enrollments.length === 0 && (
        <p className="py-8 text-center text-sm text-k-muted">
          {t("empty", { status: statusFilter ? statusFilter.toLowerCase() : "" })}
        </p>
      )}

      {enrollments.length > 0 && (
        <div className="overflow-hidden rounded-k-lg border border-k-border bg-k-surface">
          <table className="min-w-full divide-y divide-k-border">
            <thead className="bg-k-bg">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colProgram")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colLevel")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colStatus")}
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                  {t("colEnrolled")}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-k-line">
              {enrollments.map((e) => (
                <tr key={e.id} className="hover:bg-k-bg">
                  <td className="px-4 py-3 text-sm font-medium text-k-dark">
                    {e.programName}
                  </td>
                  <td className="px-4 py-3">
                    <LevelBadge level={e.level} />
                  </td>
                  <td className="px-4 py-3">
                    <Badge
                      variant={e.status === "ACTIVE" ? "active" : "inactive"}
                      label={e.status}
                      small
                    />
                  </td>
                  <td className="px-4 py-3 text-sm text-k-muted">
                    {formatDate(e.enrollmentDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
