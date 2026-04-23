import Link from "next/link";
import { getTranslations } from "next-intl/server";
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
  const { id } = await params;

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href={`/programs/${id}/classes`} className="hover:text-gray-700">
          {t("classDetailBreadcrumb")}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{t("classNewBreadcrumb")}</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t("classNewPageTitle")}</h1>

      <ClassForm programId={id} />
    </div>
  );
}
