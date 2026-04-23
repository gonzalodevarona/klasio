"use client";

import { useTranslations } from "next-intl";
import { Flag, XCircle } from "lucide-react";

interface SessionStatusBadgeProps {
  status: string;
  reason?: string | null;
}

export default function SessionStatusBadge({ status, reason }: SessionStatusBadgeProps) {
  const t = useTranslations("badges.sessionStatus");

  if (status === "CANCELLED") {
    return (
      <span
        title={reason ?? undefined}
        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-red-100 text-red-700"
      >
        <XCircle className="w-3.5 h-3.5" />
        {t("CANCELLED")}
      </span>
    );
  }

  if (status === "ALERTED") {
    return (
      <span
        title={reason ?? undefined}
        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-slate-100 text-slate-600"
      >
        {t("ALERTED")}
        <Flag className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
      </span>
    );
  }

  // SCHEDULED and any unknown status
  return (
    <span className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-slate-100 text-slate-600">
      {t("SCHEDULED")}
    </span>
  );
}
