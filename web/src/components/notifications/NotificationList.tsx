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

  const { notifications, totalPages, isLoading, error, refresh, markReadOptimistic } =
    useNotifications(page, unreadOnly);
  const { markRead } = useMarkNotificationRead();
  const { markAllRead } = useMarkAllNotificationsRead();
  const { refreshCount } = useNotificationCount();

  async function handleRead(id: string) {
    markReadOptimistic(id);
    refreshCount();
    await markRead(id);
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
        <div className="flex gap-1 bg-k-bg rounded-k-sm p-1">
          <button
            type="button"
            onClick={() => handleTabChange(false)}
            className={[
              "px-3 py-1 text-sm rounded-[6px] transition-colors",
              !unreadOnly
                ? "bg-k-surface font-semibold text-k-dark shadow-k-card"
                : "text-k-muted hover:text-k-dark",
            ].join(" ")}
          >
            {t("tabAll")}
          </button>
          <button
            type="button"
            onClick={() => handleTabChange(true)}
            className={[
              "px-3 py-1 text-sm rounded-[6px] transition-colors",
              unreadOnly
                ? "bg-k-surface font-semibold text-k-dark shadow-k-card"
                : "text-k-muted hover:text-k-dark",
            ].join(" ")}
          >
            {t("tabUnreadOnly")}
          </button>
        </div>

        <button
          type="button"
          onClick={handleMarkAll}
          className="text-k-muted hover:text-k-dark text-sm font-medium transition-colors"
        >
          {t("markAllAsRead")}
        </button>
      </div>

      {/* Content */}
      {isLoading && (
        <p className="text-sm text-k-muted py-6 text-center">Loading…</p>
      )}

      {error && (
        <p className="text-sm text-k-danger-text py-6 text-center">{error}</p>
      )}

      {!isLoading && !error && notifications.length === 0 && (
        <p className="text-sm text-k-muted py-6 text-center">
          {t("listEmpty")}
        </p>
      )}

      {!isLoading && !error && notifications.length > 0 && (
        <div className="rounded-k-md border border-k-border divide-y divide-k-line overflow-hidden">
          {notifications.map((n) => (
            <NotificationItem key={n.id} notification={n} onRead={handleRead} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-k-muted">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1 rounded border border-k-border disabled:opacity-40 hover:bg-k-bg transition-colors"
          >
            {tPag("previous")}
          </button>
          <span className="font-[var(--font-mono)] text-xs">
            {tPag("summary", { current: page + 1, total: totalPages, count: totalPages })}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1 rounded border border-k-border disabled:opacity-40 hover:bg-k-bg transition-colors"
          >
            {tPag("next")}
          </button>
        </div>
      )}
    </div>
  );
}
