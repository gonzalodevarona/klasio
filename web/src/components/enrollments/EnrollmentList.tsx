"use client";

import React, { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { EnrollmentDetail, Level } from "@/lib/types/enrollment";
import { useStudentEnrollments } from "@/hooks/useStudentEnrollments";
import LevelBadge from "./LevelBadge";
import LevelHistoryList from "./LevelHistoryList";

interface EnrollmentListProps {
  studentId: string;
  onEnrollClick: () => void;
  onRefresh?: () => void;
}

const LEVELS: Level[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];
const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "", label: "All" },
];

export default function EnrollmentList({
  studentId,
  onEnrollClick,
  onRefresh,
}: EnrollmentListProps) {
  const [statusFilter, setStatusFilter] = useState<string>("ACTIVE");
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [promoteEnrollmentId, setPromoteEnrollmentId] = useState<string | null>(null);
  const [promoteCurrentLevel, setPromoteCurrentLevel] = useState<Level | null>(null);
  const [promoteTargetLevel, setPromoteTargetLevel] = useState<Level | "">("");
  const [processing, setProcessing] = useState<string | null>(null);

  const { enrollments, loading, error, refetch } = useStudentEnrollments(studentId, statusFilter);

  function toggleExpand(id: string) {
    setExpandedId((current) => (current === id ? null : id));
  }

  function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  function handleStatusFilterChange(value: string) {
    setStatusFilter(value);
    setExpandedId(null);
  }

  async function handleUnenroll(enrollmentId: string, e: React.MouseEvent) {
    e.stopPropagation();
    setActionError(null);
    setProcessing(enrollmentId);
    try {
      await api.post<EnrollmentDetail>(`/enrollments/${enrollmentId}/unenroll`, {});
      refetch();
      onRefresh?.();
    } catch (err) {
      if (err instanceof ApiError) {
        setActionError(err.message);
      } else {
        setActionError("Failed to unenroll. Please try again.");
      }
    } finally {
      setProcessing(null);
    }
  }

  function openPromoteModal(enrollmentId: string, currentLevel: Level, e: React.MouseEvent) {
    e.stopPropagation();
    setActionError(null);
    setPromoteEnrollmentId(enrollmentId);
    setPromoteCurrentLevel(currentLevel);
    setPromoteTargetLevel("");
  }

  function closePromoteModal() {
    setPromoteEnrollmentId(null);
    setPromoteCurrentLevel(null);
    setPromoteTargetLevel("");
    setActionError(null);
  }

  async function handlePromoteSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!promoteEnrollmentId || !promoteTargetLevel) return;

    setActionError(null);
    setProcessing(promoteEnrollmentId);
    try {
      await api.post<EnrollmentDetail>(`/enrollments/${promoteEnrollmentId}/promote`, { level: promoteTargetLevel });
      closePromoteModal();
      refetch();
      onRefresh?.();
    } catch (err) {
      if (err instanceof ApiError) {
        setActionError(err.message);
      } else {
        setActionError("Failed to promote. Please try again.");
      }
    } finally {
      setProcessing(null);
    }
  }

  const availableLevels = promoteCurrentLevel
    ? LEVELS.filter((l) => l !== promoteCurrentLevel)
    : LEVELS;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <h3 className="text-lg font-semibold text-gray-900">Enrollments</h3>
        <div className="flex items-center gap-3">
          {/* Status filter */}
          <div className="flex items-center gap-2">
            <label htmlFor="enrollmentStatusFilter" className="text-sm text-gray-600 whitespace-nowrap">
              Status:
            </label>
            <select
              id="enrollmentStatusFilter"
              value={statusFilter}
              onChange={(e) => handleStatusFilterChange(e.target.value)}
              className="rounded-md border border-gray-300 px-2 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <button
            type="button"
            onClick={onEnrollClick}
            className="inline-flex items-center rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            Enroll in Program
          </button>
        </div>
      </div>

      {actionError && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700 border border-red-200" role="alert">
          {actionError}
        </div>
      )}

      {loading ? (
        <div className="py-6 text-center text-sm text-gray-500">Loading enrollments...</div>
      ) : error ? (
        <div className="py-6 text-center text-sm text-red-600">{error}</div>
      ) : enrollments.length === 0 ? (
        <div className="text-center py-6">
          <p className="text-sm text-gray-500">
            No {statusFilter ? statusFilter.toLowerCase() : ""} enrollments found.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Program
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Level
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Enrollment Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {enrollments.map((enrollment) => (
                <React.Fragment key={enrollment.id}>
                  <tr
                    onClick={() => toggleExpand(enrollment.id)}
                    className="hover:bg-gray-50 cursor-pointer"
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {enrollment.programName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <LevelBadge level={enrollment.level as Level} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(enrollment.enrollmentDate)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                        enrollment.status === "ACTIVE"
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-600"
                      }`}>
                        {enrollment.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm" onClick={(e) => e.stopPropagation()}>
                      {enrollment.status === "ACTIVE" && (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            disabled={processing === enrollment.id}
                            onClick={(e) => openPromoteModal(enrollment.id, enrollment.level as Level, e)}
                            className="rounded px-2 py-1 text-xs font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 disabled:opacity-50"
                          >
                            Promote
                          </button>
                          <button
                            type="button"
                            disabled={processing === enrollment.id}
                            onClick={(e) => handleUnenroll(enrollment.id, e)}
                            className="rounded px-2 py-1 text-xs font-medium text-red-700 bg-red-50 hover:bg-red-100 disabled:opacity-50"
                          >
                            {processing === enrollment.id ? "..." : "Unenroll"}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                  {expandedId === enrollment.id && (
                    <tr>
                      <td colSpan={5} className="px-6 py-4 bg-gray-50">
                        <LevelHistoryList enrollmentId={enrollment.id} />
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Promote modal */}
      {promoteEnrollmentId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
            <h4 className="text-base font-semibold text-gray-900 mb-4">Promote Student</h4>
            <form onSubmit={handlePromoteSubmit} className="space-y-4" noValidate>
              <div>
                <p className="text-sm text-gray-600 mb-3">
                  Current level: <LevelBadge level={promoteCurrentLevel!} />
                </p>
                <label htmlFor="targetLevel" className="block text-sm font-medium text-gray-700 mb-1">
                  New level <span className="text-red-500">*</span>
                </label>
                <select
                  id="targetLevel"
                  value={promoteTargetLevel}
                  onChange={(e) => setPromoteTargetLevel(e.target.value as Level)}
                  className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                >
                  <option value="">Select a level</option>
                  {availableLevels.map((l) => (
                    <option key={l} value={l}>
                      {l.charAt(0) + l.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
              </div>
              {actionError && (
                <div className="rounded-md bg-red-50 p-3 text-sm text-red-700 border border-red-200">
                  {actionError}
                </div>
              )}
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={closePromoteModal}
                  className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!promoteTargetLevel || processing !== null}
                  className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {processing ? "Promoting..." : "Promote"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
