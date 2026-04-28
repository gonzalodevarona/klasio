"use client";

import { useTranslations } from "next-intl";

export interface UnlimitedBadgeProps {
  expiresAt: Date;
}

export function UnlimitedBadge({ expiresAt }: UnlimitedBadgeProps) {
  const t = useTranslations("membership.unlimited");

  const today = new Date();
  const daysRemaining = Math.ceil(
    (expiresAt.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)
  );

  return (
    <div className="rounded-lg border border-purple-200 bg-purple-50 p-4">
      <div className="flex items-center gap-2">
        <span className="inline-block rounded-full bg-purple-600 px-3 py-1 text-sm font-semibold text-white">
          {t("badge")}
        </span>
      </div>
      <p className="mt-2 text-sm text-gray-600">
        {t("daysRemaining", { days: daysRemaining })}
      </p>
    </div>
  );
}
