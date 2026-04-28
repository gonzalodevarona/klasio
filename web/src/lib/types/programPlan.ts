export type ProgramModality = "HOURS_BASED" | "CLASSES_PER_WEEK" | "UNLIMITED";

export type PlanStatus = "ACTIVE" | "INACTIVE";

export interface ScheduleEntry {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
}

export interface ProgramPlanSummary {
  id: string;
  name: string;
  modality: ProgramModality;
  cost: number;
  hours: number | null;
  managerId: string;
  managerName?: string;
  status: PlanStatus;
  programId?: string;
  programName?: string;
}

export interface ProgramPlanDetail extends ProgramPlanSummary {
  programId: string;
  tenantId: string;
  scheduleEntries: ScheduleEntry[];
  createdAt: string;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface CreateProgramPlanRequest {
  name: string;
  modality: string;
  cost: number;
  hours?: number | null;
  managerId: string;
  scheduleEntries?: ScheduleEntry[];
}

export interface UpdateProgramPlanRequest {
  name: string;
  cost: number;
  hours?: number | null;
  managerId: string;
  scheduleEntries?: ScheduleEntry[];
}
