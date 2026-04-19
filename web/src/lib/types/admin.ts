export interface AdminSummary {
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

export interface AdminListResponse {
  content: AdminSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateAdminRequest {
  tenantId: string;
  email: string;
  password: string;
  identityDocumentType: string;
  identityNumber: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
}
