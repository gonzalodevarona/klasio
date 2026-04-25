"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { MembershipStatus } from "@/lib/types/membership";

interface MembershipStatusBadgeProps {
  status: MembershipStatus;
}

const STATUS_VARIANT: Record<MembershipStatus, BadgeVariant> = {
  EXPIRED:                    "rejected",
  INACTIVE:                   "inactive",
  PENDING_PAYMENT:            "rejected",
  PENDING_PAYMENT_VALIDATION: "pending",
  PENDING_MANAGER_ACTIVATION: "pending",
  ACTIVE:                     "active",
};

export default function MembershipStatusBadge({ status }: MembershipStatusBadgeProps) {
  const t = useTranslations("badges.membershipStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
