import React from "react";
import { Card } from "./Card";
import { cn } from "@/lib/utils";

export interface StatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  subColor?: string;
  dark?: boolean;
}

export function StatCard({ label, value, sub, subColor, dark = false }: StatCardProps) {
  const labelClass = "text-k-muted";
  const valueClass = dark ? "text-k-volt" : "text-k-dark";
  const defaultSubClass = dark ? "text-k-volt" : "text-k-muted";

  return (
    <Card padding="md" dark={dark}>
      <div className="flex flex-col gap-1.5">
        <span
          className={cn(
            "font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em]",
            labelClass,
          )}
        >
          {label}
        </span>
        <span
          className={cn(
            "text-[40px] font-extrabold tracking-[-0.03em] leading-none",
            valueClass,
          )}
        >
          {value}
        </span>
        {sub && (
          <span className={cn("text-xs font-medium", subColor ?? defaultSubClass)}>
            {sub}
          </span>
        )}
      </div>
    </Card>
  );
}
