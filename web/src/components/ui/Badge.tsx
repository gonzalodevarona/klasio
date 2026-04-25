import React from "react";
import { cn } from "@/lib/utils";

export type BadgeVariant =
  | "active" | "expiring" | "inactive"
  | "pending" | "approved" | "rejected"
  | "beginner" | "intermediate" | "advanced"
  | "info";

export interface BadgeProps {
  variant: BadgeVariant;
  label: string;
  small?: boolean;
  className?: string;
  icon?: React.ReactNode;
  title?: string;
}

const VARIANT_CLASSES: Record<BadgeVariant, string> = {
  active:       "bg-k-volt text-k-volt-text",
  expiring:     "bg-k-warn-bg text-k-warn-text",
  inactive:     "bg-k-bg text-k-subtle border border-k-border",
  pending:      "bg-k-warn-bg text-k-warn-text",
  approved:     "bg-k-volt text-k-volt-text",
  rejected:     "bg-k-danger-bg text-k-danger-text",
  beginner:     "bg-k-info-bg text-k-info-text",
  intermediate: "bg-k-warn-bg text-k-warn-text",
  advanced:     "bg-k-volt text-k-volt-text",
  info:         "bg-k-info-bg text-k-info-text",
};

export function Badge({ variant, label, small, className, icon, title }: BadgeProps) {
  return (
    <span
      title={title}
      className={cn(
        "rounded-full font-semibold inline-flex items-center gap-1",
        small ? "text-[10px] px-2 py-px" : "text-[11px] px-2.5 py-0.5",
        VARIANT_CLASSES[variant],
        className,
      )}
    >
      {label}
      {icon}
    </span>
  );
}
