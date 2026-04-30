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
  OPEN:         "open",
};

export default function ClassLevelBadge({ level }: ClassLevelBadgeProps) {
  const t = useTranslations("badges.classLevel");
  // Defensive fallback: unknown future values fall back to "info" rather than crashing.
  const variant: BadgeVariant = LEVEL_VARIANT[level] ?? "info";
  return <Badge variant={variant} label={t(level)} />;
}
