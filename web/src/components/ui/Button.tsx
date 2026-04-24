import React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cn } from "@/lib/utils";

export type ButtonVariant = "primary" | "volt" | "outline" | "ghost" | "danger";
export type ButtonSize = "sm" | "md" | "icon";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant: ButtonVariant;
  size?: ButtonSize;
  asChild?: boolean;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "bg-k-dark text-white hover:bg-[#1A1A1A]",
  volt:    "bg-k-volt text-k-dark hover:bg-[#B8EE3A]",
  outline: "bg-transparent text-k-dark border border-k-border hover:bg-k-surface",
  ghost:   "bg-k-bg text-k-subtle hover:bg-k-border",
  danger:  "bg-k-danger-bg text-k-danger-text border border-k-danger-text/30",
};

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm:   "text-xs px-3 py-1.5 rounded-k-sm",
  md:   "text-sm font-semibold px-4 py-2 rounded-k-sm",
  icon: "h-8 w-8 p-0 rounded-k-sm inline-flex items-center justify-center",
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant, size = "md", asChild = false, className, children, ...rest }, ref) => {
    const classes = cn(
      "inline-flex items-center justify-center transition duration-k disabled:opacity-40 disabled:cursor-not-allowed",
      SIZE_CLASSES[size],
      VARIANT_CLASSES[variant],
      className,
    );

    if (asChild) {
      return (
        <Slot className={classes} {...rest}>
          {children}
        </Slot>
      );
    }

    return (
      <button ref={ref} type="button" className={classes} {...rest}>
        {children}
      </button>
    );
  },
);

Button.displayName = "Button";
