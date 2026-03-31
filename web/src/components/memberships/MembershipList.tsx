"use client";

import Link from "next/link";
import { MembershipSummary } from "@/lib/types/membership";
import MembershipStatusBadge from "./MembershipStatusBadge";
import HourBalance from "./HourBalance";

interface MembershipListProps {
  memberships: MembershipSummary[];
  studentId: string;
  onActivate?: (id: string) => void;
  onValidatePayment?: (id: string) => void;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString();
}

export default function MembershipList({
  memberships,
  studentId,
  onActivate,
  onValidatePayment,
}: MembershipListProps) {
  if (memberships.length === 0) {
    return (
      <div className="rounded-md border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500">
        No memberships found.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-md border border-gray-200">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Plan</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Hours</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Start</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Expires</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {memberships.map((m) => (
            <tr key={m.id} className="hover:bg-gray-50">
              <td className="px-4 py-3 text-sm text-gray-700 font-medium">
                {m.planName}
              </td>
              <td className="px-4 py-3">
                <MembershipStatusBadge status={m.status} />
              </td>
              <td className="px-4 py-3">
                <HourBalance available={m.availableHours} purchased={m.purchasedHours} />
              </td>
              <td className="px-4 py-3 text-gray-600">{formatDate(m.startDate)}</td>
              <td className="px-4 py-3 text-gray-600">{formatDate(m.expirationDate)}</td>
              <td className="px-4 py-3">
                <div className="flex items-center gap-2">
                  <Link
                    href={`/students/${studentId}/memberships/${m.id}`}
                    className="text-blue-600 hover:underline text-xs"
                  >
                    View
                  </Link>
                  {m.status === "PENDING_PAYMENT_VALIDATION" && onValidatePayment && (
                    <button
                      onClick={() => onValidatePayment(m.id)}
                      className="text-xs text-indigo-600 hover:underline"
                    >
                      Validate Payment
                    </button>
                  )}
                  {m.status === "PENDING_MANAGER_ACTIVATION" && onActivate && (
                    <button
                      onClick={() => onActivate(m.id)}
                      className="text-xs text-green-600 hover:underline"
                    >
                      Activate
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
