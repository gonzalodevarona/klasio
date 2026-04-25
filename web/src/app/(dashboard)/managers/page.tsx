import { getTranslations } from "next-intl/server";
import ManagerList from "@/components/managers/ManagerList";

export const metadata = {
  title: "Managers - Klasio",
};

export default async function ManagersPage() {
  const t = await getTranslations("managers");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>
      <ManagerList />
    </div>
  );
}
