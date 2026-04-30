"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { ProgramClassDetail as ProgramClassDetailType } from "@/lib/types/programClass";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { primaryRole } from "@/lib/types/auth";
import ClassLevelBadge from "./ClassLevelBadge";
import ClassStatusBadge from "./ClassStatusBadge";
import ClassTypeBadge from "./ClassTypeBadge";
import ScheduleDisplay from "./ScheduleDisplay";
import ClassRosterPanel from "@/components/attendance/ClassRosterPanel";

interface ClassDetailProps {
  programId: string;
  programClass: ProgramClassDetailType;
  onChanged?: () => void;
}

export default function ClassDetail({
  programId,
  programClass,
  onChanged,
}: ClassDetailProps) {
  const t = useTranslations("classes");
  const { user } = useAuth();
  const [showConfirm, setShowConfirm] = useState<
    "deactivate" | "reactivate" | "removeProfessor" | null
  >(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(
        `/programs/${programId}/classes/${programClass.id}/${action}`
      );
      const label = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: t("detailClassSuccessFeedback", { action: label }),
      });
      setShowConfirm(null);
      onChanged?.();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : t("detailClassErrorFeedback", { action });
      setFeedback({ type: "error", message });
    } finally {
      setActionLoading(false);
    }
  }

  async function handleRemoveProfessor() {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.delete(
        `/programs/${programId}/classes/${programClass.id}/professor`
      );
      setFeedback({
        type: "success",
        message: t("detailRemoveProfessorSuccess"),
      });
      setShowConfirm(null);
      onChanged?.();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : t("detailRemoveProfessorError");
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
              {programClass.name}
            </h2>
          </div>
          <div className="flex items-center gap-3">
            <ClassStatusBadge status={programClass.status} />
            {programClass.status !== "INACTIVE" && (
              <Link
                href={`/programs/${programId}/classes/${programClass.id}/edit`}
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
              <dt className="text-sm font-medium text-gray-500">{t("detailName")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {programClass.name}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailLevel")}</dt>
              <dd className="mt-1">
                <ClassLevelBadge level={programClass.level} />
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailType")}</dt>
              <dd className="mt-1">
                <ClassTypeBadge type={programClass.type} />
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">
                {t("detailMaxStudents")}
              </dt>
              <dd className="mt-1 text-sm text-gray-900">
                {programClass.maxStudents}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailProfessor")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {programClass.professorName ?? (programClass.professorId ? programClass.professorId : t("detailProfessorUnassigned"))}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailStatus")}</dt>
              <dd className="mt-1">
                <ClassStatusBadge status={programClass.status} />
              </dd>
            </div>

            <div className="sm:col-span-2">
              <dt className="text-sm font-medium text-gray-500">{t("detailSchedule")}</dt>
              <dd className="mt-1">
                <ScheduleDisplay
                  entries={programClass.scheduleEntries}
                  type={programClass.type}
                />
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailCreatedAt")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {formatDate(programClass.createdAt)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailCreatedBy")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {programClass.createdBy}
              </dd>
            </div>

            {programClass.updatedAt && (
              <>
                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    {t("detailLastUpdated")}
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {formatDate(programClass.updatedAt)}
                  </dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-gray-500">
                    {t("detailUpdatedBy")}
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {programClass.updatedBy}
                  </dd>
                </div>
              </>
            )}
          </dl>
        </div>

        {/* Session Roster */}
        <div className="border-t border-gray-200">
          <ClassRosterPanel
            classId={programClass.id}
            classLevel={programClass.level}
            userRole={user ? primaryRole(user.roles) : undefined}
            programId={programId}
            managedProgramIds={[]}
            professorClassIds={[]}
          />
        </div>

        {/* Professor Assignment */}
        <div className="px-6 py-4 border-t border-gray-200">
          <h3 className="text-sm font-medium text-gray-900 mb-3">
            {t("detailProfessorAssignmentTitle")}
          </h3>
          {programClass.professorId ? (
            <>
              {showConfirm !== "removeProfessor" && (
                <button
                  type="button"
                  onClick={() => setShowConfirm("removeProfessor")}
                  className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
                >
                  {t("detailRemoveProfessorButton")}
                </button>
              )}

              {showConfirm === "removeProfessor" && (
                <div className="space-y-3">
                  <p className="text-sm text-gray-700">
                    {t("detailRemoveProfessorConfirm")}
                  </p>
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={handleRemoveProfessor}
                      disabled={actionLoading}
                      className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {actionLoading ? t("detailRemovingButton") : t("detailConfirmRemovalButton")}
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
            </>
          ) : (
            <p className="text-sm text-gray-500">
              {t("detailNoProfessor")}
            </p>
          )}
        </div>

        {/* Actions */}
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
          {programClass.status === "ACTIVE" &&
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
                  onClick={() => setShowConfirm(null)}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {t("detailCancelButton")}
                </button>
              </div>
            </div>
          )}

          {programClass.status === "INACTIVE" && (
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
