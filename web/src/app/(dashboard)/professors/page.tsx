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
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>

      <ProfessorList />
    </div>
  );
}
