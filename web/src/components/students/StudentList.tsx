"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { StudentStatus } from "@/lib/types/student";
import { useStudents } from "@/hooks/useStudents";
import StudentStatusBadge from "./StudentStatusBadge";
import { Table, Thead, Th, Tr, Td, Input, Select, Button } from "@/components/ui";

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
        <Select
          label={t("filterStatusLabel")}
          value={statusFilter ?? ""}
          onChange={(e) => handleStatusChange(e.target.value)}
        >
          <option value="">{tCommon("all")}</option>
          <option value="ACTIVE">{tCommon("active")}</option>
          <option value="INACTIVE">{tCommon("inactive")}</option>
        </Select>

        <div className="flex items-center gap-2">
          <Input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={handleSearchKeyDown}
            placeholder={t("filterSearchPlaceholder")}
          />
          <Button variant="outline" size="sm" type="button" onClick={handleSearch}>
            {tCommon("search")}
          </Button>
          {search && (
            <Button variant="outline" size="sm" type="button" onClick={handleClearSearch}>
              {tCommon("clear")}
            </Button>
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
          <Table>
            <Thead>
              <tr>
                <Th>{t("colName")}</Th>
                <Th>{t("colDocument")}</Th>
                <Th>{t("colEmail")}</Th>
                <Th>{t("colAge")}</Th>
                <Th>{t("colStatus")}</Th>
                <Th>{t("colMembership")}</Th>
              </tr>
            </Thead>
            <tbody>
              {students.map((student) => (
                <Tr key={student.id}>
                  <Td bold>
                    <Link
                      href={`/students/${student.id}`}
                      className="hover:text-blue-600 hover:underline"
                    >
                      {student.firstName} {student.lastName}
                    </Link>
                  </Td>
                  <Td mono muted>
                    {student.identityDocumentType} {student.identityNumber}
                  </Td>
                  <Td muted>{student.email}</Td>
                  <Td muted>{student.age}</Td>
                  <Td>
                    <StudentStatusBadge status={student.status} />
                  </Td>
                  <Td>
                    {student.hasActiveMembership ? (
                      <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                        {t("membershipActive")}
                      </span>
                    ) : (
                      <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600">
                        {t("membershipNone")}
                      </span>
                    )}
                  </Td>
                </Tr>
              ))}
            </tbody>
          </Table>

          {/* Pagination */}
          <div className="flex items-center justify-between border-t border-gray-200 pt-4">
            <p className="text-sm text-gray-700">
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
  );
}
