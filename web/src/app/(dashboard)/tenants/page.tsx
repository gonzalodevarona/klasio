import Link from "next/link";
import TenantList from "@/components/tenants/TenantList";

export const metadata = {
  title: "Tenants - Klasio",
};

export default function TenantsPage() {
  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Tenants</h1>
        <Link
          href="/tenants/new"
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          Create New League
        </Link>
      </div>

      <TenantList />
    </div>
  );
}
