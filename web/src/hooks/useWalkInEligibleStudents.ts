"use client";
import { useEffect, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export type EligibleStudent = {
  studentId: string;
  fullName: string;
  idDocument: string;
  enrollmentId: string;
  membershipId: string;
  availableHours: number;
  level: string;
};

export function useWalkInEligibleStudents(
  classId: string,
  sessionDate: string,
  startTime: string,
  level: string | null
) {
  const [students, setStudents] = useState<EligibleStudent[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let aborted = false;
    const params = new URLSearchParams({ startTime });
    if (level) params.set("level", level);
    const url = `${API_BASE}/classes/${classId}/sessions/${sessionDate}/walk-in/eligible-students?${params.toString()}`;
    setIsLoading(true);
    setError(null);
    fetch(url, { credentials: "include" })
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((data: EligibleStudent[]) => { if (!aborted) setStudents(data); })
      .catch((e) => { if (!aborted) setError(e as Error); })
      .finally(() => { if (!aborted) setIsLoading(false); });
    return () => { aborted = true; };
  }, [classId, sessionDate, startTime, level]);

  return { students, isLoading, error };
}
