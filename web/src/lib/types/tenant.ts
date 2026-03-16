export type TenantStatus = "ACTIVE" | "INACTIVE";

export interface TenantSummary {
  id: string;
  slug: string;
  name: string;
  sportDiscipline: string;
  status: TenantStatus;
  createdAt: string;
}

export interface TenantDetail extends TenantSummary {
  logoUrl: string | null;
  contactEmail: string;
  contactPhone: string | null;
  contactAddress: string | null;
  createdBy: string;
  deactivatedAt: string | null;
  deactivatedBy: string | null;
}

export interface TenantListResponse {
  content: TenantSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTenantRequest {
  name: string;
  sportDiscipline: string;
  slug?: string;
  contactEmail: string;
  contactPhone?: string;
  contactAddress?: string;
  logo?: File;
}
