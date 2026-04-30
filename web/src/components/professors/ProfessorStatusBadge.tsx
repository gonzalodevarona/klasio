"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ProfessorStatus } from "@/lib/types/professor";

interface ProfessorStatusBadgeProps {
  status: ProfessorStatus;
}

const STATUS_VARIANT: Record<ProfessorStatus, BadgeVariant> = {
  INVITED:     "pending",
  ACTIVE:      "active",
  DEACTIVATED: "rejected",
};

export default function ProfessorStatusBadge({ status }: ProfessorStatusBadgeProps) {
  const t = useTranslations("badges.professorStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
