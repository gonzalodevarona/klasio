"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProgramPlanForm from "@/components/programs/ProgramPlanForm";
import { useProgramDetail } from "@/hooks/usePrograms";
import { useProgramPlanDetail } from "@/hooks/useProgramPlans";

interface EditPlanPageProps {
  params: Promise<{ id: string; planId: string }>;
}

export default function EditPlanPage({ params }: EditPlanPageProps) {
  const t = useTranslations("programs");
  const { id, planId } = use(params);
  const { program, loading: programLoading } = useProgramDetail(id);
  const { plan, loading: planLoading, error } = useProgramPlanDetail(id, planId);

  const loading = programLoading || planLoading;

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          {t("detailBreadcrumb")}
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/programs/${id}`}
          className="hover:text-gray-700 hover:underline"
        >
          {program?.name ?? id}
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/programs/${id}/plans/${planId}`}
          className="hover:text-gray-700 hover:underline"
        >
          {plan?.name ?? t("planDetailBreadcrumb")}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{t("planEditBreadcrumb")}</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">{t("planEditPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          {t("planEditLoadingText")}
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

      {plan && (
        <div className="bg-white shadow rounded-lg p-6">
          <ProgramPlanForm
            programId={id}
            tenantId={program?.tenantId}
            plan={plan}
          />
        </div>
      )}
    </div>
  );
}
