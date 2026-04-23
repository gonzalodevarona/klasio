import { getTranslations } from "next-intl/server";
import ManagerList from "@/components/managers/ManagerList";

export const metadata = {
  title: "Managers - Klasio",
};

export default async function ManagersPage() {
  const t = await getTranslations("managers");

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
      </div>
      <ManagerList />
    </div>
  );
}
