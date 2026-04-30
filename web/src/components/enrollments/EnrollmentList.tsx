"use client";

import React, { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { EnrollmentDetail, Level } from "@/lib/types/enrollment";
import { useStudentEnrollments } from "@/hooks/useStudentEnrollments";
import LevelBadge from "./LevelBadge";
import LevelHistoryList from "./LevelHistoryList";
import { Table, Thead, Th, Tr, Td, Select, Button } from "@/components/ui";

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
          <Select
            label="Status:"
            value={statusFilter}
            onChange={(e) => handleStatusFilterChange(e.target.value)}
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
          <Button variant="primary" size="sm" onClick={onEnrollClick}>
            Enroll in Program
          </Button>
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
        <Table>
          <Thead>
            <tr>
              <Th>Program</Th>
              <Th>Level</Th>
              <Th>Enrollment Date</Th>
              <Th>Status</Th>
              <Th right>Actions</Th>
            </tr>
          </Thead>
          <tbody>
            {enrollments.map((enrollment) => (
              <React.Fragment key={enrollment.id}>
                <Tr onClick={() => toggleExpand(enrollment.id)}>
                  <Td bold>{enrollment.programName}</Td>
                  <Td>
                    <LevelBadge level={enrollment.level as Level} />
                  </Td>
                  <Td muted>{formatDate(enrollment.enrollmentDate)}</Td>
                  <Td>
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                      enrollment.status === "ACTIVE"
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-600"
                    }`}>
                      {enrollment.status}
                    </span>
                  </Td>
                  <Td right onClick={(e) => e.stopPropagation()}>
                    {enrollment.status === "ACTIVE" && (
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={processing === enrollment.id}
                          onClick={(e) => openPromoteModal(enrollment.id, enrollment.level as Level, e)}
                        >
                          Promote
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={processing === enrollment.id}
                          onClick={(e) => handleUnenroll(enrollment.id, e)}
                        >
                          {processing === enrollment.id ? "..." : "Unenroll"}
                        </Button>
                      </div>
                    )}
                  </Td>
                </Tr>
                {expandedId === enrollment.id && (
                  <Tr className="bg-k-bg">
                    <Td colSpan={5}>
                      <LevelHistoryList enrollmentId={enrollment.id} />
                    </Td>
                  </Tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </Table>
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
