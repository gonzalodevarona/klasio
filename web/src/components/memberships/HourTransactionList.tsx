"use client";

import { useTranslations } from "next-intl";
import { HourTransactionType } from "@/lib/types/membership";
import { useHourTransactions } from "@/hooks/useHourTransactions";

const TYPE_STYLES: Record<HourTransactionType, string> = {
  ATTENDANCE_DEDUCTION: "text-red-600",
  MANUAL_ADDITION:      "text-green-600",
  MANUAL_SUBTRACTION:   "text-orange-600",
};

function formatDelta(delta: number): string {
  if (delta === 0) return "—";
  return delta > 0 ? `+${delta}` : String(delta);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

interface HourTransactionListProps {
  membershipId: string;
}

export default function HourTransactionList({ membershipId }: HourTransactionListProps) {
  const t = useTranslations("memberships");
  const tType = useTranslations("badges.hourTransactionType");
  const { transactions, loading, error } = useHourTransactions(membershipId);

  if (loading) return <div className="py-4 text-sm text-gray-500">{t("txLoading")}</div>;

  if (error) {
    return (
      <div className="rounded-md bg-red-50 p-4 text-sm text-red-700 border border-red-200">
        {error}
      </div>
    );
  }

  if (transactions.length === 0) {
    return <div className="py-4 text-sm text-gray-500">{t("txEmpty")}</div>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-sm">
        <thead>
          <tr className="border-b border-gray-200">
            <th className="py-2 pr-4 text-left font-medium text-gray-600">{t("txColType")}</th>
            <th className="py-2 pr-4 text-right font-medium text-gray-600">{t("txColDelta")}</th>
            <th className="py-2 pr-4 text-left font-medium text-gray-600">{t("txColReason")}</th>
            <th className="py-2 pr-4 text-left font-medium text-gray-600">{t("txColActor")}</th>
            <th className="py-2 text-left font-medium text-gray-600">{t("txColDate")}</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {transactions.map((tx) => (
            <tr key={tx.id}>
              <td className="py-2 pr-4">
                <span className={`font-medium ${TYPE_STYLES[tx.type]}`}>
                  {tType(tx.type)}
                </span>
              </td>
              <td
                className={`py-2 pr-4 text-right font-mono ${TYPE_STYLES[tx.type]}`}
                data-testid={`tx-delta-${tx.id}`}
              >
                {formatDelta(tx.delta)}
              </td>
              <td className="py-2 pr-4 text-gray-600 max-w-xs truncate">{tx.reason ?? "—"}</td>
              <td className="py-2 pr-4 text-gray-500 text-xs font-mono">
                {tx.actorId.slice(0, 8)}… <span className="text-gray-400">({tx.actorRole})</span>
              </td>
              <td className="py-2 text-gray-500 text-xs">{formatDate(tx.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
