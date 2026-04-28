"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ChevronDown, ChevronRight } from "lucide-react";
import { ClassLevel, ClassStatus } from "@/lib/types/programClass";
import { useAllClasses } from "@/hooks/useProgramClasses";
import { useAuth } from "@/hooks/useAuth";
import { primaryRole } from "@/lib/types/auth";
import ClassLevelBadge from "@/components/classes/ClassLevelBadge";
import ClassTypeBadge from "@/components/classes/ClassTypeBadge";
import ClassStatusBadge from "@/components/classes/ClassStatusBadge";
import ClassRosterPanel from "@/components/attendance/ClassRosterPanel";
import { Button, Input, Select } from "@/components/ui";

export default function AllClassesPage() {
  const t = useTranslations("classes");
  const tPagination = useTranslations("pagination");
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [levelFilter, setLevelFilter] = useState<ClassLevel | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<ClassStatus | undefined>(undefined);
  const [programNameInput, setProgramNameInput] = useState("");
  const [programNameFilter, setProgramNameFilter] = useState<string | undefined>(undefined);
  const [expandedClassId, setExpandedClassId] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const SIZE = 20;

  // Debounce the program name filter by 300ms
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setProgramNameFilter(programNameInput.trim() || undefined);
      setPage(0);
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [programNameInput]);

  const { classes, totalPages, totalElements, loading, error } = useAllClasses(
    page,
    SIZE,
    levelFilter,
    statusFilter,
    programNameFilter
  );

  function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  function toggleExpand(classId: string) {
    setExpandedClassId((prev) => (prev === classId ? null : classId));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>

      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <label htmlFor="programNameFilter" className="text-sm font-medium text-k-subtle">
              {t("filterProgramLabel")}
            </label>
            <Input
              id="programNameFilter"
              type="text"
              value={programNameInput}
              onChange={(e) => setProgramNameInput(e.target.value)}
              placeholder={t("filterProgramPlaceholder")}
              className="w-48"
            />
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="levelFilter" className="text-sm font-medium text-k-subtle">
              {t("filterLevelLabel")}
            </label>
            <Select
              id="levelFilter"
              value={levelFilter ?? ""}
              onChange={(e) => {
                setLevelFilter(e.target.value === "" ? undefined : (e.target.value as ClassLevel));
                setPage(0);
              }}
              className="w-auto"
            >
              <option value="">{t("filterAll")}</option>
              <option value="BEGINNER">{t("filterBeginnerOption")}</option>
              <option value="INTERMEDIATE">{t("filterIntermediateOption")}</option>
              <option value="ADVANCED">{t("filterAdvancedOption")}</option>
              <option value="OPEN">{t("filterOpenOption")}</option>
            </Select>
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="statusFilter" className="text-sm font-medium text-k-subtle">
              {t("filterStatusLabel")}
            </label>
            <Select
              id="statusFilter"
              value={statusFilter ?? ""}
              onChange={(e) => {
                setStatusFilter(e.target.value === "" ? undefined : (e.target.value as ClassStatus));
                setPage(0);
              }}
              className="w-auto"
            >
              <option value="">{t("filterAll")}</option>
              <option value="ACTIVE">{t("filterActive")}</option>
              <option value="INACTIVE">{t("filterInactive")}</option>
            </Select>
          </div>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-k-muted">{t("listLoading")}</div>
        )}

        {error && (
          <div
            className="rounded-k-sm bg-k-danger-bg p-4 text-sm text-k-danger-text border border-k-danger-text/30"
            role="alert"
          >
            {error}
          </div>
        )}

        {!loading && !error && classes.length === 0 && (
          <div className="text-center py-8 text-sm text-k-muted">{t("listEmpty")}</div>
        )}

        {!loading && !error && classes.length > 0 && (
          <>
            <div className="overflow-x-auto rounded-lg border border-k-border">
              <table className="min-w-full divide-y divide-k-border">
                <thead className="bg-k-bg">
                  <tr>
                    <th className="w-10 px-3 py-3" aria-label="Expand" />
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colName")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colProgram")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colLevel")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colType")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colProfessor")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colMaxStudents")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colStatus")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                      {t("colCreated")}
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-k-surface divide-y divide-k-border">
                  {classes.map((c) => (
                    <React.Fragment key={c.id}>
                      <tr
                        className={`hover:bg-k-bg cursor-pointer ${expandedClassId === c.id ? "bg-k-bg" : ""}`}
                        onClick={() => toggleExpand(c.id)}
                      >
                        <td className="px-3 py-4 text-center text-k-muted">
                          {expandedClassId === c.id ? (
                            <ChevronDown className="w-4 h-4 mx-auto text-k-volt" />
                          ) : (
                            <ChevronRight className="w-4 h-4 mx-auto" />
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-k-dark">
                          <Link
                            href={`/programs/${c.programId}/classes/${c.id}`}
                            className="hover:text-k-subtle hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {c.name}
                          </Link>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          <Link
                            href={`/programs/${c.programId}/classes`}
                            className="hover:text-k-subtle hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {c.programName ?? c.programId}
                          </Link>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassLevelBadge level={c.level} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassTypeBadge type={c.type} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          {c.professorName ?? <span className="text-k-muted italic">{t("colUnassigned")}</span>}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          {c.maxStudents}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassStatusBadge status={c.status} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                          {formatDate(c.createdAt)}
                        </td>
                      </tr>

                      {expandedClassId === c.id && (
                        <tr>
                          <td colSpan={9} className="p-0">
                            <ClassRosterPanel classId={c.id} userRole={user ? primaryRole(user.roles) : undefined} />
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between border-t border-k-border pt-4">
              <p className="text-sm text-k-subtle">
                {tPagination("summary", { current: page + 1, total: totalPages, count: totalElements })}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  {tPagination("previous")}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                >
                  {tPagination("next")}
                </Button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
