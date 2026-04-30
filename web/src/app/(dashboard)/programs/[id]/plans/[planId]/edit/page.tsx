"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Button, Card } from "@/components/ui";
import ProgramPlanForm from "@/components/programs/ProgramPlanForm";
import { useProgramDetail } from "@/hooks/usePrograms";
import { useProgramPlanDetail } from "@/hooks/useProgramPlans";

interface EditPlanPageProps {
  params: Promise<{ id: string; planId: string }>;
}

export default function EditPlanPage({ params }: EditPlanPageProps) {
  const t = useTranslations("programs");
  const tCommon = useTranslations("common");
  const { id, planId } = use(params);
  const { program, loading: programLoading } = useProgramDetail(id);
  const { plan, loading: planLoading, error } = useProgramPlanDetail(id, planId);

  const loading = programLoading || planLoading;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/plans/${planId}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}`} className="hover:text-k-subtle">{program?.name ?? id}</Link>
          <span className="mx-2">/</span>
          <Link href={`/programs/${id}/plans/${planId}`} className="hover:text-k-subtle">
            {plan?.name ?? t("planDetailBreadcrumb")}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("planEditBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">{t("planEditPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          {t("planEditLoadingText")}
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

      {plan && (
        <Card padding="md">
          <ProgramPlanForm
            programId={id}
            tenantId={program?.tenantId}
            plan={plan}
          />
        </Card>
      )}
    </div>
  );
}
