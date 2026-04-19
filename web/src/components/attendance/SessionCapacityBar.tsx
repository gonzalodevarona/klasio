"use client";

interface SessionCapacityBarProps {
  current: number;
  max: number;
  className?: string;
}

export default function SessionCapacityBar({ current, max, className }: SessionCapacityBarProps) {
  const pct = max > 0 ? (current / max) * 100 : 0;

  const barColor =
    pct >= 80 ? "bg-red-500" : pct >= 50 ? "bg-yellow-500" : "bg-green-500";

  const textColor =
    pct >= 80 ? "text-red-600" : pct >= 50 ? "text-yellow-600" : "text-green-600";

  return (
    <div className={`flex flex-col gap-1 min-w-[100px] ${className ?? ""}`}>
      <div className={`text-xs font-medium ${textColor}`}>
        {current}/{max} registered
      </div>
      <div className="h-1.5 w-full rounded-full bg-gray-200 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${barColor}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
