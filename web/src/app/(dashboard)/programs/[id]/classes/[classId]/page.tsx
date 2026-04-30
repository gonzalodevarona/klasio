"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ClassDetail from "@/components/classes/ClassDetail";
import { useProgramClassDetail } from "@/hooks/useProgramClasses";
import { Button } from "@/components/ui";

interface ClassDetailPageProps {
  params: Promise<{ id: string; classId: string }>;
}

export default function ClassDetailPage({ params }: ClassDetailPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, classId } = use(params);
  const { programClass, loading, error, refetch } = useProgramClassDetail(id, classId);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/classes`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}/classes`} className="hover:text-k-subtle">{t("classDetailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">
            {programClass ? programClass.name : classId}
          </span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("classDetailLoadingText")}
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
