"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations, useLocale } from "next-intl";
import { formatDate } from "@/lib/utils";
import { ClassLevel, ClassStatus } from "@/lib/types/programClass";
import { useProgramClasses } from "@/hooks/useProgramClasses";
import ClassLevelBadge from "./ClassLevelBadge";
import ClassTypeBadge from "./ClassTypeBadge";
import ClassStatusBadge from "./ClassStatusBadge";
import { Table, Thead, Th, Tr, Td, Select, Button } from "@/components/ui";

interface ClassListProps {
  programId: string;
}

export default function ClassList({ programId }: ClassListProps) {
  const t = useTranslations("classes");
  const tPagination = useTranslations("pagination");
  const locale = useLocale();
  const [page, setPage] = useState(0);
  const [levelFilter, setLevelFilter] = useState<ClassLevel | undefined>(
    undefined
  );
  const [statusFilter, setStatusFilter] = useState<ClassStatus | undefined>(
    undefined
  );
  const SIZE = 20;

  const { classes, totalPages, totalElements, loading, error } = useProgramClasses(
    programId,
    page,
    SIZE,
    levelFilter,
    statusFilter
  );

  function handleLevelChange(value: string) {
    setLevelFilter(value === "" ? undefined : (value as ClassLevel));
    setPage(0);
  }

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : (value as ClassStatus));
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
      <div className="flex items-center gap-3">
        <Select
          label={t("filterLevelLabel")}
          value={levelFilter ?? ""}
          onChange={(e) => handleLevelChange(e.target.value)}
        >
          <option value="">{t("filterAll")}</option>
          <option value="BEGINNER">{t("filterBeginnerOption")}</option>
          <option value="INTERMEDIATE">{t("filterIntermediateOption")}</option>
          <option value="ADVANCED">{t("filterAdvancedOption")}</option>
        </Select>

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
      ) : classes.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-sm text-gray-500">{t("listEmpty")}</p>
          <Link
            href={`/programs/${programId}/classes/new`}
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
                <Th>{t("colLevel")}</Th>
                <Th>{t("colType")}</Th>
                <Th>{t("colMaxStudents")}</Th>
                <Th>{t("colStatus")}</Th>
                <Th>{t("colCreated")}</Th>
              </tr>
            </Thead>
            <tbody>
              {classes.map((c) => (
                <Tr key={c.id}>
                  <Td bold>
                    <Link
                      href={`/programs/${programId}/classes/${c.id}`}
                      className="hover:text-blue-600 hover:underline"
                    >
                      {c.name}
                    </Link>
                  </Td>
                  <Td>
                    <ClassLevelBadge level={c.level} />
                  </Td>
                  <Td>
                    <ClassTypeBadge type={c.type} />
                  </Td>
                  <Td muted>{c.maxStudents}</Td>
                  <Td>
                    <ClassStatusBadge status={c.status} />
                  </Td>
                  <Td muted>{formatDate(c.createdAt, locale)}</Td>
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
