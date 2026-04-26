"use client";

import { useRouter } from "next/navigation";
import type { Notification } from "@/hooks/useNotifications";
import NotificationTypeIcon from "./NotificationTypeIcon";

function formatRelativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

interface NotificationItemProps {
  notification: Notification;
  onRead: (id: string) => void;
}

export default function NotificationItem({
  notification,
  onRead,
}: NotificationItemProps) {
  const router = useRouter();

  function handleClick() {
    onRead(notification.id);

    const isSessionType =
      notification.type === "CLASS_SESSION_ALERTED" ||
      notification.type === "CLASS_SESSION_CANCELLED";
    const classId = notification.metadata?.classId;

    if (isSessionType && classId) {
      router.push(`/classes/${classId}`);
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className={[
        "relative w-full text-left flex items-start gap-3 px-4 py-3 transition-colors",
        "hover:bg-k-bg focus:outline-none focus-visible:ring-2 focus-visible:ring-k-volt",
        notification.read ? "bg-k-surface" : "bg-[#F9FFEA]",
      ].join(" ")}
    >
      {!notification.read && (
        <span className="absolute left-0 top-0 bottom-0 w-[3px] bg-k-volt rounded-r-full" />
      )}
      <div className="mt-0.5 shrink-0 w-8 h-8 rounded-[8px] bg-k-bg flex items-center justify-center">
        <NotificationTypeIcon type={notification.type} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-k-dark truncate">
          {notification.title}
        </p>
        <p className="text-xs text-k-muted mt-0.5 line-clamp-2">
          {notification.body}
        </p>
        <p className="text-[#9A9A98] font-[var(--font-mono)] text-[10px] mt-1">
          {formatRelativeTime(notification.createdAt)}
        </p>
      </div>
    </button>
  );
}
