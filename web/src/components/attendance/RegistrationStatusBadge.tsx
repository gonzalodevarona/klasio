"use client";

import { useTranslations } from "next-intl";
import { RegistrationStatus } from "@/lib/types/attendance";

interface RegistrationStatusBadgeProps {
  status: RegistrationStatus;
}

const STATUS_STYLES: Record<RegistrationStatus, string> = {
  REGISTERED:           "bg-green-100 text-green-700",
  CANCELLED_BY_STUDENT: "bg-gray-100 text-gray-500",
  CANCELLED_BY_SYSTEM:  "bg-gray-100 text-gray-500",
  SESSION_CANCELLED:    "bg-red-100 text-red-700",
  PRESENT:              "bg-blue-100 text-blue-700",
  PRESENT_NO_HOURS:     "bg-orange-100 text-orange-700",
  ABSENT:               "bg-red-100 text-red-700",
};

export default function RegistrationStatusBadge({ status }: RegistrationStatusBadgeProps) {
  const t = useTranslations("badges.registrationStatus");
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status] ?? "bg-gray-100 text-gray-500"}`}>
      {t(status)}
    </span>
  );
}
