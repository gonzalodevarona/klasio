"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ProgramStatus } from "@/lib/types/program";

interface ProgramStatusBadgeProps {
  status: ProgramStatus;
}

const STATUS_VARIANT: Record<ProgramStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function ProgramStatusBadge({ status }: ProgramStatusBadgeProps) {
  const t = useTranslations("badges.programStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
