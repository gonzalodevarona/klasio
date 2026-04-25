"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { StudentStatus } from "@/lib/types/student";

interface StudentStatusBadgeProps {
  status: StudentStatus;
}

const STATUS_VARIANT: Record<StudentStatus, BadgeVariant> = {
  ACTIVE:   "active",
  INACTIVE: "inactive",
};

export default function StudentStatusBadge({ status }: StudentStatusBadgeProps) {
  const t = useTranslations("badges.studentStatus");
  return <Badge variant={STATUS_VARIANT[status]} label={t(status)} />;
}
