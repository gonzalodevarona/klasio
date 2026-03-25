"use client";

import { use } from "react";
import Link from "next/link";
import ProfessorForm from "@/components/professors/ProfessorForm";
import { useProfessorDetail } from "@/hooks/useProfessors";

interface EditProfessorPageProps {
  params: Promise<{ id: string }>;
}

export default function EditProfessorPage({ params }: EditProfessorPageProps) {
  const { id } = use(params);
  const { professor, loading, error } = useProfessorDetail(id);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/professors" className="hover:text-gray-700 hover:underline">
          Professors
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/professors/${id}`}
          className="hover:text-gray-700 hover:underline"
        >
          {professor ? `${professor.firstName} ${professor.lastName}` : id}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Edit</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">Edit Professor</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading professor...
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

      {professor && <ProfessorForm professor={professor} />}
    </div>
  );
}
