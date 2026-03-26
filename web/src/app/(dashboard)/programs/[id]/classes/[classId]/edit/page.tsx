"use client";

import { use } from "react";
import Link from "next/link";
import ClassForm from "@/components/classes/ClassForm";
import { useProgramClassDetail } from "@/hooks/useProgramClasses";

interface EditClassPageProps {
  params: Promise<{ id: string; classId: string }>;
}

export default function EditClassPage({ params }: EditClassPageProps) {
  const { id, classId } = use(params);
  const { programClass, loading, error } = useProgramClassDetail(
    id,
    classId
  );

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          Programs
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/programs/${id}/classes`}
          className="hover:text-gray-700 hover:underline"
        >
          Classes
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/programs/${id}/classes/${classId}`}
          className="hover:text-gray-700 hover:underline"
        >
          {programClass ? programClass.name : classId}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Edit</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">Edit Class</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading class...
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

      {programClass && (
        <ClassForm programId={id} programClass={programClass} />
      )}
    </div>
  );
}
