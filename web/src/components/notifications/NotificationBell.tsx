"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Bell } from "lucide-react";
import { useTranslations } from "next-intl";
import { useNotificationCount } from "@/context/NotificationCountContext";
import NotificationDropdown from "./NotificationDropdown";

export default function NotificationBell() {
  const t = useTranslations("notifications");
  const { count, hasCancellation } = useNotificationCount();
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [dropdownStyle, setDropdownStyle] = useState<React.CSSProperties>({});

  // Compute dropdown position relative to viewport (fixed positioning)
  useEffect(() => {
    if (!open || !buttonRef.current) return;
    const rect = buttonRef.current.getBoundingClientRect();
    setDropdownStyle({
      position: "fixed",
      top: rect.bottom + 8,
      left: rect.left,
      zIndex: 9999,
    });
  }, [open]);

  // Close when clicking outside
  useEffect(() => {
    if (!open) return;

    function handleMouseDown(event: MouseEvent) {
      const target = event.target as Node;
      if (
        buttonRef.current?.contains(target) ||
        dropdownRef.current?.contains(target)
      ) return;
      setOpen(false);
    }

    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [open]);

  const badgeLabel = count > 10 ? t("badgeMax") : String(count);
  const bellColor = hasCancellation ? "text-red-400 hover:text-red-300" : "text-gray-300 hover:text-white";

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={t("bellAriaLabel")}
        className={`relative p-1 transition-colors rounded ${bellColor}`}
      >
        <Bell className="h-5 w-5" />
        {count > 0 && (
          <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-[1.1rem] h-[1.1rem] px-0.5 rounded-full bg-red-500 text-white text-[10px] font-bold leading-none">
            {badgeLabel}
          </span>
        )}
      </button>

      {open && typeof document !== "undefined" && createPortal(
        <div ref={dropdownRef} style={dropdownStyle}>
          <NotificationDropdown onClose={() => setOpen(false)} />
        </div>,
        document.body
      )}
    </>
  );
}
