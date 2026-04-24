"use client";

import React, { useEffect } from "react";
import { Button } from "./Button";
import { cn } from "@/lib/utils";

export type ModalSize = "sm" | "md" | "lg" | "xl";

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  size?: ModalSize;
  className?: string;
  children: React.ReactNode;
}

const SIZE_CLASSES: Record<ModalSize, string> = {
  sm: "max-w-[400px]",
  md: "max-w-[480px]",
  lg: "max-w-[600px]",
  xl: "max-w-[800px]",
};

export function Modal({
  open,
  onClose,
  title,
  size = "md",
  className,
  children,
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      data-testid="modal-overlay"
      className="fixed inset-0 z-50 bg-k-dark/55 backdrop-blur-sm flex items-center justify-center"
      onClick={onClose}
    >
      <div
        data-testid="modal-panel"
        onClick={(e) => e.stopPropagation()}
        className={cn(
          "bg-k-surface rounded-k-xl shadow-k-modal max-h-[85vh] overflow-y-auto w-full",
          SIZE_CLASSES[size],
          className,
        )}
      >
        <div className="flex justify-between items-center px-7 py-6 border-b border-k-border">
          <h2 className="text-base font-bold">{title}</h2>
          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            data-testid="modal-close"
            aria-label="Close"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </Button>
        </div>
        <div className="px-7 py-6">{children}</div>
      </div>
    </div>
  );
}
