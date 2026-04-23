"use client";

import { useTranslations } from "next-intl";
import { ClassStatus } from "@/lib/types/programClass";

interface ClassStatusBadgeProps {
  status: ClassStatus;
}

const STATUS_STYLES: Record<ClassStatus, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-gray-100 text-gray-800",
};

export default function ClassStatusBadge({ status }: ClassStatusBadgeProps) {
  const t = useTranslations("badges.classStatus");
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {t(status)}
    </span>
  );
}
