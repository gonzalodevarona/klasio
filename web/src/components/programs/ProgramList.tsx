"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations, useLocale } from "next-intl";
import { formatDate } from "@/lib/utils";
import { ProgramStatus } from "@/lib/types/program";
import { usePrograms } from "@/hooks/usePrograms";
import ProgramStatusBadge from "./ProgramStatusBadge";
import { Table, Thead, Th, Tr, Td, Select, Button } from "@/components/ui";

export default function ProgramList() {
  const t = useTranslations("programs");
  const tPagination = useTranslations("pagination");
  const locale = useLocale();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<ProgramStatus | undefined>(
    undefined
  );
  const SIZE = 20;

  const { programs, totalPages, totalElements, loading, error } = usePrograms(
    page,
    SIZE,
    statusFilter
  );

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : (value as ProgramStatus));
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
      {/* Filter */}
      <div className="flex items-center gap-3">
        <Select
          label={t("filterStatusLabel")}
          value={statusFilter ?? ""}
          onChange={(e) => handleStatusChange(e.target.value)}
        >
          <option value="">{t("filterAll")}</option>
          <option value="ACTIVE">{t("filterActive")}</option>
          <option value="INACTIVE">{t("filterInactive")}</option>
        </Select>
      </div>

      {loading ? (
        <div className="text-center py-8 text-sm text-gray-500">
          {t("listLoading")}
        </div>
      ) : programs.length === 0 ? (
        <div className="text-center py-8 text-sm text-gray-500">
          {t("listEmpty")}
        </div>
      ) : (
        <>
          <Table>
            <Thead>
              <tr>
                <Th>{t("colName")}</Th>
                <Th>{t("colStatus")}</Th>
                <Th>{t("colCreatedAt")}</Th>
              </tr>
            </Thead>
            <tbody>
              {programs.map((program) => (
                <Tr key={program.id}>
                  <Td bold>
                    <Link
                      href={`/programs/${program.id}`}
                      className="hover:text-blue-600 hover:underline"
                    >
                      {program.name}
                    </Link>
                  </Td>
                  <Td>
                    <ProgramStatusBadge status={program.status} />
                  </Td>
                  <Td muted>{formatDate(program.createdAt, locale)}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>

          {/* Pagination */}
          <div className="flex items-center justify-between border-t border-k-line pt-4">
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
  );
}
