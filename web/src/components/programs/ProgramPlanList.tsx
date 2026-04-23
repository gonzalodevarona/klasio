"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { ProgramPlanSummary } from "@/lib/types/programPlan";
import ProgramStatusBadge from "./ProgramStatusBadge";

interface ProgramPlanListProps {
  programId: string;
  plans: ProgramPlanSummary[];
  loading: boolean;
  error: string | null;
}

function formatCost(cost: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(cost);
}

export default function ProgramPlanList({
  programId,
  plans,
  loading,
  error,
}: ProgramPlanListProps) {
  const t = useTranslations("programs");

  if (loading) {
    return (
      <div className="animate-pulse space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-12 bg-gray-100 rounded" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200">
        {error}
      </div>
    );
  }

  if (plans.length === 0) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500 text-sm">
          {t("plansEmpty")}
        </p>
        <Link
          href={`/programs/${programId}/plans/new`}
          className="mt-3 inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700"
        >
          {t("plansAddFirstButton")}
        </Link>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColName")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColModality")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColCost")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColHours")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColManager")}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColStatus")}
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
              {t("plansColActions")}
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {plans.map((plan) => (
            <tr key={plan.id} className="hover:bg-gray-50">
              <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">
                <Link
                  href={`/programs/${programId}/plans/${plan.id}`}
                  className="text-blue-600 hover:text-blue-800"
                >
                  {plan.name}
                </Link>
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {plan.modality === "HOURS_BASED"
                  ? t("modalityHoursBased")
                  : plan.modality === "CLASSES_PER_WEEK"
                  ? t("modalityClassesPerWeek")
                  : plan.modality}
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {formatCost(plan.cost)}
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {plan.hours != null ? `${plan.hours}h` : "-"}
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {plan.managerName ?? plan.managerId}
              </td>
              <td className="px-4 py-3 whitespace-nowrap">
                <ProgramStatusBadge status={plan.status} />
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-right text-sm">
                <Link
                  href={`/programs/${programId}/plans/${plan.id}`}
                  className="text-blue-600 hover:text-blue-800"
                >
                  {t("plansView")}
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
