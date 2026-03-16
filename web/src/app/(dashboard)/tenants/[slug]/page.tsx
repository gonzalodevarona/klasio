"use client";

import { use } from "react";
import Link from "next/link";
import TenantDetail from "@/components/tenants/TenantDetail";
import { useTenantDetail } from "@/hooks/useTenants";

interface TenantSlugPageProps {
  params: Promise<{ slug: string }>;
}

export default function TenantSlugPage({ params }: TenantSlugPageProps) {
  const { slug } = use(params);
  const { tenant, loading, error, refetch } = useTenantDetail(slug);

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/tenants" className="hover:text-gray-700 hover:underline">
          Tenants
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{tenant?.name ?? slug}</span>
      </nav>

      {loading && (
        <div className="text-center py-8 text-sm text-gray-500">
          Loading tenant details...
        </div>
      )}

      {error && (
        <div
          className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200"
          role="alert"
        >
          {error}
        </div>
      )}

      {tenant && <TenantDetail tenant={tenant} onDeactivated={refetch} />}
    </div>
  );
}
