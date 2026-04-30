"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ClassType } from "@/lib/types/programClass";

interface ClassTypeBadgeProps {
  type: ClassType;
}

const TYPE_VARIANT: Record<ClassType, BadgeVariant> = {
  RECURRING: "info",
  // TODO: no purple token in design system — collapses with default inactive grey
  ONE_TIME:  "inactive",
};

export default function ClassTypeBadge({ type }: ClassTypeBadgeProps) {
  const t = useTranslations("badges.classType");
  return <Badge variant={TYPE_VARIANT[type]} label={t(type)} />;
}
