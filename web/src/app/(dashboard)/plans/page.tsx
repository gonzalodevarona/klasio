"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useAllPlans } from "@/hooks/usePlans";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";
import { Select } from "@/components/ui";

function formatCost(cost: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(cost);
}

export default function PlansPage() {
  const t = useTranslations("programs");
  const [statusFilter, setStatusFilter] = useState<string | undefined>(
    undefined
  );
  const { plans, loading, error } = useAllPlans(statusFilter);

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : value);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("allPlansPageTitle")}</h1>
      </div>

      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <label
            htmlFor="statusFilter"
            className="text-sm font-medium text-k-subtle"
          >
            {t("filterStatusLabel")}
          </label>
          <Select
            id="statusFilter"
            value={statusFilter ?? ""}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="w-auto"
          >
            <option value="">{t("filterAll")}</option>
            <option value="ACTIVE">{t("filterActive")}</option>
            <option value="INACTIVE">{t("filterInactive")}</option>
          </Select>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-k-muted">
            {t("allPlansLoading")}
          </div>
        )}

        {error && (
          <div
            className="rounded-k-sm bg-k-danger-bg p-4 text-sm text-k-danger-text border border-k-danger-text/30"
            role="alert"
          >
            {error}
          </div>
        )}

        {!loading && !error && plans.length === 0 && (
          <div className="text-center py-8 text-sm text-k-muted">
            {t("allPlansEmpty")}
          </div>
        )}

        {!loading && !error && plans.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-k-border">
              <thead className="bg-k-bg">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("allPlansColPlanName")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("allPlansColProgram")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColModality")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColCost")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColManager")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-k-muted uppercase tracking-wider">
                    {t("plansColStatus")}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-k-surface divide-y divide-k-border">
                {plans.map((plan) => (
                  <tr key={plan.id} className="hover:bg-k-bg">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-k-dark">
                      <Link
                        href={`/programs/${plan.programId ?? ""}/plans/${plan.id}`}
                        className="text-k-subtle hover:text-k-dark hover:underline"
                      >
                        {plan.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {plan.programName ?? "-"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {plan.modality === "HOURS_BASED"
                        ? t("modalityHoursBased")
                        : plan.modality === "CLASSES_PER_WEEK"
                        ? t("modalityClassesPerWeek")
                        : plan.modality}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {formatCost(plan.cost)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-k-muted">
                      {plan.managerName ?? plan.managerId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <ProgramStatusBadge status={plan.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
