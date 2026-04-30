export type ProgramStatus = "ACTIVE" | "INACTIVE";
export type ProgramModality = "HOURS_BASED" | "CLASSES_PER_WEEK" | "UNLIMITED";

export interface ProgramPlanSummary {
  id: string;
  programId: string;
  name: string;
  modality: ProgramModality;
  cost: number;
  hours: number | null;
  managerId: string;
  status: ProgramStatus;
}

export interface ProgramSummary {
  id: string;
  name: string;
  status: ProgramStatus;
  createdAt: string;
}

export interface ProgramDetail extends ProgramSummary {
  tenantId: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface ProgramListResponse {
  content: ProgramSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateProgramRequest {
  name: string;
}

export interface UpdateProgramRequest {
  name: string;
}
