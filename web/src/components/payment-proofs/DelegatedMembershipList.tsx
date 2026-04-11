"use client";

import { useDelegatedMemberships } from "@/hooks/useDelegatedMemberships";
import { useMembershipActions } from "@/hooks/useMemberships";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString();
}

export function DelegatedMembershipList() {
  const { memberships, loading, error, refetch } = useDelegatedMemberships();
  const { activateMembership, loading: activating } = useMembershipActions();

  async function handleActivate(membershipId: string) {
    await activateMembership(membershipId);
    refetch();
  }

  if (loading) {
    return <p className="text-sm text-gray-500">Loading delegated memberships…</p>;
  }

  if (error) {
    return (
      <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
        {error}
      </div>
    );
  }

  if (memberships.length === 0) {
    return <p className="text-sm text-gray-400">No memberships awaiting your activation.</p>;
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Student
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Program
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Delegated
            </th>
            <th className="px-4 py-3" />
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {memberships.map((m) => (
            <tr key={m.membershipId} className="hover:bg-gray-50">
              <td className="px-4 py-3 text-sm text-gray-900">{m.studentName}</td>
              <td className="px-4 py-3 text-sm text-gray-700">{m.programName}</td>
              <td className="px-4 py-3 text-sm text-gray-500">{formatDate(m.delegatedAt)}</td>
              <td className="px-4 py-3 text-right">
                <button
                  onClick={() => handleActivate(m.membershipId)}
                  disabled={activating}
                  className="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  Activate
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
