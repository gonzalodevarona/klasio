export type ProfessorStatus = "INVITED" | "ACTIVE" | "DEACTIVATED";

export interface ProfessorSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  status: ProfessorStatus;
  createdAt: string;
}

export interface ProfessorDetail extends ProfessorSummary {
  tenantId: string;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface ProfessorListResponse {
  content: ProfessorSummary[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateProfessorRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
}

export interface UpdateProfessorRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
}
