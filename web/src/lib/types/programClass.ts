export type ClassLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type ClassType = "RECURRING" | "ONE_TIME";
export type ClassStatus = "ACTIVE" | "INACTIVE";

export interface ClassScheduleEntry {
  dayOfWeek?: string;
  specificDate?: string;
  startTime: string;
  endTime: string;
}

export interface ProgramClassSummary {
  id: string;
  programId: string;
  programName?: string;
  name: string;
  level: ClassLevel;
  type: ClassType;
  professorId?: string;
  professorName?: string;
  maxStudents: number;
  status: ClassStatus;
  createdAt: string;
}

export interface ProgramClassDetail extends ProgramClassSummary {
  tenantId: string;
  professorName?: string;
  scheduleEntries: ClassScheduleEntry[];
  createdBy: string;
  updatedAt?: string;
  updatedBy?: string;
}

export interface ProgramClassListResponse {
  content: ProgramClassSummary[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface CreateClassRequest {
  name: string;
  level: ClassLevel;
  type: ClassType;
  scheduleEntries: ClassScheduleEntry[];
  professorId: string;
  maxStudents: number;
}

export interface UpdateClassRequest {
  name: string;
  level: ClassLevel;
  scheduleEntries: ClassScheduleEntry[];
  maxStudents: number;
}

export interface AssignProfessorRequest {
  professorId: string;
}
