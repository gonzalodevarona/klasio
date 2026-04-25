"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { MembershipSummary } from "@/lib/types/membership";
import MembershipStatusBadge from "./MembershipStatusBadge";
import HourBalance from "./HourBalance";
import { Table, Thead, Th, Tr, Td, Button } from "@/components/ui";

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
    <Table>
      <Thead>
        <tr>
          <Th>{t("colPlan")}</Th>
          <Th>{t("colStatus")}</Th>
          <Th>{t("colHours")}</Th>
          <Th>{t("colStart")}</Th>
          <Th>{t("colExpires")}</Th>
          <Th right>{t("colActions")}</Th>
        </tr>
      </Thead>
      <tbody>
        {memberships.map((m) => (
          <Tr key={m.id}>
            <Td bold>{m.planName}</Td>
            <Td>
              <MembershipStatusBadge status={m.status} />
            </Td>
            <Td>
              <HourBalance available={m.availableHours} purchased={m.purchasedHours} />
            </Td>
            <Td muted>{formatDate(m.startDate)}</Td>
            <Td muted>{formatDate(m.expirationDate)}</Td>
            <Td right>
              <div className="flex items-center justify-end gap-2">
                <Link
                  href={`/students/${studentId}/memberships/${m.id}`}
                  className="text-blue-600 hover:underline text-xs"
                >
                  {t("actionView")}
                </Link>
                {m.status === "PENDING_PAYMENT_VALIDATION" && onValidatePayment && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onValidatePayment(m.id)}
                  >
                    {t("actionValidatePayment")}
                  </Button>
                )}
                {m.status === "PENDING_MANAGER_ACTIVATION" && onActivate && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onActivate(m.id)}
                  >
                    {t("actionActivate")}
                  </Button>
                )}
              </div>
            </Td>
          </Tr>
        ))}
      </tbody>
    </Table>
  );
}
