import Link from "next/link";
import { getTranslations } from "next-intl/server";
import ProgramList from "@/components/programs/ProgramList";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("pageTitle")} - Klasio` };
}

export default async function ProgramsPage() {
  const t = await getTranslations("programs");

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
        <Link
          href="/programs/new"
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          {t("createButton")}
        </Link>
      </div>

      <ProgramList />
    </div>
  );
}
