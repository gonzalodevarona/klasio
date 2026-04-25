import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ProgramForm from "@/components/programs/ProgramForm";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("newPageTitle")} - Klasio` };
}

export default async function NewProgramPage() {
  const t = await getTranslations("programs");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/programs">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/programs" className="hover:text-k-subtle">{t("detailBreadcrumb")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{t("newBreadcrumbNew")}</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">
        {t("newPageTitle")}
      </h1>

      <ProgramForm />
    </div>
  );
}
