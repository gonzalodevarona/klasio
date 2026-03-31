"use client";

import { useAuth } from "@/hooks/useAuth";

export default function ManagerDashboard() {
  const { user, logout } = useAuth();

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Manager Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Program Overview</p>
        </div>
        <button
          onClick={logout}
          className="px-4 py-2 text-sm text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"
        >
          Sign Out
        </button>
      </div>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">
          Welcome, Manager. Manage your program from the sidebar.
        </p>
      </div>
    </div>
  );
}
