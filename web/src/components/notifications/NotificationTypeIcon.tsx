"use client";

import { AlertTriangle, Bell } from "lucide-react";

interface NotificationTypeIconProps {
  type: string;
}

export default function NotificationTypeIcon({ type }: NotificationTypeIconProps) {
  if (type === "SESSION_ALERTED") {
    return <AlertTriangle className="w-5 h-5 text-amber-500" />;
  }
  if (type === "SESSION_CANCELLED") {
    return <Bell className="w-5 h-5 text-red-500" />;
  }
  return <Bell className="w-5 h-5 text-gray-400" />;
}
