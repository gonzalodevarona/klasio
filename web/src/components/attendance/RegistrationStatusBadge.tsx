"use client";

import { RegistrationStatus } from "@/lib/types/attendance";

interface RegistrationStatusBadgeProps {
  status: string;
}

const STATUS_STYLES: Record<RegistrationStatus, string> = {
  REGISTERED:           "bg-green-100 text-green-700",
  CANCELLED_BY_STUDENT: "bg-gray-100 text-gray-500",
  CANCELLED_BY_SYSTEM:  "bg-gray-100 text-gray-500",
  PRESENT:              "bg-blue-100 text-blue-700",
  PRESENT_NO_HOURS:     "bg-orange-100 text-orange-700",
  ABSENT:               "bg-red-100 text-red-700",
};

const STATUS_LABELS: Record<RegistrationStatus, string> = {
  REGISTERED:           "Registered",
  CANCELLED_BY_STUDENT: "Cancelled",
  CANCELLED_BY_SYSTEM:  "Cancelled (System)",
  PRESENT:              "Present",
  PRESENT_NO_HOURS:     "Present (No Hours)",
  ABSENT:               "Absent",
};

export default function RegistrationStatusBadge({ status }: RegistrationStatusBadgeProps) {
  const key = status as RegistrationStatus;
  const styles = STATUS_STYLES[key] ?? "bg-gray-100 text-gray-500";
  const label = STATUS_LABELS[key] ?? status;

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${styles}`}
    >
      {label}
    </span>
  );
}
