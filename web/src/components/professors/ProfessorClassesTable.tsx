"use client";

import Link from "next/link";
import { useProfessorClasses } from "@/hooks/useProgramClasses";

const LEVEL_LABELS: Record<string, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-700",
  INACTIVE: "bg-gray-100 text-gray-600",
};

interface ProfessorClassesTableProps {
  professorId: string;
}

export default function ProfessorClassesTable({ professorId }: ProfessorClassesTableProps) {
  const { classes, totalElements, loading, error } = useProfessorClasses(professorId, 0, 50);

  return (
    <div className="bg-white shadow rounded-lg overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200">
        <h3 className="text-base font-semibold text-gray-900">
          Assigned Classes
          {!loading && (
            <span className="ml-2 text-sm font-normal text-gray-500">
              ({totalElements})
            </span>
          )}
        </h3>
      </div>

      {loading && (
        <div className="px-6 py-8 text-center text-sm text-gray-500">
          Loading classes...
        </div>
      )}

      {error && (
        <div className="px-6 py-4 text-sm text-red-700 bg-red-50 border-t border-red-100">
          {error}
        </div>
      )}

      {!loading && !error && classes.length === 0 && (
        <div className="px-6 py-8 text-center text-sm text-gray-500">
          No classes assigned to this professor.
        </div>
      )}

      {!loading && !error && classes.length > 0 && (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Class Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Program
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Level
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Capacity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {classes.map((cls) => (
                <tr key={cls.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900">
                    <Link
                      href={`/programs/${cls.programId}/classes/${cls.id}`}
                      className="text-blue-600 hover:underline"
                    >
                      {cls.name}
                    </Link>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {cls.programName ?? "-"}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {LEVEL_LABELS[cls.level] ?? cls.level}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {cls.maxStudents}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        STATUS_STYLES[cls.status] ?? "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {cls.status.charAt(0) + cls.status.slice(1).toLowerCase()}
                    </span>
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
