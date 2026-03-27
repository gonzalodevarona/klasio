"use client";

import { use } from "react";
import Link from "next/link";
import StudentForm from "@/components/students/StudentForm";
import { useStudentDetail } from "@/hooks/useStudents";

interface EditStudentPageProps {
  params: Promise<{ id: string }>;
}

export default function EditStudentPage({ params }: EditStudentPageProps) {
  const { id } = use(params);
  const { student, loading, error } = useStudentDetail(id);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/students" className="hover:text-gray-700 hover:underline">
          Students
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/students/${id}`}
          className="hover:text-gray-700 hover:underline"
        >
          {student ? `${student.firstName} ${student.lastName}` : id}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Edit</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">Edit Student</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading student...
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

      {student && <StudentForm student={student} />}
    </div>
  );
}
