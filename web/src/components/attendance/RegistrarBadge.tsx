"use client";

import { useTranslations } from "next-intl";
import { useUsersByIds } from "@/hooks/useUsersByIds";

type Props = { createdBy: string };

export function RegistrarBadge({ createdBy }: Props) {
  const t = useTranslations("attendance.walkIn");
  const { users } = useUsersByIds([createdBy]);
  const u = users[createdBy];
  if (!u) return null;
  return (
    <span className="ml-2 text-xs text-gray-500 italic">
      {t("registeredBy", { name: u.fullName, role: u.role })}
    </span>
  );
}
