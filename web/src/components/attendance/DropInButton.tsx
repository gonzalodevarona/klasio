"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { DropInModal } from "./DropInModal";

const WINDOW_BEFORE_MS = 20 * 60 * 1000;
const WINDOW_AFTER_MS  = 10 * 60 * 1000;

interface Props {
  classId: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  programDropInPrice: string;
  onRegistered: () => void;
}

export function DropInButton({
  classId,
  sessionDate,
  startTime,
  endTime,
  programDropInPrice,
  onRegistered,
}: Props) {
  const t = useTranslations("attendance.dropIn");
  const [open, setOpen] = useState(false);

  const inWindow = isInsideWindow(sessionDate, startTime, endTime);

  return (
    <>
      <button
        type="button"
        disabled={!inWindow}
        title={!inWindow ? t("outsideWindowTooltip") : undefined}
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-1.5 rounded-md bg-violet-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-violet-700 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
      >
        {t("buttonLabel")}
      </button>
      {open && (
        <DropInModal
          classId={classId}
          sessionDate={sessionDate}
          startTime={startTime}
          programDropInPrice={programDropInPrice}
          onRegistered={() => {
            onRegistered();
            setOpen(false);
          }}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}

function isInsideWindow(sessionDate: string, startTime: string, endTime: string): boolean {
  const start = new Date(`${sessionDate}T${startTime}`).getTime();
  const end   = new Date(`${sessionDate}T${endTime}`).getTime();
  const now   = Date.now();
  return now >= start - WINDOW_BEFORE_MS && now <= end + WINDOW_AFTER_MS;
}
