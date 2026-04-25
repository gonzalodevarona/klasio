import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import TenantList from "@/components/tenants/TenantList";

export const metadata = {
  title: "Tenants - Klasio",
};

export default async function TenantsPage() {
  const t = await getTranslations("tenants");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href="/tenants/new">+ {t("createButton")}</Link>
        </Button>
      </div>

      <TenantList />
    </div>
  );
}
