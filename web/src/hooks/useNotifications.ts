"use client";

import { useCallback, useEffect, useRef, useState } from "react";

export interface Notification {
  id: string;
  type: string;
  title: string;
  body: string;
  metadata: Record<string, string>;
  readAt: string | null;
  read: boolean;
  createdAt: string;
}

interface NotificationsPage {
  items: Notification[];
  total: number;
  page: number;
  size: number;
}

interface UseNotificationsResult {
  notifications: Notification[];
  totalPages: number;
  isLoading: boolean;
  error: string | null;
  refresh: () => void;
}

export function useNotifications(
  page: number,
  unreadOnly: boolean
): UseNotificationsResult {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const refresh = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError(null);

    const params = new URLSearchParams({
      page: String(page),
      size: "20",
      unread: String(unreadOnly),
    });

    fetch(`/api/me/notifications?${params.toString()}`, {
      credentials: "include",
    })
      .then((res) => {
        if (!res.ok) throw new Error(`Failed to load notifications: ${res.status}`);
        return res.json() as Promise<NotificationsPage>;
      })
      .then((data) => {
        if (cancelled) return;
        setNotifications(
          (data.items ?? []).map((n) => ({ ...n, read: n.readAt != null }))
        );
        setTotalPages(Math.ceil(data.total / data.size) || 0);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Failed to load notifications.");
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [page, unreadOnly, tick]);

  return { notifications, totalPages, isLoading, error, refresh };
}

interface UseUnreadCountResult {
  count: number;
  refreshCount: () => void;
}

const POLL_INTERVAL_MS = 30_000;

export function useUnreadCount(): UseUnreadCountResult {
  const [count, setCount] = useState(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchCount = useCallback(() => {
    if (typeof document !== "undefined" && document.hidden) return;

    fetch(`/api/me/notifications/unread-count`, {
      credentials: "include",
    })
      .then((res) => {
        if (!res.ok) return;
        return res.json() as Promise<{ count: number }>;
      })
      .then((data) => {
        if (data !== undefined) setCount(data.count);
      })
      .catch(() => {
        // swallow — polling errors are non-critical
      });
  }, []);

  useEffect(() => {
    fetchCount();

    intervalRef.current = setInterval(fetchCount, POLL_INTERVAL_MS);

    const onVisibility = () => {
      if (!document.hidden) {
        fetchCount();
      }
    };
    document.addEventListener("visibilitychange", onVisibility);

    return () => {
      if (intervalRef.current !== null) clearInterval(intervalRef.current);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [fetchCount]);

  return { count, refreshCount: fetchCount };
}

interface UseMarkNotificationReadResult {
  markRead: (id: string) => Promise<void>;
}

export function useMarkNotificationRead(): UseMarkNotificationReadResult {
  const markRead = useCallback(async (id: string): Promise<void> => {
    const res = await fetch(`/api/me/notifications/${id}/read`, {
      method: "PATCH",
      credentials: "include",
    });
    if (!res.ok) throw new Error(`Failed: ${res.status}`);
  }, []);

  return { markRead };
}

interface UseMarkAllNotificationsReadResult {
  markAllRead: () => Promise<void>;
}

export function useMarkAllNotificationsRead(): UseMarkAllNotificationsReadResult {
  const markAllRead = useCallback(async (): Promise<void> => {
    const res = await fetch(`/api/me/notifications/mark-all-read`, {
      method: "POST",
      credentials: "include",
    });
    if (!res.ok) throw new Error(`Failed: ${res.status}`);
  }, []);

  return { markAllRead };
}
