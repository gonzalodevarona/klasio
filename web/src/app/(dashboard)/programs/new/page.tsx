import Link from "next/link";
import { getTranslations } from "next-intl/server";
import ProgramForm from "@/components/programs/ProgramForm";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("newPageTitle")} - Klasio` };
}

export default async function NewProgramPage() {
  const t = await getTranslations("programs");

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          {t("detailBreadcrumb")}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{t("newBreadcrumbNew")}</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">
        {t("newPageTitle")}
      </h1>

      <ProgramForm />
    </div>
  );
}
