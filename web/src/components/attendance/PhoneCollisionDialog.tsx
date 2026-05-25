"use client";

import { useTranslations } from "next-intl";

interface Props {
  open: boolean;
  phone: string;
  fullName: string;
  totalVisits: number;
  existingAttendeeId: string;
  onConfirm: (existingId: string) => void;
  onCancel: () => void;
}

export function PhoneCollisionDialog({
  open,
  phone,
  fullName,
  totalVisits,
  existingAttendeeId,
  onConfirm,
  onCancel,
}: Props) {
  const t = useTranslations("attendance.dropIn.phoneCollision");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">{t("title")}</h3>
        <p className="text-sm text-gray-700 mb-1">
          {t("body", { phone, fullName, count: totalVisits })}
        </p>
        <p className="text-sm text-gray-700 mb-4">{t("question")}</p>
        <div className="flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
          >
            {t("cancel")}
          </button>
          <button
            onClick={() => onConfirm(existingAttendeeId)}
            className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700"
          >
            {t("yes")}
          </button>
        </div>
      </div>
    </div>
  );
}
