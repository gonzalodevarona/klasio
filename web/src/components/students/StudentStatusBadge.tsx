"use client";

import { useTranslations } from "next-intl";
import { StudentStatus } from "@/lib/types/student";

interface StudentStatusBadgeProps {
  status: StudentStatus;
}

const STATUS_STYLES: Record<StudentStatus, string> = {
  ACTIVE:   "bg-green-100 text-green-800",
  INACTIVE: "bg-red-100 text-red-800",
};

export default function StudentStatusBadge({ status }: StudentStatusBadgeProps) {
  const t = useTranslations("badges.studentStatus");
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}>
      {t(status)}
    </span>
  );
}
