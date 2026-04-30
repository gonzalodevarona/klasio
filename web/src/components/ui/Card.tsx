import React from "react";
import { cn } from "@/lib/utils";

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  padding?: "sm" | "md" | "lg";
  dark?: boolean;
  children: React.ReactNode;
}

const PADDING_CLASSES = {
  sm: "p-4",
  md: "p-6",
  lg: "p-8",
} as const;

export function Card({
  padding = "md",
  dark = false,
  className,
  children,
  ...rest
}: CardProps) {
  return (
    <div
      className={cn(
        "rounded-k-lg",
        dark ? "bg-k-dark" : "bg-k-surface border-[1.5px] border-k-border",
        PADDING_CLASSES[padding],
        className
      )}
      {...rest}
    >
      {children}
    </div>
  );
}
