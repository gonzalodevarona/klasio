import { getTranslations } from "next-intl/server";
import AdminList from "@/components/admins/AdminList";

export const metadata = {
  title: "Admins - Klasio",
};

export default async function AdminsPage() {
  const t = await getTranslations("admins");

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">{t("pageTitle")}</h1>
      </div>

      <AdminList />
    </div>
  );
}
