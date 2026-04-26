"use client";

import { AlertTriangle, Bell } from "lucide-react";

interface NotificationTypeIconProps {
  type: string;
}

export default function NotificationTypeIcon({ type }: NotificationTypeIconProps) {
  if (type === "CLASS_SESSION_ALERTED") {
    return <AlertTriangle className="w-5 h-5 text-k-warn-text" />;
  }
  if (type === "CLASS_SESSION_CANCELLED") {
    return <Bell className="w-5 h-5 text-k-danger-text" />;
  }
  return <Bell className="w-5 h-5 text-k-muted" />;
}
