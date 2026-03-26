"use client";

import { useState } from "react";
import Link from "next/link";
import { ClassLevel, ClassStatus } from "@/lib/types/programClass";
import { useProgramClasses } from "@/hooks/useProgramClasses";
import ClassLevelBadge from "./ClassLevelBadge";
import ClassTypeBadge from "./ClassTypeBadge";
import ClassStatusBadge from "./ClassStatusBadge";

interface ClassListProps {
  programId: string;
}

export default function ClassList({ programId }: ClassListProps) {
  const [page, setPage] = useState(0);
  const [levelFilter, setLevelFilter] = useState<ClassLevel | undefined>(
    undefined
  );
  const [statusFilter, setStatusFilter] = useState<ClassStatus | undefined>(
    undefined
  );
  const SIZE = 20;

  const { classes, totalPages, totalElements, loading, error } = useProgramClasses(
    programId,
    page,
    SIZE,
    levelFilter,
    statusFilter
  );

  function handleLevelChange(value: string) {
    setLevelFilter(value === "" ? undefined : (value as ClassLevel));
    setPage(0);
  }

  function handleStatusChange(value: string) {
    setStatusFilter(value === "" ? undefined : (value as ClassStatus));
    setPage(0);
  }

  function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  if (error) {
    return (
      <div
        className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
        role="alert"
      >
        {error}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex items-center gap-3">
        <label htmlFor="levelFilter" className="text-sm font-medium text-gray-700">
          Level:
        </label>
        <select
          id="levelFilter"
          value={levelFilter ?? ""}
          onChange={(e) => handleLevelChange(e.target.value)}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All</option>
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCED">Advanced</option>
        </select>

        <label htmlFor="statusFilter" className="text-sm font-medium text-gray-700">
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

      {loading ? (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading classes...
        </div>
      ) : classes.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-sm text-gray-500">No classes found</p>
          <Link
            href={`/programs/${programId}/classes/new`}
            className="mt-2 inline-flex items-center text-sm text-blue-600 hover:text-blue-700"
          >
            Create your first class
          </Link>
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Level
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Max Students
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Created
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {classes.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      <Link
                        href={`/programs/${programId}/classes/${c.id}`}
                        className="hover:text-blue-600 hover:underline"
                      >
                        {c.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <ClassLevelBadge level={c.level} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <ClassTypeBadge type={c.type} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {c.maxStudents}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <ClassStatusBadge status={c.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(c.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between border-t border-gray-200 pt-4">
            <p className="text-sm text-gray-700">
              Page {page + 1} of {totalPages} ({totalElements} total)
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1}
                className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
