"use client";

import { useTranslations } from "next-intl";
import { Flag, XCircle } from "lucide-react";
import { Badge } from "@/components/ui";

interface SessionStatusBadgeProps {
  status: string;
  reason?: string | null;
}

export default function SessionStatusBadge({ status, reason }: SessionStatusBadgeProps) {
  const t = useTranslations("badges.sessionStatus");

  if (status === "CANCELLED") {
    return (
      <Badge
        variant="rejected"
        label={t("CANCELLED")}
        title={reason ?? undefined}
        icon={<XCircle className="w-3.5 h-3.5" />}
      />
    );
  }

  if (status === "ALERTED") {
    return (
      <Badge
        variant="inactive"
        label={t("ALERTED")}
        title={reason ?? undefined}
        icon={<Flag className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />}
      />
    );
  }

  return <Badge variant="inactive" label={t("SCHEDULED")} />;
}
