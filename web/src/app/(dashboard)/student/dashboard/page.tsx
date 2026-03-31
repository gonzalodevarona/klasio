"use client";

import { useAuth } from "@/hooks/useAuth";

export default function StudentDashboard() {
  const { user, logout } = useAuth();

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Student Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Your Memberships & Classes</p>
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
          Welcome, Student. Your hour balance and memberships will appear here.
        </p>
      </div>
    </div>
  );
}
