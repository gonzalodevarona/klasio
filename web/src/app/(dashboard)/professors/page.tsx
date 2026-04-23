import { getTranslations } from "next-intl/server";
import ProfessorList from "@/components/professors/ProfessorList";

export const metadata = {
  title: "Professors - Klasio",
};

export default async function ProfessorsPage() {
  const t = await getTranslations("professors");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
      </div>

      <ProfessorList />
    </div>
  );
}
