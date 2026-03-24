"use client";

import { use } from "react";
import Link from "next/link";
import ProgramDetail from "@/components/programs/ProgramDetail";
import { useProgramDetail } from "@/hooks/usePrograms";

interface ProgramDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ProgramDetailPage({ params }: ProgramDetailPageProps) {
  const { id } = use(params);
  const { program, loading, error, refetch } = useProgramDetail(id);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          Programs
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{program?.name ?? id}</span>
      </nav>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading program details...
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

      {program && <ProgramDetail program={program} onStatusChanged={refetch} />}
    </div>
  );
}
