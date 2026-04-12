"use client";

import { useState } from "react";
import { useMyEnrollments } from "@/hooks/useMyEnrollments";
import LevelBadge from "@/components/enrollments/LevelBadge";

const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "", label: "All" },
];

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentEnrollmentsPage() {
  const [statusFilter, setStatusFilter] = useState<string>("ACTIVE");
  const { enrollments, loading, error } = useMyEnrollments(statusFilter || undefined);

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Enrollments</h1>
          <p className="mt-1 text-sm text-gray-500">
            Programs you are enrolled in and your current level.
          </p>
        </div>
        <div className="flex items-center gap-2 pt-1">
          <label htmlFor="statusFilter" className="text-sm text-gray-600 whitespace-nowrap">
            Status:
          </label>
          <select
            id="statusFilter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-md border border-gray-300 px-2 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-gray-500">Loading…</p>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {!loading && !error && enrollments.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-400">
          No {statusFilter ? statusFilter.toLowerCase() : ""} enrollments found.
        </p>
      )}

      {enrollments.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Program
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Level
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Enrolled
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {enrollments.map((e) => (
                <tr key={e.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">
                    {e.programName}
                  </td>
                  <td className="px-4 py-3">
                    <LevelBadge level={e.level} />
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                        e.status === "ACTIVE"
                          ? "bg-green-100 text-green-800"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {e.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {formatDate(e.enrollmentDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
