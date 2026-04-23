"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ProgramDetail as ProgramDetailType } from "@/lib/types/program";
import { api, ApiError } from "@/lib/api";
import ProgramStatusBadge from "./ProgramStatusBadge";
import ProgramPlanList from "./ProgramPlanList";
import { useProgramPlans } from "@/hooks/useProgramPlans";

interface ProgramDetailProps {
  program: ProgramDetailType;
  onStatusChanged?: () => void;
}

type Tab = "details" | "plans";

export default function ProgramDetail({
  program,
  onStatusChanged,
}: ProgramDetailProps) {
  const t = useTranslations("programs");
  const { plans, loading: plansLoading, error: plansError } = useProgramPlans(program.id);
  const [activeTab, setActiveTab] = useState<Tab>("details");
  const [showConfirm, setShowConfirm] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(`/programs/${program.id}/${action}`);
      const label = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: t("detailSuccessFeedback", { action: label }),
      });
      setShowConfirm(false);
      onStatusChanged?.();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : t("detailErrorFeedback", { action });
      setFeedback({ type: "error", message });
    } finally {
      setActionLoading(false);
    }
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
    <div className="space-y-6">
      {feedback && (
        <div
          className={`rounded-md p-4 text-sm border ${
            feedback.type === "success"
              ? "bg-green-50 text-green-700 border-green-200"
              : "bg-red-50 text-red-700 border-red-200"
          }`}
          role="alert"
        >
          {feedback.message}
        </div>
      )}

      <div className="bg-white shadow rounded-lg overflow-hidden">
        {/* Header */}
        <div className="px-6 py-5 border-b border-gray-200 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">
              {program.name}
            </h2>
          </div>
          <div className="flex items-center gap-3">
            <ProgramStatusBadge status={program.status} />
            {program.status === "ACTIVE" && (
              <Link
                href={`/programs/${program.id}/edit`}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              >
                {t("detailEditButton")}
              </Link>
            )}
          </div>
        </div>

        {/* Tab Bar */}
        <div className="border-b border-gray-200">
          <nav className="flex -mb-px px-6" aria-label="Tabs">
            <button
              type="button"
              onClick={() => setActiveTab("details")}
              className={`py-3 px-4 text-sm font-medium border-b-2 ${
                activeTab === "details"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
              }`}
            >
              {t("detailTabDetails")}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("plans")}
              className={`py-3 px-4 text-sm font-medium border-b-2 ${
                activeTab === "plans"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
              }`}
            >
              {t("detailTabPlans")}
            </button>
            <Link
              href={`/programs/${program.id}/classes`}
              className="py-3 px-4 text-sm font-medium border-b-2 border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
            >
              {t("detailTabClasses")}
            </Link>
          </nav>
        </div>

        {/* Details Tab */}
        {activeTab === "details" && (
          <>
            <div className="px-6 py-5">
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailName")}</dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {program.name}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailStatus")}</dt>
                  <dd className="mt-1">
                    <ProgramStatusBadge status={program.status} />
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailCreatedAt")}</dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {formatDate(program.createdAt)}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailCreatedBy")}</dt>
                  <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                    {program.createdBy}
                  </dd>
                </div>

                {program.updatedAt && (
                  <>
                    <div>
                      <dt className="text-sm font-medium text-gray-500">
                        {t("detailLastUpdated")}
                      </dt>
                      <dd className="mt-1 text-sm text-gray-900">
                        {formatDate(program.updatedAt)}
                      </dd>
                    </div>

                    <div>
                      <dt className="text-sm font-medium text-gray-500">
                        {t("detailUpdatedBy")}
                      </dt>
                      <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                        {program.updatedBy}
                      </dd>
                    </div>
                  </>
                )}
              </dl>
            </div>

            {/* Actions */}
            <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
              {program.status === "ACTIVE" && !showConfirm && (
                <button
                  type="button"
                  onClick={() => setShowConfirm(true)}
                  className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
                >
                  {t("detailDeactivateButton")}
                </button>
              )}

              {program.status === "ACTIVE" && showConfirm && (
                <div className="space-y-3">
                  <p className="text-sm text-gray-700">
                    {t("detailConfirmDeactivate")}
                  </p>
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={() => handleStatusAction("deactivate")}
                      disabled={actionLoading}
                      className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {actionLoading ? t("detailDeactivatingButton") : t("detailConfirmDeactivateButton")}
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowConfirm(false)}
                      disabled={actionLoading}
                      className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {t("detailCancelButton")}
                    </button>
                  </div>
                </div>
              )}

              {program.status === "INACTIVE" && (
                <button
                  type="button"
                  onClick={() => handleStatusAction("reactivate")}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? t("detailReactivatingButton") : t("detailReactivateButton")}
                </button>
              )}
            </div>
          </>
        )}

        {/* Plans Tab */}
        {activeTab === "plans" && (
          <div className="px-6 py-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">{t("plansTitle")}</h3>
              {program.status === "ACTIVE" && (
                <Link
                  href={`/programs/${program.id}/plans/new`}
                  className="inline-flex items-center rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700"
                >
                  {t("plansAddButton")}
                </Link>
              )}
            </div>
            <ProgramPlanList
              programId={program.id}
              plans={plans}
              loading={plansLoading}
              error={plansError}
            />
          </div>
        )}
      </div>
    </div>
  );
}
