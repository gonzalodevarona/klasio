"use client";
import { createContext, useContext, useCallback, useEffect, useRef, useState, ReactNode } from "react";

interface NotificationCountContextValue {
  count: number;
  hasCancellation: boolean;
  refreshCount: () => void;
}

const NotificationCountContext = createContext<NotificationCountContextValue>({
  count: 0,
  hasCancellation: false,
  refreshCount: () => {},
});

const POLL_INTERVAL_MS = 30_000;

export function NotificationCountProvider({ children }: { children: ReactNode }) {
  const [count, setCount] = useState(0);
  const [hasCancellation, setHasCancellation] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchCount = useCallback(async () => {
    if (document.hidden) return;
    try {
      const res = await fetch("/api/me/notifications/unread-count", { credentials: "include" });
      if (res.ok) {
        const data = await res.json() as { count: number; hasCancellation: boolean };
        setCount(data.count);
        setHasCancellation(data.hasCancellation ?? false);
      }
    } catch {
      // network errors are silent — count stays at last known value
    }
  }, []);

  useEffect(() => {
    fetchCount();
    intervalRef.current = setInterval(fetchCount, POLL_INTERVAL_MS);
    const onVisibility = () => { if (!document.hidden) fetchCount(); };
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [fetchCount]);

  return (
    <NotificationCountContext.Provider value={{ count, hasCancellation, refreshCount: fetchCount }}>
      {children}
    </NotificationCountContext.Provider>
  );
}

export function useNotificationCount() {
  return useContext(NotificationCountContext);
}
