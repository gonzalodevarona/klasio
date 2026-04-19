import ManagerList from "@/components/managers/ManagerList";

export default function ManagersPage() {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Managers</h1>
        <p className="text-sm text-gray-500 mt-1">Manage program-level managers across all tenants.</p>
      </div>
      <ManagerList />
    </div>
  );
}
