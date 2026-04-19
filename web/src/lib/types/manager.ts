export interface ManagerSummary {
  id: string;
  tenantId: string;
  tenantName: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  identityDocumentType: string;
  identityNumber: string;
  phoneNumber: string | null;
  status: string;
  createdAt: string;
}

export interface ManagerListResponse {
  content: ManagerSummary[];
  totalPages: number;
  totalElements: number;
}

export interface CreateManagerRequest {
  tenantId: string;
  email: string;
  password?: string; // optional — backend auto-generates when omitted or empty
  identityDocumentType: string;
  identityNumber: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}
