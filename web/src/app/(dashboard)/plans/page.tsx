"use client";

import { useState } from "react";
import Link from "next/link";
import { useAllPlans } from "@/hooks/usePlans";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";

const MODALITY_LABELS: Record<string, string> = {
  HOURS_BASED: "Hours Based",
  CLASSES_PER_WEEK: "Classes per Week",
};

function formatCost(cost: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(cost);
}

export default function PlansPage() {
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
        <h1 className="text-2xl font-bold text-gray-900">All Plans</h1>
      </div>

      <div className="space-y-4">
        {/* Filter */}
        <div className="flex items-center gap-3">
          <label
            htmlFor="statusFilter"
            className="text-sm font-medium text-gray-700"
          >
            Status:
          </label>
          <select
            id="statusFilter"
            value={statusFilter ?? ""}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        {loading && (
          <div className="text-center py-8 text-sm text-gray-500">
            Loading plans...
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
            No plans found
          </div>
        )}

        {!loading && !error && plans.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Plan Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Program
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Modality
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Cost
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Manager
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
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
                      {MODALITY_LABELS[plan.modality] ?? plan.modality}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatCost(plan.cost)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono text-xs">
                      {plan.managerId}
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
