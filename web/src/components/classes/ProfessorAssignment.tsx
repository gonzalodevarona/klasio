"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";

interface ProfessorAssignmentProps {
  programId: string;
  classId: string;
  professorId?: string;
  onChanged?: () => void;
}

export default function ProfessorAssignment({
  programId,
  classId,
  professorId,
  onChanged,
}: ProfessorAssignmentProps) {
  const [inputProfessorId, setInputProfessorId] = useState("");
  const [showAssignConfirm, setShowAssignConfirm] = useState(false);
  const [showRemoveConfirm, setShowRemoveConfirm] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  async function handleAssignProfessor() {
    if (!inputProfessorId.trim()) return;

    setActionLoading(true);
    setFeedback(null);

    try {
      await api.put(
        `/programs/${programId}/classes/${classId}/professor`,
        { professorId: inputProfessorId.trim() }
      );
      setFeedback({
        type: "success",
        message: "Professor has been assigned successfully.",
      });
      setShowAssignConfirm(false);
      setInputProfessorId("");
      onChanged?.();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : "Failed to assign professor. Please try again.";
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
        `/programs/${programId}/classes/${classId}/professor`
      );
      setFeedback({
        type: "success",
        message: "Professor has been removed successfully.",
      });
      setShowRemoveConfirm(false);
      onChanged?.();
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : "Failed to remove professor. Please try again.";
      setFeedback({ type: "error", message });
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="bg-white shadow rounded-lg overflow-hidden">
      <div className="px-6 py-5 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900">
          Professor Assignment
        </h3>
      </div>

      <div className="px-6 py-5 space-y-4">
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

        <div>
          <dt className="text-sm font-medium text-gray-500">
            Current Professor
          </dt>
          <dd className="mt-1 text-sm text-gray-900 font-mono">
            {professorId ?? "Unassigned"}
          </dd>
        </div>

        {/* Assign Professor */}
        <div className="space-y-3">
          <div className="flex items-end gap-3">
            <div className="flex-1">
              <label
                htmlFor="professorIdInput"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Professor ID
              </label>
              <input
                id="professorIdInput"
                type="text"
                value={inputProfessorId}
                onChange={(e) => setInputProfessorId(e.target.value)}
                className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="UUID of the professor"
              />
            </div>
            {!showAssignConfirm && (
              <button
                type="button"
                onClick={() => setShowAssignConfirm(true)}
                disabled={!inputProfessorId.trim()}
                className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Assign Professor
              </button>
            )}
          </div>

          {showAssignConfirm && (
            <div className="space-y-3">
              <p className="text-sm text-gray-700">
                Are you sure you want to assign this professor to the class?
              </p>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleAssignProfessor}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? "Assigning..." : "Confirm Assignment"}
                </button>
                <button
                  type="button"
                  onClick={() => setShowAssignConfirm(false)}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Remove Professor Action */}
      {professorId && (
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
          {!showRemoveConfirm ? (
            <button
              type="button"
              onClick={() => setShowRemoveConfirm(true)}
              className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
            >
              Remove Professor
            </button>
          ) : (
            <div className="space-y-3">
              <p className="text-sm text-gray-700">
                Are you sure you want to remove the professor from this class?
              </p>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleRemoveProfessor}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? "Removing..." : "Confirm Removal"}
                </button>
                <button
                  type="button"
                  onClick={() => setShowRemoveConfirm(false)}
                  disabled={actionLoading}
                  className="inline-flex items-center rounded-md bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
