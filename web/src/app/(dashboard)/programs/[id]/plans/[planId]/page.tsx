"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useProgramDetail } from "@/hooks/usePrograms";
import { useProgramPlanDetail } from "@/hooks/useProgramPlans";
import { api, ApiError } from "@/lib/api";
import ProgramStatusBadge from "@/components/programs/ProgramStatusBadge";

const DAY_LABELS: Record<string, string> = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
  SUNDAY: "Sunday",
};

interface PlanDetailPageProps {
  params: Promise<{ id: string; planId: string }>;
}

export default function PlanDetailPage({ params }: PlanDetailPageProps) {
  const { id, planId } = use(params);
  const { program } = useProgramDetail(id);
  const { plan, loading, error, refetch } = useProgramPlanDetail(id, planId);

  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(`/programs/${id}/plans/${planId}/${action}`);
      const label = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: `Plan has been ${label} successfully.`,
      });
      refetch();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : `Failed to ${action} plan.`;
      setFeedback({ type: "error", message });
    } finally {
      setActionLoading(false);
    }
  }

  function formatCost(cost: number): string {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "COP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(cost);
  }

  function formatDate(dateString: string | null): string {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          Programs
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/programs/${id}`}
          className="hover:text-gray-700 hover:underline"
        >
          {program?.name ?? id}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{plan?.name ?? "Plan"}</span>
      </nav>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading plan details...
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

      {feedback && (
        <div
          className={`rounded-md p-4 text-sm border mb-4 ${
            feedback.type === "success"
              ? "bg-green-50 text-green-700 border-green-200"
              : "bg-red-50 text-red-700 border-red-200"
          }`}
          role="alert"
        >
          {feedback.message}
        </div>
      )}

      {plan && (
        <div className="space-y-6">
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-200 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-gray-900">
                  {plan.name}
                </h2>
                <p className="text-sm text-gray-500 mt-1">
                  {formatCost(plan.cost)}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <ProgramStatusBadge status={plan.status} />
                {plan.status === "ACTIVE" && (
                  <Link
                    href={`/programs/${id}/plans/${planId}/edit`}
                    className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50"
                  >
                    Edit
                  </Link>
                )}
              </div>
            </div>

            <div className="px-6 py-5">
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
                <div>
                  <dt className="text-sm font-medium text-gray-500">Modality</dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {plan.modality === "HOURS_BASED"
                      ? "Hours Based"
                      : plan.modality === "CLASSES_PER_WEEK"
                        ? "Classes per Week"
                        : plan.modality}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">Manager ID</dt>
                  <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                    {plan.managerId}
                  </dd>
                </div>

                {plan.hours != null && (
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Hours</dt>
                    <dd className="mt-1 text-sm text-gray-900">
                      {plan.hours}
                    </dd>
                  </div>
                )}

                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    Created At
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {formatDate(plan.createdAt)}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    Created By
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                    {plan.createdBy}
                  </dd>
                </div>

                {plan.updatedAt && (
                  <>
                    <div>
                      <dt className="text-sm font-medium text-gray-500">
                        Last Updated
                      </dt>
                      <dd className="mt-1 text-sm text-gray-900">
                        {formatDate(plan.updatedAt)}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sm font-medium text-gray-500">
                        Updated By
                      </dt>
                      <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                        {plan.updatedBy}
                      </dd>
                    </div>
                  </>
                )}
              </dl>
            </div>

            {plan.scheduleEntries.length > 0 && (
              <div className="px-6 py-5 border-t border-gray-200">
                <h3 className="text-sm font-medium text-gray-500 mb-3">
                  Schedule
                </h3>
                <div className="space-y-2">
                  {plan.scheduleEntries.map((entry, index) => (
                    <div
                      key={index}
                      className="flex items-center gap-3 text-sm text-gray-900 bg-gray-50 rounded-md px-3 py-2"
                    >
                      <span className="font-medium w-24">
                        {DAY_LABELS[entry.dayOfWeek] ?? entry.dayOfWeek}
                      </span>
                      <span>
                        {entry.startTime} - {entry.endTime}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
              {plan.status === "ACTIVE" && (
                <button
                  type="button"
                  onClick={() => handleStatusAction("deactivate")}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? "Deactivating..." : "Deactivate Plan"}
                </button>
              )}
              {plan.status === "INACTIVE" && (
                <button
                  type="button"
                  onClick={() => handleStatusAction("reactivate")}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? "Reactivating..." : "Reactivate Plan"}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
