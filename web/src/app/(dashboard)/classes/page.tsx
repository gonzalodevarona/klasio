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
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
      </div>

      <div className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <label htmlFor="programNameFilter" className="text-sm font-medium text-gray-700">
              {t("filterProgramLabel")}
            </label>
            <input
              id="programNameFilter"
              type="text"
              value={programNameInput}
              onChange={(e) => setProgramNameInput(e.target.value)}
              placeholder={t("filterProgramPlaceholder")}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-48"
            />
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="levelFilter" className="text-sm font-medium text-gray-700">
              {t("filterLevelLabel")}
            </label>
            <select
              id="levelFilter"
              value={levelFilter ?? ""}
              onChange={(e) => {
                setLevelFilter(e.target.value === "" ? undefined : (e.target.value as ClassLevel));
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">{t("filterAll")}</option>
              <option value="BEGINNER">{t("filterBeginnerOption")}</option>
              <option value="INTERMEDIATE">{t("filterIntermediateOption")}</option>
              <option value="ADVANCED">{t("filterAdvancedOption")}</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="statusFilter" className="text-sm font-medium text-gray-700">
              {t("filterStatusLabel")}
            </label>
            <select
              id="statusFilter"
              value={statusFilter ?? ""}
              onChange={(e) => {
                setStatusFilter(e.target.value === "" ? undefined : (e.target.value as ClassStatus));
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">{t("filterAll")}</option>
              <option value="ACTIVE">{t("filterActive")}</option>
              <option value="INACTIVE">{t("filterInactive")}</option>
            </select>
          </div>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-gray-500">{t("listLoading")}</div>
        )}

        {error && (
          <div
            className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
            role="alert"
          >
            {error}
          </div>
        )}

        {!loading && !error && classes.length === 0 && (
          <div className="text-center py-8 text-sm text-gray-500">{t("listEmpty")}</div>
        )}

        {!loading && !error && classes.length > 0 && (
          <>
            <div className="overflow-x-auto rounded-lg border border-gray-200">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="w-10 px-3 py-3" aria-label="Expand" />
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colName")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colProgram")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colLevel")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colType")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colProfessor")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colMaxStudents")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colStatus")}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t("colCreated")}
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {classes.map((c) => (
                    <React.Fragment key={c.id}>
                      <tr
                        className={`hover:bg-gray-50 cursor-pointer ${expandedClassId === c.id ? "bg-blue-50" : ""}`}
                        onClick={() => toggleExpand(c.id)}
                      >
                        <td className="px-3 py-4 text-center text-gray-400">
                          {expandedClassId === c.id ? (
                            <ChevronDown className="w-4 h-4 mx-auto text-blue-500" />
                          ) : (
                            <ChevronRight className="w-4 h-4 mx-auto" />
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                          <Link
                            href={`/programs/${c.programId}/classes/${c.id}`}
                            className="hover:text-blue-600 hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {c.name}
                          </Link>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          <Link
                            href={`/programs/${c.programId}/classes`}
                            className="hover:text-blue-600 hover:underline"
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
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {c.professorName ?? <span className="text-gray-400 italic">{t("colUnassigned")}</span>}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {c.maxStudents}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <ClassStatusBadge status={c.status} />
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
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

            <div className="flex items-center justify-between border-t border-gray-200 pt-4">
              <p className="text-sm text-gray-700">
                {tPagination("summary", { current: page + 1, total: totalPages, count: totalElements })}
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {tPagination("previous")}
                </button>
                <button
                  type="button"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                  className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {tPagination("next")}
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
