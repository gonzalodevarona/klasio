"use client";

import { useTranslations } from "next-intl";
import { Badge, type BadgeVariant } from "@/components/ui";
import { Level } from "@/lib/types/enrollment";

interface LevelBadgeProps {
  level: Level;
}

const LEVEL_VARIANT: Record<Level, BadgeVariant> = {
  BEGINNER:     "beginner",
  INTERMEDIATE: "intermediate",
  ADVANCED:     "advanced",
};

export default function LevelBadge({ level }: LevelBadgeProps) {
  const t = useTranslations("badges.enrollmentLevel");
  return <Badge variant={LEVEL_VARIANT[level]} label={t(level)} />;
}
