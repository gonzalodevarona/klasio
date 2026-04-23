"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from "@/hooks/useNotifications";
import { useNotificationCount } from "@/context/NotificationCountContext";
import NotificationItem from "./NotificationItem";

export default function NotificationList() {
  const t = useTranslations("notifications");
  const tPag = useTranslations("pagination");
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);

  const { notifications, totalPages, isLoading, error, refresh } =
    useNotifications(page, unreadOnly);
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

  function handleTabChange(nextUnreadOnly: boolean) {
    setUnreadOnly(nextUnreadOnly);
    setPage(0);
  }

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        {/* Tab toggle */}
        <div className="flex gap-1 bg-gray-100 rounded-md p-1">
          <button
            type="button"
            onClick={() => handleTabChange(false)}
            className={[
              "px-3 py-1 text-sm rounded transition-colors",
              !unreadOnly
                ? "bg-white font-medium text-gray-900 shadow-sm"
                : "text-gray-500 hover:text-gray-700",
            ].join(" ")}
          >
            {t("tabAll")}
          </button>
          <button
            type="button"
            onClick={() => handleTabChange(true)}
            className={[
              "px-3 py-1 text-sm rounded transition-colors",
              unreadOnly
                ? "bg-white font-medium text-gray-900 shadow-sm"
                : "text-gray-500 hover:text-gray-700",
            ].join(" ")}
          >
            {t("tabUnreadOnly")}
          </button>
        </div>

        <button
          type="button"
          onClick={handleMarkAll}
          className="text-sm text-indigo-600 hover:text-indigo-800 transition-colors"
        >
          {t("markAllAsRead")}
        </button>
      </div>

      {/* Content */}
      {isLoading && (
        <p className="text-sm text-gray-400 py-6 text-center">Loading…</p>
      )}

      {error && (
        <p className="text-sm text-red-500 py-6 text-center">{error}</p>
      )}

      {!isLoading && !error && notifications.length === 0 && (
        <p className="text-sm text-gray-400 py-6 text-center">
          {t("listEmpty")}
        </p>
      )}

      {!isLoading && !error && notifications.length > 0 && (
        <div className="rounded-lg border border-gray-200 divide-y divide-gray-100 overflow-hidden">
          {notifications.map((n) => (
            <NotificationItem key={n.id} notification={n} onRead={handleRead} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-500">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1 rounded border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors"
          >
            {tPag("previous")}
          </button>
          <span>
            {tPag("summary", { current: page + 1, total: totalPages, count: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 rounded border border-gray-200 disabled:opacity-40 hover:bg-gray-50 transition-colors"
          >
            {tPag("next")}
          </button>
        </div>
      )}
    </div>
  );
}
