"use client";

import { AlertTriangle, XCircle } from "lucide-react";

interface SessionStatusBadgeProps {
  status: string;
  reason?: string | null;
}

const STATUS_CONFIG: Record<
  string,
  { label: string; className: string; icon?: React.ReactNode }
> = {
  SCHEDULED: {
    label: "Scheduled",
    className: "bg-slate-100 text-slate-600",
  },
  ALERTED: {
    label: "Alert",
    className: "bg-amber-100 text-amber-700",
    icon: <AlertTriangle className="w-3.5 h-3.5" />,
  },
  CANCELLED: {
    label: "Cancelled",
    className: "bg-red-100 text-red-700",
    icon: <XCircle className="w-3.5 h-3.5" />,
  },
  COMPLETED: {
    label: "Completed",
    className: "bg-emerald-100 text-emerald-700",
  },
};

export default function SessionStatusBadge({ status, reason }: SessionStatusBadgeProps) {
  const config = STATUS_CONFIG[status] ?? STATUS_CONFIG["SCHEDULED"];

  return (
    <span
      title={reason ?? undefined}
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${config.className}`}
    >
      {config.icon}
      {config.label}
    </span>
  );
}
