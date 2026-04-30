import { getTranslations } from "next-intl/server";
import { StatCard } from "@/components/ui";
import { DelegatedMembershipList } from "@/components/payment-proofs/DelegatedMembershipList";

export const metadata = { title: "Manager Dashboard - Klasio" };

export default async function ManagerDashboard() {
  const t = await getTranslations("managerDashboard");
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("title")}</h1>
        <p className="font-[var(--font-mono)] text-xs text-k-muted mt-1">{t("subtitle")}</p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label={t("statClassesThisWeek")} value="—" dark />
        <StatCard label={t("statStudentsInProgram")} value="—" />
        <StatCard label={t("statPendingActivations")} value="—" />
        <StatCard label={t("statHoursLogged")} value="—" />
      </div>
      <div className="mt-8">
        <h2 className="text-base font-semibold text-k-dark mb-4">{t("membershipsHeading")}</h2>
        <DelegatedMembershipList />
      </div>
    </div>
  );
}
