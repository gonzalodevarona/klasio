"use client";

import { use } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import TenantDetail from "@/components/tenants/TenantDetail";
import { useTenantDetail } from "@/hooks/useTenants";
import { Button } from "@/components/ui";

interface TenantSlugPageProps {
  params: Promise<{ slug: string }>;
}

export default function TenantSlugPage({ params }: TenantSlugPageProps) {
  const tCommon = useTranslations("common");
  const tTenants = useTranslations("tenants");
  const { slug } = use(params);
  const { tenant, loading, error, refetch } = useTenantDetail(slug);

  return (
    <div>
      <div className="mb-6">
        <Button variant="ghost" size="sm" asChild>
          <Link href="/tenants">← {tCommon("back")}</Link>
        </Button>
        <nav className="mt-2 font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted">
          <Link href="/tenants" className="hover:text-k-subtle">{tTenants("pageTitle")}</Link>
          <span className="mx-2">/</span>
          <span className="text-k-subtle">{tenant?.name ?? slug}</span>
        </nav>
      </div>

      {loading && (
        <div className="text-center py-8 text-sm text-k-muted">
          Loading tenant details...
        </div>
      )}

      {error && (
        <div
          className="rounded-k-sm bg-k-danger-bg border border-k-danger-text/30 p-4 text-sm text-k-danger-text"
          role="alert"
        >
          {error}
        </div>
      )}

      {tenant && <TenantDetail tenant={tenant} onDeactivated={refetch} />}
    </div>
  );
}
