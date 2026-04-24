import React from "react";
import { cn } from "@/lib/utils";

export interface HoursBarProps {
  used: number;
  total: number;
}

function colorClass(pct: number): string {
  if (pct >= 100) return "bg-k-volt";
  if (pct >= 66) return "bg-[#8AE800]";
  if (pct >= 33) return "bg-[#FFC107]";
  return "bg-k-border";
}

export function HoursBar({ used, total }: HoursBarProps) {
  const remaining = Math.max(total - used, 0);
  const pct = total > 0 ? (remaining / total) * 100 : 0;
  const width = Math.min(pct, 100);

  return (
    <div className="flex items-center gap-2">
      <div
        data-testid="hours-bar-track"
        className="w-20 h-1 bg-k-line rounded-full overflow-hidden"
      >
        <div
          data-testid="hours-bar-fill"
          className={cn("h-full", colorClass(pct))}
          style={{ width: `${width}%` }}
        />
      </div>
      <span className="font-[var(--font-mono)] text-[11px] text-k-subtle">
        {used}/{total}h
      </span>
    </div>
  );
}
