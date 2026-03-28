"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useMemberships, useMembershipActions } from "@/hooks/useMemberships";
import MembershipList from "@/components/memberships/MembershipList";

interface Props {
  params: Promise<{ id: string }>;
}

export default function StudentMembershipsPage({ params }: Props) {
  const { id: studentId } = use(params);
  const { memberships, loading, error, refetch } = useMemberships(studentId);
  const { activateMembership, validatePayment, loading: actionLoading, error: actionError } =
    useMembershipActions();

  const [statusFilter, setStatusFilter] = useState<string>("");

  const filtered = statusFilter
    ? memberships.filter((m) => m.status === statusFilter)
    : memberships;

  async function handleActivate(id: string) {
    await activateMembership(id);
    refetch();
  }

  async function handleValidatePayment(id: string) {
    await validatePayment(id, { activateDirectly: true });
    refetch();
  }

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/students" className="hover:text-gray-700 hover:underline">
          Students
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/students/${studentId}`} className="hover:text-gray-700 hover:underline">
          Student
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Memberships</span>
      </nav>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Memberships</h1>
        <Link
          href={`/students/${studentId}/memberships/new`}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700"
        >
          New Membership
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-4 flex gap-2">
        {["", "ACTIVE", "INACTIVE", "EXPIRED", "PENDING_PAYMENT_VALIDATION", "PENDING_MANAGER_ACTIVATION"].map(
          (s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                statusFilter === s
                  ? "bg-blue-600 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {s === "" ? "All" : s.replace(/_/g, " ")}
            </button>
          )
        )}
      </div>

      {(actionError || error) && (
        <div className="mb-4 rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200">
          {actionError ?? error}
        </div>
      )}

      {loading ? (
        <div className="py-8 text-center text-sm text-gray-500">Loading memberships...</div>
      ) : (
        <MembershipList
          memberships={filtered}
          studentId={studentId}
          onActivate={actionLoading ? undefined : handleActivate}
          onValidatePayment={actionLoading ? undefined : handleValidatePayment}
        />
      )}
    </div>
  );
}
