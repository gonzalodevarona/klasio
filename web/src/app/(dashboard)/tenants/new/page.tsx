import Link from "next/link";
import TenantForm from "@/components/tenants/TenantForm";

export const metadata = {
  title: "Create New League - Klasio",
};

export default function NewTenantPage() {
  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/tenants" className="hover:text-gray-700 hover:underline">
          Tenants
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">New</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">
        Create New League
      </h1>

      <TenantForm />
    </div>
  );
}
