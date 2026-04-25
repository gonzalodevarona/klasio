"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { TenantStatus } from "@/lib/types/tenant";
import { useTenants } from "@/hooks/useTenants";
import TenantStatusBadge from "./TenantStatusBadge";
import { Table, Thead, Th, Tr, Td, Select, Button } from "@/components/ui";

export default function TenantList() {
  const t = useTranslations("tenants");
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<TenantStatus | undefined>(
    undefined
  );
  const SIZE = 10;

  const { tenants, totalPages, totalElements, loading, error } = useTenants(
    page,
    SIZE,
    statusFilter
  );

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : (value as TenantStatus));
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
      ) : tenants.length === 0 ? (
        <div className="text-center py-8 text-sm text-gray-500">
          {t("listEmpty")}
        </div>
      ) : (
        <>
          <Table>
            <Thead>
              <tr>
                <Th>{t("colName")}</Th>
                <Th>{t("colSlug")}</Th>
                <Th>{t("colDiscipline")}</Th>
                <Th>{t("colStatus")}</Th>
                <Th>{t("colCreatedAt")}</Th>
              </tr>
            </Thead>
            <tbody>
              {tenants.map((tenant) => (
                <Tr key={tenant.id}>
                  <Td bold>
                    <Link
                      href={`/tenants/${tenant.slug}`}
                      className="hover:text-blue-600 hover:underline"
                    >
                      {tenant.name}
                    </Link>
                  </Td>
                  <Td mono muted>{tenant.slug}</Td>
                  <Td muted>{tenant.discipline}</Td>
                  <Td>
                    <TenantStatusBadge status={tenant.status} />
                  </Td>
                  <Td muted>{formatDate(tenant.createdAt)}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>

          {/* Pagination */}
          <div className="flex items-center justify-between border-t border-k-line pt-4">
            <p className="text-sm text-k-subtle">
              Page {page + 1} of {totalPages} ({totalElements} total)
            </p>
            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
