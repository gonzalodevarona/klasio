import AdminList from "@/components/admins/AdminList";

export const metadata = {
  title: "Admins - Klasio",
};

export default function AdminsPage() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Admin Users</h1>
        <p className="mt-1 text-sm text-gray-500">
          Manage administrator accounts across all tenants.
        </p>
      </div>

      <AdminList />
    </div>
  );
}
