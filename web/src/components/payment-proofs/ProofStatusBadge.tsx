"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ProofStatus } from "@/lib/types/paymentProof";

const STATUS_VARIANT: Record<ProofStatus, BadgeVariant> = {
  PENDING:    "pending",
  APPROVED:   "approved",
  REJECTED:   "rejected",
  SUPERSEDED: "inactive",
};

interface Props {
  status: ProofStatus;
}

export function ProofStatusBadge({ status }: Props) {
  const t = useTranslations("badges.proofStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
