"use client";

import { use } from "react";
import Link from "next/link";
import StudentDetail from "@/components/students/StudentDetail";
import { useStudentDetail } from "@/hooks/useStudents";

interface StudentDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function StudentDetailPage({ params }: StudentDetailPageProps) {
  const { id } = use(params);
  const { student, loading, error, refetch } = useStudentDetail(id);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/students" className="hover:text-gray-700 hover:underline">
          Students
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">
          {student ? `${student.firstName} ${student.lastName}` : id}
        </span>
      </nav>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading student details...
        </div>
      )}

      {error && (
        <div
          className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
          role="alert"
        >
          {error}
        </div>
      )}

      {student && <StudentDetail student={student} onStatusChanged={refetch} />}
    </div>
  );
}
