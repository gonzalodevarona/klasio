import React from "react";
import { cn } from "@/lib/utils";

export interface SelectProps
  extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, hint, className, children, id, ...rest }, ref) => {
    const generatedId = React.useId();
    const selectId = id ?? generatedId;

    return (
      <div>
        {label && (
          <label
            htmlFor={selectId}
            className="text-xs font-medium text-k-subtle mb-1 font-[var(--font-mono)] uppercase tracking-wide block"
          >
            {label}
          </label>
        )}
        <select
          ref={ref}
          id={selectId}
          className={cn(
            "bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm font-[var(--font-main)] w-full focus:border-k-volt focus:outline-none",
            error && "border-k-danger-text",
            className
          )}
          {...rest}
        >
          {children}
        </select>
        {error ? (
          <p className="text-xs text-k-danger-text mt-1">{error}</p>
        ) : hint ? (
          <p className="text-xs text-k-muted mt-1">{hint}</p>
        ) : null}
      </div>
    );
  }
);

Select.displayName = "Select";
