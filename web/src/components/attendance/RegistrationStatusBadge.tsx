"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { RegistrationStatus } from "@/lib/types/attendance";

interface RegistrationStatusBadgeProps {
  status: RegistrationStatus;
}

const STATUS_VARIANT: Record<RegistrationStatus, BadgeVariant> = {
  REGISTERED:           "active",
  CANCELLED_BY_STUDENT: "inactive",
  CANCELLED_BY_SYSTEM:  "inactive",
  SESSION_CANCELLED:    "rejected",
  PRESENT:              "info",
  PRESENT_NO_HOURS:     "pending",
  ABSENT:               "rejected",
};

export default function RegistrationStatusBadge({ status }: RegistrationStatusBadgeProps) {
  const t = useTranslations("badges.registrationStatus");
  const variant = STATUS_VARIANT[status] ?? "inactive";
  return <Badge variant={variant} label={t(status)} />;
}

export function DropInTag() {
  const t = useTranslations("attendance.dropIn");
  return <Badge variant="dropIn" label={t("rosterTag")} small />;
}
