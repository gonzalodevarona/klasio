import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";

export const metadata = { title: "Admin Dashboard - Klasio" };

export default async function AdminDashboard() {
  const t = await getTranslations("adminDashboard");
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{t("subtitle")}</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label={t("statStudents")} value="—" dark />
        <StatCard label={t("statActiveMemberships")} value="—" />
        <StatCard label={t("statPendingProofs")} value="—" />
        <StatCard label={t("statPrograms")} value="—" />
      </div>
    </div>
  );
}
