"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ClassStatus } from "@/lib/types/programClass";

interface ClassStatusBadgeProps {
  status: ClassStatus;
}

const STATUS_VARIANT: Record<ClassStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function ClassStatusBadge({ status }: ClassStatusBadgeProps) {
  const t = useTranslations("badges.classStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
