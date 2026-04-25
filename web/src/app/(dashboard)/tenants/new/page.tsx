import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import TenantForm from "@/components/tenants/TenantForm";

export const metadata = {
  title: "Create New Tenant - Klasio",
};

export default async function NewTenantPage() {
  const tTenants = await getTranslations("tenants");
  const tCommon = await getTranslations("common");

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/tenants">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/tenants" className="hover:text-k-subtle">{tTenants("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">New</span>
        </nav>
      </div>

      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark mb-8">Create New Tenant</h1>

      <TenantForm />
    </div>
  );
}
