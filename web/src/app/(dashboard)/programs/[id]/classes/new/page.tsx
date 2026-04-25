import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ClassForm from "@/components/classes/ClassForm";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("classNewPageTitle")} - Klasio` };
}

export default async function NewClassPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const t = await getTranslations("programs");
  const tCommon = await getTranslations("common");
  const { id } = await params;

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href={`/programs/${id}/classes`}>← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href={`/programs/${id}/classes`} className="hover:text-k-subtle">{t("classDetailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("classNewBreadcrumb")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-6">{t("classNewPageTitle")}</h1>

      <ClassForm programId={id} />
    </div>
  );
}
