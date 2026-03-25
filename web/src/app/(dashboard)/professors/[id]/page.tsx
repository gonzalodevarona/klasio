"use client";

import { use } from "react";
import Link from "next/link";
import ProfessorDetail from "@/components/professors/ProfessorDetail";
import { useProfessorDetail } from "@/hooks/useProfessors";

interface ProfessorDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ProfessorDetailPage({ params }: ProfessorDetailPageProps) {
  const { id } = use(params);
  const { professor, loading, error, refetch } = useProfessorDetail(id);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/professors" className="hover:text-gray-700 hover:underline">
          Professors
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">
          {professor ? `${professor.firstName} ${professor.lastName}` : id}
        </span>
      </nav>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading professor details...
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

      {professor && <ProfessorDetail professor={professor} onStatusChanged={refetch} />}
    </div>
  );
}
