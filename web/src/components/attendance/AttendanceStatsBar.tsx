"use client";

import type { AttendanceStats } from "@/lib/types/attendance";

interface Props {
  stats: AttendanceStats | null;
  loading: boolean;
}

function SkeletonCard() {
  return (
    <div className="rounded-k-lg border-[1.5px] border-k-border bg-k-surface p-4 animate-pulse">
      <div className="h-[10px] w-16 bg-k-bg rounded mb-3" />
      <div className="h-[32px] w-14 bg-k-bg rounded" />
    </div>
  );
}

export default function AttendanceStatsBar({ stats, loading }: Props) {
  if (loading) {
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        {[0, 1, 2, 3].map((i) => <SkeletonCard key={i} />)}
      </div>
    );
  }

  if (!stats) return null;

  const totalCancelled = stats.cancelledByStudent + stats.cancelledBySystem;

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
      {/* Attended + rate */}
      <div className="rounded-k-lg border-[1.5px] border-k-border bg-k-surface p-4">
        <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
          Attended
        </p>
        <div className="flex items-baseline gap-2">
          <span className="text-[32px] font-extrabold text-k-dark leading-none">
            {stats.attended}
          </span>
          <span className="px-2 py-1 rounded-full text-xs font-semibold bg-k-volt/20 text-k-volt">
            {stats.attendanceRatePercent}%
          </span>
        </div>
      </div>

      {/* Cancelled */}
      <div className="rounded-k-lg border-[1.5px] border-k-border bg-k-surface p-4">
        <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
          Cancelled
        </p>
        <span className="text-[32px] font-extrabold text-k-dark leading-none">
          {totalCancelled}
        </span>
      </div>

      {/* Absent */}
      <div className="rounded-k-lg border-[1.5px] border-k-border bg-k-surface p-4">
        <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
          Absent
        </p>
        <span className="text-[32px] font-extrabold text-k-dark leading-none">
          {stats.absent}
        </span>
      </div>

      {/* Hours consumed */}
      <div className="rounded-k-lg border-[1.5px] border-k-border bg-k-surface p-4">
        <p className="font-[var(--font-mono)] text-[10px] uppercase tracking-[0.1em] text-k-muted mb-2">
          Hours consumed
        </p>
        <span className="text-[32px] font-extrabold text-k-dark leading-none">
          {stats.totalHoursConsumed}h
        </span>
      </div>
    </div>
  );
}
