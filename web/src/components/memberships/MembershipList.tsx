"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
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
  const t = useTranslations("memberships");

  if (memberships.length === 0) {
    return (
      <div className="rounded-md border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500">
        {t("listEmpty")}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-md border border-gray-200">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-gray-600">{t("colPlan")}</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">{t("colStatus")}</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">{t("colHours")}</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">{t("colStart")}</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">{t("colExpires")}</th>
            <th className="px-4 py-3 text-left font-medium text-gray-600">{t("colActions")}</th>
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
                    {t("actionView")}
                  </Link>
                  {m.status === "PENDING_PAYMENT_VALIDATION" && onValidatePayment && (
                    <button
                      onClick={() => onValidatePayment(m.id)}
                      className="text-xs text-indigo-600 hover:underline"
                    >
                      {t("actionValidatePayment")}
                    </button>
                  )}
                  {m.status === "PENDING_MANAGER_ACTIVATION" && onActivate && (
                    <button
                      onClick={() => onActivate(m.id)}
                      className="text-xs text-green-600 hover:underline"
                    >
                      {t("actionActivate")}
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
