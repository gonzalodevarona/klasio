"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { WalkInModal } from "./WalkInModal";

type Props = {
  classId: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  classLevel: string;
  onRegistered: () => void;
};

const WINDOW_BEFORE_MS = 20 * 60 * 1000;
const WINDOW_AFTER_MS  = 10 * 60 * 1000;

export function WalkInButton({
  classId,
  sessionDate,
  startTime,
  endTime,
  durationMinutes,
  classLevel,
  onRegistered,
}: Props) {
  const t = useTranslations("attendance.walkIn");
  const [open, setOpen] = useState(false);

  const inWindow = isInsideWindow(sessionDate, startTime, endTime);

  return (
    <>
      <button
        type="button"
        disabled={!inWindow}
        title={!inWindow ? t("outsideWindowTooltip") : undefined}
        onClick={() => setOpen(true)}
        className="px-3 py-1 text-sm rounded bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        {t("buttonLabel")}
      </button>
      {open && (
        <WalkInModal
          classId={classId}
          sessionDate={sessionDate}
          startTime={startTime}
          durationMinutes={durationMinutes}
          classLevel={classLevel}
          onClose={() => setOpen(false)}
          onSuccess={() => {
            setOpen(false);
            onRegistered();
          }}
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
