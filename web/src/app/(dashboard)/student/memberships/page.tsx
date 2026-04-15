"use client";

import Link from "next/link";
import { useMyMemberships } from "@/hooks/useMemberships";
import MembershipStatusBadge from "@/components/memberships/MembershipStatusBadge";
import HourBalance from "@/components/memberships/HourBalance";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

export default function StudentMembershipsPage() {
  const { memberships, loading, error } = useMyMemberships();

  const hasActiveMembership = memberships.some(
    (m) =>
      m.status === "ACTIVE" ||
      m.status === "PENDING_PAYMENT" ||
      m.status === "PENDING_PAYMENT_VALIDATION" ||
      m.status === "PENDING_MANAGER_ACTIVATION"
  );

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Memberships</h1>
          <p className="mt-1 text-sm text-gray-500">
            Your active and past program memberships.
          </p>
        </div>
        {!hasActiveMembership && (
          <Link
            href="/student/memberships/new"
            className="shrink-0 rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
          >
            + New Membership
          </Link>
        )}
      </div>

      {loading && (
        <p className="py-8 text-center text-sm text-gray-500">Loading…</p>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {!loading && !error && memberships.length === 0 && (
        <p className="py-8 text-center text-sm text-gray-400">
          You have no memberships yet.
        </p>
      )}

      <div className="space-y-6">
        {memberships.map((m) => (
          <div
            key={m.id}
            className="rounded-lg border border-gray-200 bg-white p-5 space-y-4"
          >
            {/* Header */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-gray-900">{m.planName}</p>
                <p className="text-xs text-gray-400 mt-0.5 font-mono">{m.id}</p>
              </div>
              <MembershipStatusBadge status={m.status} />
            </div>

            {/* Hour balance bar */}
            <HourBalance available={m.availableHours} purchased={m.purchasedHours} />

            {/* Key info */}
            <div className="grid grid-cols-2 gap-x-8 gap-y-1 text-sm">
              <div>
                <span className="text-gray-500">Expires: </span>
                <span className="font-medium text-gray-900">
                  {formatDate(m.expirationDate)}
                </span>
              </div>
              <div>
                <span className="text-gray-500">Payment: </span>
                <span className="font-medium text-gray-900">
                  {m.paymentValidated ? "Validated" : "Pending"}
                </span>
              </div>
            </div>

            {/* Actions row */}
            <div className="pt-1 flex items-center gap-4">
              <Link
                href={`/student/memberships/${m.id}`}
                className="text-sm text-indigo-600 hover:text-indigo-800 font-medium"
              >
                View details →
              </Link>
              {(m.status === "EXPIRED" || m.status === "INACTIVE") && (
                <Link
                  href={`/student/memberships/new?renew=${m.id}`}
                  className="text-sm font-medium text-emerald-600 hover:text-emerald-800"
                >
                  Renew →
                </Link>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
