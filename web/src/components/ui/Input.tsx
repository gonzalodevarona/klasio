import React from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, leftIcon, className, id, ...rest }, ref) => {
    const generatedId = React.useId();
    const inputId = id ?? generatedId;

    return (
      <div>
        {label && (
          <label
            htmlFor={inputId}
            className="text-xs font-medium text-k-subtle mb-1 font-[var(--font-mono)] uppercase tracking-wide block"
          >
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none flex items-center">
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              "bg-k-surface border border-k-border rounded-k-sm px-3 py-2 text-sm font-[var(--font-main)] w-full focus:border-k-volt focus:outline-none",
              error && "border-k-danger-text",
              leftIcon && "pl-9",
              className
            )}
            {...rest}
          />
        </div>
        {error ? (
          <p className="text-xs text-k-danger-text mt-1">{error}</p>
        ) : hint ? (
          <p className="text-xs text-k-muted mt-1">{hint}</p>
        ) : null}
      </div>
    );
  }
);

Input.displayName = "Input";
