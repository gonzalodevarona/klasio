"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { StudentDetail as StudentDetailType, IDENTITY_DOCUMENT_TYPES } from "@/lib/types/student";
import { api, ApiError } from "@/lib/api";
import StudentStatusBadge from "./StudentStatusBadge";
import EnrollmentList from "@/components/enrollments/EnrollmentList";
import EnrollmentForm from "@/components/enrollments/EnrollmentForm";

interface StudentDetailProps {
  student: StudentDetailType;
  onStatusChanged?: () => void;
}

function getDocumentTypeLabel(code: string): string {
  const found = IDENTITY_DOCUMENT_TYPES.find((dt) => dt.value === code);
  return found ? `${found.value} - ${found.label}` : code;
}

export default function StudentDetail({
  student,
  onStatusChanged,
}: StudentDetailProps) {
  const t = useTranslations("students");
  const tCommon = useTranslations("common");

  const [showConfirm, setShowConfirm] = useState<"deactivate" | "reactivate" | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [showEnrollForm, setShowEnrollForm] = useState(false);
  const [enrollmentListKey, setEnrollmentListKey] = useState(0);

  async function handleStatusAction(action: "deactivate" | "reactivate") {
    setActionLoading(true);
    setFeedback(null);

    try {
      await api.post(`/students/${student.id}/${action}`);
      const label = action === "deactivate" ? "deactivated" : "reactivated";
      setFeedback({
        type: "success",
        message: t("detailSuccessFeedback", { action: label }),
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

  function handleEnrollSuccess() {
    setShowEnrollForm(false);
    setEnrollmentListKey((k) => k + 1);
    setFeedback({
      type: "success",
      message: t("detailEnrollSuccessFeedback"),
    });
    onStatusChanged?.();
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

  const hasTutor = student.tutorFirstName || student.tutorLastName;

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
              {student.firstName} {student.lastName}
            </h2>
            <p className="text-sm text-gray-500 mt-1">{student.email}</p>
          </div>
          <div className="flex items-center gap-3">
            <StudentStatusBadge status={student.status} />
            {student.status !== "INACTIVE" && (
              <Link
                href={`/students/${student.id}/edit`}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              >
                {t("detailEditButton")}
              </Link>
            )}
          </div>
        </div>

        {/* Personal Info */}
        <div className="px-6 py-5">
          <h3 className="text-sm font-semibold text-gray-900 uppercase tracking-wider mb-3">{t("detailPersonalInfoLegend")}</h3>
          <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-4">
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailFirstName")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{student.firstName}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailLastName")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{student.lastName}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailEmail")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{student.email}</dd>
            </div>
            {student.phone && (
              <div>
                <dt className="text-sm font-medium text-gray-500">{t("detailPhone")}</dt>
                <dd className="mt-1 text-sm text-gray-900">{student.phone}</dd>
              </div>
            )}
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailDob")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {new Date(student.dateOfBirth + "T00:00:00").toLocaleDateString("en-US", {
                  year: "numeric", month: "long", day: "numeric",
                })}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailAge")}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {student.age} years{student.age < 18 && (
                  <span className="ml-2 inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                    {t("detailMinor")}
                  </span>
                )}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailStatus")}</dt>
              <dd className="mt-1"><StudentStatusBadge status={student.status} /></dd>
            </div>
          </dl>
        </div>

        {/* Identity & Health */}
        <div className="px-6 py-5 border-t border-gray-200">
          <h3 className="text-sm font-semibold text-gray-900 uppercase tracking-wider mb-3">{t("detailIdentityHealthLegend")}</h3>
          <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-4">
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailDocType")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{getDocumentTypeLabel(student.identityDocumentType)}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailDocNumber")}</dt>
              <dd className="mt-1 text-sm text-gray-900 font-mono">{student.identityNumber}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t("detailEps")}</dt>
              <dd className="mt-1 text-sm text-gray-900">{student.eps}</dd>
            </div>
            {student.bloodType && (
              <div>
                <dt className="text-sm font-medium text-gray-500">{t("detailBloodType")}</dt>
                <dd className="mt-1 text-sm text-gray-900 font-semibold">{student.bloodType}</dd>
              </div>
            )}
          </dl>
        </div>

        {/* Tutor Information */}
        {hasTutor && (
          <div className="px-6 py-5 border-t border-gray-200">
            <h3 className="text-sm font-semibold text-gray-900 uppercase tracking-wider mb-3">{t("detailTutorInfoLegend")}</h3>
            <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-4">
              {student.tutorFirstName && (
                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailTutorName")}</dt>
                  <dd className="mt-1 text-sm text-gray-900">
                    {student.tutorFirstName} {student.tutorLastName}
                  </dd>
                </div>
              )}
              {student.tutorRelationship && (
                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailTutorRelationship")}</dt>
                  <dd className="mt-1 text-sm text-gray-900">{student.tutorRelationship}</dd>
                </div>
              )}
              {student.tutorPhone && (
                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailTutorPhone")}</dt>
                  <dd className="mt-1 text-sm text-gray-900">{student.tutorPhone}</dd>
                </div>
              )}
              {student.tutorEmail && (
                <div>
                  <dt className="text-sm font-medium text-gray-500">{t("detailTutorEmail")}</dt>
                  <dd className="mt-1 text-sm text-gray-900">{student.tutorEmail}</dd>
                </div>
              )}
            </dl>
          </div>
        )}

        {/* Metadata */}
        <div className="px-6 py-5 border-t border-gray-200 bg-gray-50">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4 text-xs">
            <div>
              <dt className="font-medium text-gray-500">{t("detailCreatedAt")}</dt>
              <dd className="mt-1 text-gray-900">{formatDate(student.createdAt)}</dd>
            </div>
            <div>
              <dt className="font-medium text-gray-500">{t("detailCreatedBy")}</dt>
              <dd className="mt-1 text-gray-900">{student.createdBy}</dd>
            </div>
            {student.updatedAt && (
              <>
                <div>
                  <dt className="font-medium text-gray-500">{t("detailUpdatedAt")}</dt>
                  <dd className="mt-1 text-gray-900">{formatDate(student.updatedAt)}</dd>
                </div>
                <div>
                  <dt className="font-medium text-gray-500">{t("detailUpdatedBy")}</dt>
                  <dd className="mt-1 text-gray-900">{student.updatedBy}</dd>
                </div>
              </>
            )}
          </dl>
        </div>

        {/* Actions */}
        <div className="px-6 py-4 border-t border-gray-200">
          {student.status === "ACTIVE" && showConfirm !== "deactivate" && (
            <button type="button" onClick={() => setShowConfirm("deactivate")}
              className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2">
              {t("detailDeactivateButton")}
            </button>
          )}

          {showConfirm === "deactivate" && (
            <div className="space-y-3">
              <p className="text-sm text-gray-700">{t("detailDeactivateConfirm")}</p>
              <div className="flex gap-3">
                <button type="button" onClick={() => handleStatusAction("deactivate")} disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed">
                  {actionLoading ? tCommon("deactivating") : t("detailDeactivateModalTitle")}
                </button>
                <button type="button" onClick={() => setShowConfirm(null)} disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                  {t("detailCancelButton")}
                </button>
              </div>
            </div>
          )}

          {student.status === "INACTIVE" && (
            <button type="button" onClick={() => handleStatusAction("reactivate")} disabled={actionLoading}
              className="inline-flex items-center rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed">
              {actionLoading ? tCommon("reactivating") : t("detailReactivateButton")}
            </button>
          )}
        </div>
      </div>

      {/* Enrollments Section */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="px-6 py-5">
          {showEnrollForm ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">{t("detailEnrollTitle")}</h3>
                <button type="button" onClick={() => setShowEnrollForm(false)}
                  className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50">
                  {t("detailCancelButton")}
                </button>
              </div>
              <EnrollmentForm studentId={student.id} onSuccess={handleEnrollSuccess} />
            </div>
          ) : (
            <EnrollmentList
              key={enrollmentListKey}
              studentId={student.id}
              onEnrollClick={() => setShowEnrollForm(true)}
            />
          )}
        </div>
      </div>

      {/* Memberships Section */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="px-6 py-5 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">{t("detailMembershipsTitle")}</h3>
            <p className="text-sm text-gray-500 mt-1">{t("detailMembershipsDesc")}</p>
          </div>
          <Link
            href={`/students/${student.id}/memberships`}
            className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t("detailViewMembershipsLink")}
          </Link>
        </div>
      </div>
    </div>
  );
}
