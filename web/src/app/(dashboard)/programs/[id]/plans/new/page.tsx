"use client";

import { use } from "react";
import Link from "next/link";
import ProgramPlanForm from "@/components/programs/ProgramPlanForm";
import { useProgramDetail } from "@/hooks/usePrograms";

interface NewPlanPageProps {
  params: Promise<{ id: string }>;
}

export default function NewPlanPage({ params }: NewPlanPageProps) {
  const { id } = use(params);
  const { program, loading, error } = useProgramDetail(id);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          Programs
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/programs/${id}`}
          className="hover:text-gray-700 hover:underline"
        >
          {program?.name ?? id}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">New Plan</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">Add New Plan</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading program...
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

      {program && (
        <div className="bg-white shadow rounded-lg p-6">
          <ProgramPlanForm programId={id} />
        </div>
      )}
    </div>
  );
}
