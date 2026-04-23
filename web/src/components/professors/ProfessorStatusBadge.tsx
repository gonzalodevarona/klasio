"use client";

import { useTranslations } from "next-intl";
import { ProfessorStatus } from "@/lib/types/professor";

interface ProfessorStatusBadgeProps {
  status: ProfessorStatus;
}

const STATUS_STYLES: Record<ProfessorStatus, string> = {
  INVITED:     "bg-yellow-100 text-yellow-800",
  ACTIVE:      "bg-green-100 text-green-800",
  DEACTIVATED: "bg-red-100 text-red-800",
};

export default function ProfessorStatusBadge({ status }: ProfessorStatusBadgeProps) {
  const t = useTranslations("badges.professorStatus");
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}>
      {t(status)}
    </span>
  );
}
