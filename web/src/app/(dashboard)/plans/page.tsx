"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useAllPlans } from "@/hooks/usePlans";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";

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
        <h1 className="text-2xl font-bold text-gray-900">{t("allPlansPageTitle")}</h1>
      </div>

      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <label
            htmlFor="statusFilter"
            className="text-sm font-medium text-gray-700"
          >
            {t("filterStatusLabel")}
          </label>
          <select
            id="statusFilter"
            value={statusFilter ?? ""}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">{t("filterAll")}</option>
            <option value="ACTIVE">{t("filterActive")}</option>
            <option value="INACTIVE">{t("filterInactive")}</option>
          </select>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-gray-500">
            {t("allPlansLoading")}
          </div>
        )}

        {error && (
          <div
            className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
            role="alert"
          >
            {error}
          </div>
        )}

        {!loading && !error && plans.length === 0 && (
          <div className="text-center py-8 text-sm text-gray-500">
            {t("allPlansEmpty")}
          </div>
        )}

        {!loading && !error && plans.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("allPlansColPlanName")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("allPlansColProgram")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("plansColModality")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("plansColCost")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("plansColManager")}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t("plansColStatus")}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {plans.map((plan) => (
                  <tr key={plan.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      <Link
                        href={`/programs/${plan.programId ?? ""}/plans/${plan.id}`}
                        className="text-blue-600 hover:text-blue-800 hover:underline"
                      >
                        {plan.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {plan.programName ?? "-"}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {plan.modality === "HOURS_BASED"
                        ? t("modalityHoursBased")
                        : plan.modality === "CLASSES_PER_WEEK"
                        ? t("modalityClassesPerWeek")
                        : plan.modality}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatCost(plan.cost)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
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
