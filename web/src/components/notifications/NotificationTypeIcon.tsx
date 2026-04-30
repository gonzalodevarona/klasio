"use client";

interface NotificationTypeIconProps {
  type: string;
}

function getEmoji(type: string): string {
  if (type === "CLASS_SESSION_ALERTED") return "⚠️";
  if (type === "CLASS_SESSION_CANCELLED") return "🚫";
  if (type === "CLASS_LEVEL_CHANGED") return "↕️";
  return "🔔";
}

export default function NotificationTypeIcon({ type }: NotificationTypeIconProps) {
  return (
    <span role="img" aria-label={type} className="text-base leading-none select-none">
      {getEmoji(type)}
    </span>
  );
}
