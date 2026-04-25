"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProfessorDetail from "@/components/professors/ProfessorDetail";
import { useProfessorDetail } from "@/hooks/useProfessors";
import { Button } from "@/components/ui";

interface ProfessorDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function ProfessorDetailPage({ params }: ProfessorDetailPageProps) {
  const tCommon = useTranslations("common");
  const tProfessors = useTranslations("professors");
  const { id } = use(params);
  const { professor, loading, error, refetch } = useProfessorDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/professors">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/professors" className="hover:text-k-subtle">{tProfessors("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">
            {professor ? `${professor.firstName} ${professor.lastName}` : id}
          </span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading professor details...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {professor && <ProfessorDetail professor={professor} onStatusChanged={refetch} />}
    </div>
  );
}
