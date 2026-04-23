"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from "@/hooks/useNotifications";
import { useNotificationCount } from "@/context/NotificationCountContext";
import NotificationItem from "./NotificationItem";

interface NotificationDropdownProps {
  onClose: () => void;
}

export default function NotificationDropdown({ onClose }: NotificationDropdownProps) {
  const t = useTranslations("notifications");
  const { notifications, isLoading, refresh } = useNotifications(0, false);
  const { markRead } = useMarkNotificationRead();
  const { markAllRead } = useMarkAllNotificationsRead();
  const { refreshCount } = useNotificationCount();

  async function handleRead(id: string) {
    await markRead(id);
    refresh();
    refreshCount();
  }

  async function handleMarkAll() {
    await markAllRead();
    refresh();
    refreshCount();
  }

  const recent = notifications.slice(0, 5);

  return (
    <div className="bg-white rounded-lg shadow-lg w-80 max-h-96 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 shrink-0">
        <span className="text-sm font-semibold text-gray-800">{t("dropdownTitle")}</span>
        <button
          type="button"
          onClick={handleMarkAll}
          className="text-xs text-indigo-600 hover:text-indigo-800 transition-colors"
        >
          {t("markAllRead")}
        </button>
      </div>

      {/* List */}
      <div className="overflow-y-auto flex-1 divide-y divide-gray-100">
        {isLoading && (
          <p className="text-xs text-gray-400 px-4 py-3">{t("dropdownLoading")}</p>
        )}
        {!isLoading && recent.length === 0 && (
          <p className="text-xs text-gray-400 px-4 py-3">{t("dropdownEmpty")}</p>
        )}
        {!isLoading &&
          recent.map((n) => (
            <NotificationItem
              key={n.id}
              notification={n}
              onRead={(id) => {
                handleRead(id);
                onClose();
              }}
            />
          ))}
      </div>

      {/* Footer */}
      <div className="px-4 py-2 border-t border-gray-100 shrink-0">
        <Link
          href="/notifications"
          onClick={onClose}
          className="text-xs text-indigo-600 hover:text-indigo-800 transition-colors"
        >
          {t("viewAll")}
        </Link>
      </div>
    </div>
  );
}
