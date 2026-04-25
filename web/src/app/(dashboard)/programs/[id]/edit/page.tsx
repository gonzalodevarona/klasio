"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProgramForm from "@/components/programs/ProgramForm";
import { useProgramDetail } from "@/hooks/usePrograms";
import { Button } from "@/components/ui";

interface EditProgramPageProps {
  params: Promise<{ id: string }>;
}

export default function EditProgramPage({ params }: EditProgramPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id } = use(params);
  const { program, loading, error } = useProgramDetail(id);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("planEditBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">{t("editPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("editLoadingText")}
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

      {program && <ProgramForm program={program} />}
    </div>
  );
}
