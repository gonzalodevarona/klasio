"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { StudentStatus } from "@/lib/types/student";
import { useStudents } from "@/hooks/useStudents";
import StudentStatusBadge from "./StudentStatusBadge";

export default function StudentList() {
  const t = useTranslations("students");
  const tCommon = useTranslations("common");
  const tPagination = useTranslations("pagination");

  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StudentStatus | undefined>(
    undefined
  );
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const SIZE = 20;

  const { students, totalPages, totalElements, loading, error } = useStudents(
    page,
    SIZE,
    statusFilter,
    search
  );

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : (value as StudentStatus));
    setPage(0);
  }

  function handleSearch() {
    setSearch(searchInput.trim());
    setPage(0);
  }

  function handleSearchKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      handleSearch();
    }
  }

  function handleClearSearch() {
    setSearchInput("");
    setSearch("");
    setPage(0);
  }

  function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  if (error) {
    return (
      <div
        className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
        role="alert"
      >
        {error}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex items-center gap-3 flex-wrap">
        <label htmlFor="statusFilter" className="text-sm font-medium text-gray-700">
          {t("filterStatusLabel")}
        </label>
        <select
          id="statusFilter"
          value={statusFilter ?? ""}
          onChange={(e) => handleStatusChange(e.target.value)}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">{tCommon("all")}</option>
          <option value="ACTIVE">{tCommon("active")}</option>
          <option value="INACTIVE">{tCommon("inactive")}</option>
        </select>

        <div className="flex items-center gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={handleSearchKeyDown}
            placeholder={t("filterSearchPlaceholder")}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-64"
          />
          <button
            type="button"
            onClick={handleSearch}
            className="inline-flex items-center rounded-md bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {tCommon("search")}
          </button>
          {search && (
            <button
              type="button"
              onClick={handleClearSearch}
              className="inline-flex items-center rounded-md bg-white px-3 py-1.5 text-sm font-medium text-gray-500 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {tCommon("clear")}
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8 text-sm text-gray-500">
          {t("listLoading")}
        </div>
      ) : students.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-sm text-gray-500">{t("listEmpty")}</p>
          <Link
            href="/students/new"
            className="mt-2 inline-flex items-center text-sm text-blue-600 hover:text-blue-700"
          >
            {t("listEmptyAction")}
          </Link>
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("colName")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("colDocument")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("colEmail")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("colAge")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("colStatus")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("colMembership")}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {students.map((student) => (
                  <tr key={student.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      <Link
                        href={`/students/${student.id}`}
                        className="hover:text-blue-600 hover:underline"
                      >
                        {student.firstName} {student.lastName}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">
                      {student.identityDocumentType} {student.identityNumber}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {student.email}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {student.age}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StudentStatusBadge status={student.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {student.hasActiveMembership ? (
                        <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                          {t("membershipActive")}
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600">
                          {t("membershipNone")}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
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
  );
}
