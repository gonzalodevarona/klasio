"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ProfessorDetail as ProfessorDetailType } from "@/lib/types/professor";
import { api, ApiError } from "@/lib/api";
import ProfessorStatusBadge from "./ProfessorStatusBadge";
import ProfessorClassesTable from "./ProfessorClassesTable";

interface ProfessorDetailProps {
  professor: ProfessorDetailType;
  onStatusChanged?: () => void;
}

export default function ProfessorDetail({
  professor,
  onStatusChanged,
}: ProfessorDetailProps) {
  const t = useTranslations("professors");
  const [showConfirm, setShowConfirm] = useState<"deactivate" | "reactivate" | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(`/professors/${professor.id}/${action}`);
      const pastTense = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: t("detailSuccessFeedback", { action: pastTense }),
      });
      setShowConfirm(null);
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
              {professor.firstName} {professor.lastName}
            </h2>
            <p className="text-sm text-gray-500 mt-1">{professor.email}</p>
          </div>
          <div className="flex items-center gap-3">
            <ProfessorStatusBadge status={professor.status} />
            {professor.status !== "DEACTIVATED" && (
              <Link
                href={`/professors/${professor.id}/edit`}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              >
                {t("detailEditButton")}
              </Link>
            )}
          </div>
        </div>

        {/* Details */}
        <div className="px-6 py-5">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailFirstName")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{professor.firstName}</dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailLastName")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{professor.lastName}</dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailEmail")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{professor.email}</dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailPhone")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {professor.phoneNumber || "-"}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailDocType")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{professor.identityDocumentType}</dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailDocNumber")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{professor.identityNumber}</dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailStatus")}</dt>
              <dd className="mt-1">
                <ProfessorStatusBadge status={professor.status} />
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailCreatedAt")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {formatDate(professor.createdAt)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailCreatedBy")}</dt>
              <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                {professor.createdBy}
              </dd>
            </div>

            {professor.updatedAt && (
              <>
                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    {t("detailUpdatedAt")}
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {formatDate(professor.updatedAt)}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    {t("detailUpdatedBy")}
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900 font-mono text-xs">
                    {professor.updatedBy}
                  </dd>
                </div>
              </>
            )}
          </dl>
        </div>

      </div>

      <ProfessorClassesTable professorId={professor.id} />

      <div className="bg-white shadow rounded-lg overflow-hidden">
        {/* Actions */}
        <div className="px-6 py-4 border-gray-200 bg-gray-50">
          {(professor.status === "ACTIVE" || professor.status === "INVITED") &&
            showConfirm !== "deactivate" && (
              <button
                type="button"
                onClick={() => setShowConfirm("deactivate")}
                className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
              >
                {t("detailDeactivateButton")}
              </button>
            )}

          {showConfirm === "deactivate" && (
            <div className="space-y-3">
              <p className="text-sm text-gray-700">
                {t("detailDeactivateConfirm")}
              </p>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => handleStatusAction("deactivate")}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? t("detailDeactivatingButton") : t("detailDeactivateModalTitle")}
                </button>
                <button
                  type="button"
                  onClick={() => setShowConfirm(null)}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {t("detailCancelButton")}
                </button>
              </div>
            </div>
          )}

          {professor.status === "DEACTIVATED" && (
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
      </div>
    </div>
  );
}
