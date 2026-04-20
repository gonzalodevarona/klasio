"use client";

import { useEffect, useRef, useState } from "react";
import { Bell } from "lucide-react";
import { useNotificationCount } from "@/context/NotificationCountContext";
import NotificationDropdown from "./NotificationDropdown";

export default function NotificationBell() {
  const { count } = useNotificationCount();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    if (!open) return;

    function handleMouseDown(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [open]);

  const badgeLabel = count > 10 ? "10+" : String(count);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label="Notifications"
        className="relative p-1 text-gray-300 hover:text-white transition-colors rounded"
      >
        <Bell className="h-5 w-5" />
        {count > 0 && (
          <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-[1.1rem] h-[1.1rem] px-0.5 rounded-full bg-red-500 text-white text-[10px] font-bold leading-none">
            {badgeLabel}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute left-0 mt-2 z-50">
          <NotificationDropdown onClose={() => setOpen(false)} />
        </div>
      )}
    </div>
  );
}
