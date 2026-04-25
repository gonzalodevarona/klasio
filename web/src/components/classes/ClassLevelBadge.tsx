"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { ClassLevel } from "@/lib/types/programClass";

interface ClassLevelBadgeProps {
  level: ClassLevel;
}

const LEVEL_VARIANT: Record<ClassLevel, BadgeVariant> = {
  BEGINNER:     "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED:     "advanced",
};

export default function ClassLevelBadge({ level }: ClassLevelBadgeProps) {
  const t = useTranslations("badges.classLevel");
  return <Badge variant={LEVEL_VARIANT[level]} label={t(level)} />;
}
