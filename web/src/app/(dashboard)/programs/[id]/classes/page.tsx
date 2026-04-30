import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ClassList from "@/components/classes/ClassList";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("classesPageTitle")} - Klasio` };
}

export default async function ClassesPage({ params }: { params: Promise<{ id: string }> }) {
  const t = await getTranslations("programs");
  const tCommon = await getTranslations("common");
  const { id } = await params;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("classesPageBreadcrumb")}</span>
        </nav>
      </div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("classesPageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href={`/programs/${id}/classes/new`}>+ {t("classesAddButton")}</Link>
        </Button>
      </div>
      <ClassList programId={id} />
    </div>
  );
}
