"use client";

import { use } from "react";
import Link from "next/link";
import ClassDetail from "@/components/classes/ClassDetail";
import { useProgramClassDetail } from "@/hooks/useProgramClasses";

interface ClassDetailPageProps {
  params: Promise<{ id: string; classId: string }>;
}

export default function ClassDetailPage({ params }: ClassDetailPageProps) {
  const { id, classId } = use(params);
  const { programClass, loading, error, refetch } = useProgramClassDetail(
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
        <span className="text-gray-900">
          {programClass ? programClass.name : classId}
        </span>
      </nav>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading class details...
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
        <ClassDetail
          programId={id}
          programClass={programClass}
          onChanged={refetch}
        />
      )}
    </div>
  );
}
