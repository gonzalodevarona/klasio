import React from "react";
import { Button } from "./Button";

export interface PaginationProps {
  page: number;
  totalPages: number;
  total: number;
  onPrev: () => void;
  onNext: () => void;
  labelPrev: string;
  labelNext: string;
  labelFormat: (page: number, totalPages: number, total: number) => string;
}

export function Pagination({
  page,
  totalPages,
  total,
  onPrev,
  onNext,
  labelPrev,
  labelNext,
  labelFormat,
}: PaginationProps) {
  return (
    <div className="flex justify-between items-center pt-4">
      <span className="font-[var(--font-mono)] text-xs text-k-muted">
        {labelFormat(page, totalPages, total)}
      </span>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={onPrev} disabled={page === 0}>
          {labelPrev}
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={onNext}
          disabled={page >= totalPages - 1}
        >
          {labelNext}
        </Button>
      </div>
    </div>
  );
}
