"use client";

import { useLevelHistory } from "@/hooks/useStudentEnrollments";
import LevelBadge from "./LevelBadge";
import { Level } from "@/lib/types/enrollment";

interface LevelHistoryListProps {
  enrollmentId: string;
}

export default function LevelHistoryList({ enrollmentId }: LevelHistoryListProps) {
  const { history, loading, error } = useLevelHistory(enrollmentId);

  function formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  if (loading) {
    return (
      <div className="py-3 text-sm text-gray-500">Loading level history...</div>
    );
  }

  if (error) {
    return (
      <div className="py-3 text-sm text-red-600">{error}</div>
    );
  }

  if (history.length === 0) {
    return (
      <div className="py-3 text-sm text-gray-500">No level history available.</div>
    );
  }

  return (
    <div className="space-y-3">
      <h4 className="text-sm font-medium text-gray-700">Level History</h4>
      <ul className="space-y-2">
        {history.map((entry) => (
          <li
            key={entry.id}
            className="flex items-start gap-3 rounded-md bg-gray-50 px-3 py-2 text-sm"
          >
            <div className="flex-1">
              {entry.action === "UNENROLLED" ? (
                <div className="flex items-center gap-2">
                  <LevelBadge level={entry.previousLevel as Level} />
                  <span className="text-gray-400">&rarr;</span>
                  <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                    Unenrolled
                  </span>
                </div>
              ) : entry.previousLevel === null ? (
                <div className="flex items-center gap-2">
                  <span className="text-gray-600">Initial assignment:</span>
                  <LevelBadge level={entry.newLevel as Level} />
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <LevelBadge level={entry.previousLevel as Level} />
                  <span className="text-gray-400">&rarr;</span>
                  <LevelBadge level={entry.newLevel as Level} />
                </div>
              )}
              {entry.justification && (
                <p className="mt-1 text-xs text-gray-500">
                  Reason: {entry.justification}
                </p>
              )}
            </div>
            <div className="text-right text-xs text-gray-500 whitespace-nowrap">
              <div>{formatDate(entry.changedAt)}</div>
              <div className="text-gray-400">{entry.changedByRole}</div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
