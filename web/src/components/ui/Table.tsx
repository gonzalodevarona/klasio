import React from "react";
import { cn } from "@/lib/utils";

export interface TableProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

export function Table({ className, children, ...rest }: TableProps) {
  return (
    <div
      className={cn("overflow-x-auto rounded-k-md border border-k-border w-full", className)}
      {...rest}
    >
      <table className="w-full">{children}</table>
    </div>
  );
}

export interface TheadProps extends React.HTMLAttributes<HTMLTableSectionElement> {
  children: React.ReactNode;
}

export function Thead({ className, children, ...rest }: TheadProps) {
  return (
    <thead className={cn("bg-k-bg border-b border-k-border", className)} {...rest}>
      {children}
    </thead>
  );
}

export interface ThProps extends React.ThHTMLAttributes<HTMLTableCellElement> {
  right?: boolean;
  children: React.ReactNode;
}

export function Th({ right = false, className, children, ...rest }: ThProps) {
  return (
    <th
      className={cn(
        "font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted px-4 py-2.5",
        right && "text-right",
        className,
      )}
      {...rest}
    >
      {children}
    </th>
  );
}

export interface TrProps extends React.HTMLAttributes<HTMLTableRowElement> {
  active?: boolean;
  children: React.ReactNode;
}

export function Tr({ onClick, active = false, className, children, ...rest }: TrProps) {
  return (
    <tr
      onClick={onClick}
      className={cn(
        "border-b border-k-line",
        onClick && "hover:bg-k-surface cursor-pointer",
        active && "bg-[#F9FFEA]",
        className,
      )}
      {...rest}
    >
      {children}
    </tr>
  );
}

export interface TdProps extends React.TdHTMLAttributes<HTMLTableCellElement> {
  mono?: boolean;
  muted?: boolean;
  bold?: boolean;
  right?: boolean;
  children: React.ReactNode;
}

export function Td({
  mono = false,
  muted = false,
  bold = false,
  right = false,
  className,
  children,
  ...rest
}: TdProps) {
  return (
    <td
      className={cn(
        "px-4 py-3 text-sm whitespace-nowrap",
        mono && "font-[var(--font-mono)]",
        muted && "text-k-muted",
        bold && "font-semibold",
        right && "text-right",
        className,
      )}
      {...rest}
    >
      {children}
    </td>
  );
}
