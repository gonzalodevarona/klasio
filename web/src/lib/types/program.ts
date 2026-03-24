export type ProgramStatus = "ACTIVE" | "INACTIVE";

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
