"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { DropInModal } from "./DropInModal";

interface Props {
  classId: string;
  sessionDate: string;
  startTime: string;
  programDropInPrice: string;
  onRegistered: () => void;
}

export function DropInButton({
  classId,
  sessionDate,
  startTime,
  programDropInPrice,
  onRegistered,
}: Props) {
  const t = useTranslations("attendance.dropIn");
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="inline-flex items-center gap-1.5 rounded-md bg-violet-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-violet-700 transition-colors"
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
