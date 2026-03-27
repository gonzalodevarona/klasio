export type Level = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export interface EnrollmentSummary {
  id: string;
  studentId: string;
  studentName: string;
  programId: string;
  programName: string;
  level: Level;
  enrollmentDate: string;
  status: string;
}

export interface EnrollmentDetail extends EnrollmentSummary {
  tenantId: string;
  createdAt: string;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface CreateEnrollmentRequest {
  studentId: string;
  level: string;
}

export interface EnrollmentListResponse {
  content: EnrollmentSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LevelHistoryListResponse {
  content: LevelHistoryEntry[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type LevelHistoryAction = "ENROLLED" | "PROMOTED" | "UNENROLLED";

export interface LevelHistoryEntry {
  id: string;
  previousLevel: string | null;
  newLevel: string | null;
  action: LevelHistoryAction;
  changedBy: string;
  changedByRole: string;
  changedAt: string;
  justification: string | null;
}
