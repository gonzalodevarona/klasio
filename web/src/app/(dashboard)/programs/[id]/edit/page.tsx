"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import ProgramForm from "@/components/programs/ProgramForm";
import { useProgramDetail } from "@/hooks/usePrograms";

interface EditProgramPageProps {
  params: Promise<{ id: string }>;
}

export default function EditProgramPage({ params }: EditProgramPageProps) {
  const t = useTranslations("programs");
  const { id } = use(params);
  const { program, loading, error } = useProgramDetail(id);

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
        <span className="text-gray-900">{t("planEditBreadcrumb")}</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">{t("editPageTitle")}</h1>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          {t("editLoadingText")}
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

      {program && <ProgramForm program={program} />}
    </div>
  );
}
