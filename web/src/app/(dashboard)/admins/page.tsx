import { getTranslations } from "next-intl/server";
import AdminList from "@/components/admins/AdminList";

export const metadata = {
  title: "Admins - Klasio",
};

export default async function AdminsPage() {
  const t = await getTranslations("admins");

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
      </div>

      <AdminList />
    </div>
  );
}
